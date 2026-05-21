package service

import (
	"bufio"
	"context"
	"fmt"
	"os"
	"os/exec"
	"path/filepath"
	"regexp"
	"strings"
	"time"

	"daidai-panel/config"
	"daidai-panel/database"
	"daidai-panel/model"
	"daidai-panel/pkg/cron"

	"gorm.io/gorm"
)

type PullCallback func(line string)

func PullSubscription(sub *model.Subscription) (string, error) {
	return PullSubscriptionWithCallback(sub, nil)
}

func PullSubscriptionWithCallback(sub *model.Subscription, onOutput PullCallback) (string, error) {
	return PullSubscriptionWithContext(context.Background(), sub, onOutput)
}

func PullSubscriptionWithContext(ctx context.Context, sub *model.Subscription, onOutput PullCallback) (string, error) {
	if ctx == nil {
		ctx = context.Background()
	}
	startTime := time.Now()

	var sshKeyPath string
	if sub.SSHKeyID != nil {
		var sshKey model.SSHKey
		if err := database.DB.First(&sshKey, *sub.SSHKeyID).Error; err == nil {
			tmpFile, err := writeTempSSHKey(sshKey.PrivateKey)
			if err != nil {
				return "", fmt.Errorf("写入 SSH 密钥失败: %w", err)
			}
			defer os.Remove(tmpFile)
			sshKeyPath = tmpFile
		}
	}
	authCfg, err := buildGitAuthConfig(os.Environ(), sub.URL, sub, sshKeyPath)
	if err != nil {
		return "", err
	}
	defer authCfg.CleanupFunc()

	var fullLog strings.Builder
	emit := func(line string) {
		fullLog.WriteString(line)
		fullLog.WriteString("\n")
		if onOutput != nil {
			onOutput(line)
		}
	}

	emit(fmt.Sprintf("[开始拉取] %s (%s)", sub.Name, sub.Type))

	var output string
	var pullErr error

	switch sub.Type {
	case model.SubTypeSingleFile:
		output, pullErr = pullSingleFileWithCallback(ctx, sub, sshKeyPath, emit)
	default:
		output, pullErr = pullGitRepoWithCallback(ctx, sub, authCfg, emit)
	}

	if pullErr == nil && ctx.Err() != nil {
		pullErr = fmt.Errorf("拉取已停止")
	}
	if pullErr == nil {
		pullErr = runSubscriptionHookIfConfigured(sub, emit)
	}
	if pullErr == nil && ctx.Err() != nil {
		pullErr = fmt.Errorf("拉取已停止")
	}
	if pullErr == nil {
		syncSubscriptionTasks(sub, emit)
	}

	duration := time.Since(startTime).Seconds()

	status := 0
	if pullErr != nil {
		status = 1
		emit(fmt.Sprintf("[错误] %s", pullErr.Error()))
	}

	emit(fmt.Sprintf("[完成] 耗时 %.2f 秒, 状态: %s", duration, map[int]string{0: "成功", 1: "失败"}[status]))

	subLog := model.SubLog{
		SubscriptionID: sub.ID,
		Status:         status,
		Content:        fullLog.String(),
		Duration:       duration,
	}
	database.DB.Create(&subLog)

	now := time.Now()
	database.DB.Model(sub).Updates(map[string]interface{}{
		"last_pull_at": &now,
		"status":       status,
	})

	return output, pullErr
}

func runCmdWithCallback(ctx context.Context, cmd *exec.Cmd, emit PullCallback) (string, error) {
	pipe, err := cmd.StdoutPipe()
	if err != nil {
		return "", err
	}
	cmd.Stderr = cmd.Stdout

	if err := cmd.Start(); err != nil {
		return "", err
	}

	var buf strings.Builder
	scanner := bufio.NewScanner(pipe)
	scanner.Buffer(make([]byte, 64*1024), 256*1024)
	for scanner.Scan() {
		line := scanner.Text()
		buf.WriteString(line)
		buf.WriteString("\n")
		emit(line)
	}
	if scanErr := scanner.Err(); scanErr != nil {
		if ctx != nil && ctx.Err() != nil {
			return buf.String(), fmt.Errorf("拉取已停止")
		}
		return buf.String(), scanErr
	}

	err = cmd.Wait()
	if ctx != nil && ctx.Err() != nil {
		return buf.String(), fmt.Errorf("拉取已停止")
	}
	return buf.String(), err
}

func gitHasWorkingTreeChanges(ctx context.Context, repoDir string, env []string) (bool, error) {
	cmd := exec.CommandContext(ctx, "git", "status", "--porcelain", "--untracked-files=all")
	cmd.Dir = repoDir
	cmd.Env = env

	output, err := cmd.Output()
	if err != nil {
		if ctx != nil && ctx.Err() != nil {
			return false, fmt.Errorf("拉取已停止")
		}
		return false, err
	}

	return strings.TrimSpace(string(output)) != "", nil
}

func pullGitRepoWithCallback(ctx context.Context, sub *model.Subscription, authCfg gitAuthConfig, emit PullCallback) (string, error) {
	saveDir := sub.SaveDir
	if saveDir == "" {
		saveDir = sub.Alias
		if saveDir == "" {
			parts := strings.Split(sub.URL, "/")
			saveDir = strings.TrimSuffix(parts[len(parts)-1], ".git")
		}
	}

	destDir := filepath.Join(config.C.Data.ScriptsDir, saveDir)
	if absDestDir, err := filepath.Abs(destDir); err == nil {
		destDir = absDestDir
	}
	env := authCfg.Env

	if IsGitRepo(destDir) {
		var fullOutput strings.Builder
		branchLabel := "默认分支"
		if strings.TrimSpace(sub.Branch) != "" {
			branchLabel = strings.TrimSpace(sub.Branch)
		}

		emit(fmt.Sprintf("[检测到已有仓库] %s 已存在 Git 仓库，接下来会同步远端并覆盖更新本地文件", saveDir))
		emit(fmt.Sprintf("[同步远端地址] 正在校正订阅地址 -> %s", authCfg.DisplayURL))
		output, err := syncGitRemoteWithCallback(ctx, destDir, authCfg.RemoteURL, env, emit)
		fullOutput.WriteString(output)
		if err != nil {
			return fullOutput.String(), err
		}

		fetchArgs := []string{"fetch", "--depth", "1", "--prune", "origin"}
		if strings.TrimSpace(sub.Branch) != "" {
			fetchArgs = append(fetchArgs, strings.TrimSpace(sub.Branch))
		}
		emit(fmt.Sprintf("[拉取远端更新] 正在获取分支 %s 的最新提交", branchLabel))
		cmd := exec.CommandContext(ctx, "git", fetchArgs...)
		cmd.Dir = destDir
		cmd.Env = env
		output, err = runCmdWithCallback(ctx, cmd, emit)
		fullOutput.WriteString(output)
		if err != nil {
			return fullOutput.String(), err
		}

		if err := applySparseCheckout(ctx, destDir, sub.SubPath, env, emit); err != nil {
			return fullOutput.String(), err
		}

		forceOverwrite := sub.ForceOverwrite == nil || *sub.ForceOverwrite
		if forceOverwrite {
			emit("[覆盖更新本地文件] 正在用远端最新提交覆盖当前订阅目录中的仓库内容")
			cmd = exec.CommandContext(ctx, "git", "reset", "--hard", "FETCH_HEAD")
			cmd.Dir = destDir
			cmd.Env = env
			output, err = runCmdWithCallback(ctx, cmd, emit)
			fullOutput.WriteString(output)
			if err != nil {
				return fullOutput.String(), err
			}

			emit("[清理旧文件] 正在删除远端仓库已移除、但本地仍残留的文件和目录")
			cmd = exec.CommandContext(ctx, "git", "clean", "-fd")
			cmd.Dir = destDir
			cmd.Env = env
			output, err = runCmdWithCallback(ctx, cmd, emit)
			fullOutput.WriteString(output)
		} else {
			emit("[保留本地修改] 正在合并远端更新（保留本地修改的文件）")
			hasStash, err := gitHasWorkingTreeChanges(ctx, destDir, env)
			if err != nil {
				return fullOutput.String(), err
			}
			if hasStash {
				cmd = exec.CommandContext(ctx, "git", "stash", "push", "--include-untracked", "-m", "daidai-panel-subscription-update")
				cmd.Dir = destDir
				cmd.Env = env
				output, err = runCmdWithCallback(ctx, cmd, emit)
				fullOutput.WriteString(output)
				if err != nil {
					return fullOutput.String(), err
				}
			} else {
				emit("[保留本地修改] 未检测到本地改动，跳过暂存恢复")
			}

			cmd = exec.CommandContext(ctx, "git", "reset", "--hard", "FETCH_HEAD")
			cmd.Dir = destDir
			cmd.Env = env
			output, err = runCmdWithCallback(ctx, cmd, emit)
			fullOutput.WriteString(output)
			if err != nil {
				return fullOutput.String(), err
			}

			if hasStash {
				emit("[恢复本地修改] 正在恢复之前暂存的本地修改")
				cmd = exec.CommandContext(ctx, "git", "stash", "pop")
				cmd.Dir = destDir
				cmd.Env = env
				output, err = runCmdWithCallback(ctx, cmd, emit)
				fullOutput.WriteString(output)
				if err != nil {
					emit("[提示] 本地修改与远端更新存在冲突，请手动处理")
				}
			}
		}
		return fullOutput.String(), err
	}

	if destInfo, err := os.Stat(destDir); err == nil {
		if !destInfo.IsDir() {
			return "", fmt.Errorf("保存目录已被文件占用: %s", saveDir)
		}

		entries, readErr := os.ReadDir(destDir)
		if readErr != nil {
			return "", fmt.Errorf("读取保存目录失败: %w", readErr)
		}
		if len(entries) > 0 {
			var fullOutput strings.Builder
			branchLabel := "默认分支"
			if strings.TrimSpace(sub.Branch) != "" {
				branchLabel = strings.TrimSpace(sub.Branch)
			}

			emit(fmt.Sprintf("[检测到已存在脚本目录] %s 当前不是 Git 仓库，接下来会原地初始化仓库并覆盖本地文件", saveDir))
			emit("[git init] 正在初始化本地仓库")
			cmd := exec.CommandContext(ctx, "git", "init")
			cmd.Dir = destDir
			cmd.Env = env
			output, err := runCmdWithCallback(ctx, cmd, emit)
			fullOutput.WriteString(output)
			if err != nil {
				return fullOutput.String(), err
			}

			emit(fmt.Sprintf("[同步远端地址] 正在校正订阅地址 -> %s", authCfg.DisplayURL))
			output, err = syncGitRemoteWithCallback(ctx, destDir, authCfg.RemoteURL, env, emit)
			fullOutput.WriteString(output)
			if err != nil {
				return fullOutput.String(), err
			}

			fetchArgs := []string{"fetch", "--depth", "1", "--prune", "origin"}
			if strings.TrimSpace(sub.Branch) != "" {
				fetchArgs = append(fetchArgs, strings.TrimSpace(sub.Branch))
			}
			emit(fmt.Sprintf("[拉取远端更新] 正在获取分支 %s 的最新提交", branchLabel))
			cmd = exec.CommandContext(ctx, "git", fetchArgs...)
			cmd.Dir = destDir
			cmd.Env = env
			output, err = runCmdWithCallback(ctx, cmd, emit)
			if err != nil {
				fullOutput.WriteString(output)
				return fullOutput.String(), err
			}
			fullOutput.WriteString(output)
			if ctx.Err() != nil {
				return fullOutput.String(), fmt.Errorf("拉取已停止")
			}

			if err := applySparseCheckout(ctx, destDir, sub.SubPath, env, emit); err != nil {
				return fullOutput.String(), err
			}

			forceOverwrite := sub.ForceOverwrite == nil || *sub.ForceOverwrite
			emit("[覆盖更新本地文件] 正在用远端最新提交覆盖当前脚本目录内容")
			cmd = exec.CommandContext(ctx, "git", "reset", "--hard", "FETCH_HEAD")
			cmd.Dir = destDir
			cmd.Env = env
			output, err = runCmdWithCallback(ctx, cmd, emit)
			fullOutput.WriteString(output)
			if err != nil {
				return fullOutput.String(), err
			}

			if forceOverwrite {
				emit("[清理旧文件] 正在删除远端仓库已移除、但本地仍残留的文件和目录")
				cmd = exec.CommandContext(ctx, "git", "clean", "-fd")
				cmd.Dir = destDir
				cmd.Env = env
				output, err = runCmdWithCallback(ctx, cmd, emit)
				fullOutput.WriteString(output)
				if err != nil {
					return fullOutput.String(), err
				}
			} else {
				emit("[保留本地文件] 跳过清理本地多余文件")
			}
			return fullOutput.String(), nil
		}
	}

	emit(fmt.Sprintf("[git clone] %s -> %s", authCfg.DisplayURL, saveDir))
	os.MkdirAll(destDir, 0755)
	args := []string{"clone", "--depth", "1"}
	if sub.Branch != "" {
		args = append(args, "-b", sub.Branch)
	}
	args = append(args, authCfg.RemoteURL, destDir)
	cmd := exec.CommandContext(ctx, "git", args...)
	cmd.Dir = config.C.Data.ScriptsDir
	cmd.Env = env
	output, err := runCmdWithCallback(ctx, cmd, emit)
	if err != nil {
		return output, err
	}
	if spErr := applySparseCheckout(ctx, destDir, sub.SubPath, env, emit); spErr != nil {
		return output, spErr
	}
	return output, nil
}

func applySparseCheckout(ctx context.Context, repoDir string, subPath string, env []string, emit PullCallback) error {
	subPath = strings.TrimSpace(subPath)
	if subPath == "" {
		return nil
	}

	var paths []string
	for _, p := range strings.Split(subPath, ",") {
		p = strings.TrimSpace(p)
		if p != "" {
			paths = append(paths, p)
		}
	}
	if len(paths) == 0 {
		return nil
	}

	emit(fmt.Sprintf("[sparse-checkout] 设置子目录过滤: %s", strings.Join(paths, ", ")))

	cmd := exec.CommandContext(ctx, "git", "sparse-checkout", "init", "--cone")
	cmd.Dir = repoDir
	cmd.Env = env
	if _, err := runCmdWithCallback(ctx, cmd, emit); err != nil {
		return fmt.Errorf("sparse-checkout init 失败: %w", err)
	}

	args := append([]string{"sparse-checkout", "set"}, paths...)
	cmd = exec.CommandContext(ctx, "git", args...)
	cmd.Dir = repoDir
	cmd.Env = env
	if _, err := runCmdWithCallback(ctx, cmd, emit); err != nil {
		return fmt.Errorf("sparse-checkout set 失败: %w", err)
	}

	return nil
}

func pullSingleFileWithCallback(ctx context.Context, sub *model.Subscription, _ string, emit PullCallback) (string, error) {
	saveDir := sub.SaveDir
	if saveDir == "" {
		saveDir = "downloads"
	}

	parts := strings.Split(sub.URL, "/")
	filename := parts[len(parts)-1]
	if sub.Alias != "" {
		filename = sub.Alias
	}

	destPath := filepath.Join(config.C.Data.ScriptsDir, saveDir, filename)
	emit(fmt.Sprintf("[下载] %s -> %s/%s", sub.URL, saveDir, filename))
	output, err := DownloadFileWithContext(ctx, sub.URL, destPath)
	if output != "" {
		emit(output)
	}
	return output, err
}

func syncGitRemoteWithCallback(ctx context.Context, repoDir, remoteURL string, env []string, emit PullCallback) (string, error) {
	cmd := exec.CommandContext(ctx, "git", "remote")
	cmd.Dir = repoDir
	cmd.Env = env

	remoteOutput, err := cmd.Output()
	if err != nil {
		return "", err
	}

	args := []string{"remote", "add", "origin", remoteURL}
	for _, name := range strings.Fields(string(remoteOutput)) {
		if name == "origin" {
			args = []string{"remote", "set-url", "origin", remoteURL}
			break
		}
	}

	cmd = exec.CommandContext(ctx, "git", args...)
	cmd.Dir = repoDir
	cmd.Env = env
	return runCmdWithCallback(ctx, cmd, emit)
}

func writeTempSSHKey(privateKey string) (string, error) {
	tmpFile, err := os.CreateTemp("", "ssh_key_*")
	if err != nil {
		return "", err
	}
	defer tmpFile.Close()

	if _, err := tmpFile.WriteString(privateKey); err != nil {
		os.Remove(tmpFile.Name())
		return "", err
	}

	os.Chmod(tmpFile.Name(), 0600)
	return tmpFile.Name(), nil
}

var (
	cronCommentRe          = regexp.MustCompile(`(?im)^\s*#?\s*cron\s*[:：]\s*(.+)$`)
	subscriptionTaskNameRe = regexp.MustCompile(`new\s+Env\s*\(\s*['"` + "`" + `]([^'"` + "`" + `]+)['"` + "`" + `]\s*\)`)
	// 青龙风格 `cron "EXPR" filename, tag:xxx` 单行声明，常见于 JS 顶部注释。
	// 例如：cron "6 6 6 6 *" jd_CheckCK.js, tag:京东CK检测by-ccwav
	cronDirectiveLineRe = regexp.MustCompile(`(?i)\bcron\s+["']([^"'\n\r]+)["']\s+([^\s,;]+)`)
)

type subscriptionTaskSyncOptions struct {
	autoAdd     bool
	autoDelete  bool
	defaultCron string
	allowedExts map[string]bool
}

type subscriptionTaskCandidate struct {
	Name           string
	Command        string
	CronExpression string
}

func subscriptionTaskLabel(subID uint) string {
	return fmt.Sprintf("subscription:%d", subID)
}

func hasLabel(labels []string, target string) bool {
	for _, item := range labels {
		if item == target {
			return true
		}
	}
	return false
}

func withLabel(labels []string, target string) []string {
	if hasLabel(labels, target) {
		return labels
	}
	return append(labels, target)
}

func subscriptionSaveDir(sub *model.Subscription) string {
	saveDir := sub.SaveDir
	if saveDir == "" {
		saveDir = sub.Alias
		if saveDir == "" {
			parts := strings.Split(sub.URL, "/")
			saveDir = strings.TrimSuffix(parts[len(parts)-1], ".git")
		}
	}
	return saveDir
}

func matchesSubscriptionFilters(sub *model.Subscription, filename string) bool {
	if sub.Whitelist != "" {
		matched := false
		for _, pattern := range strings.Split(sub.Whitelist, ",") {
			pattern = strings.TrimSpace(pattern)
			if pattern != "" && strings.Contains(filename, pattern) {
				matched = true
				break
			}
		}
		if !matched {
			return false
		}
	}
	if sub.Blacklist != "" {
		for _, pattern := range strings.Split(sub.Blacklist, ",") {
			pattern = strings.TrimSpace(pattern)
			if pattern != "" && strings.Contains(filename, pattern) {
				return false
			}
		}
	}
	return true
}

func syncSubscriptionTasks(sub *model.Subscription, emit PullCallback) {
	options := getSubscriptionTaskSyncOptions(sub)
	if !options.autoAdd && !options.autoDelete {
		return
	}

	candidates := collectSubscriptionTaskCandidates(sub, options)
	label := subscriptionTaskLabel(sub.ID)

	var managedTasks []model.Task
	queryTasksByLabel(label).Find(&managedTasks)
	managedByCommand := make(map[string]*model.Task, len(managedTasks))
	for i := range managedTasks {
		managedByCommand[strings.TrimSpace(managedTasks[i].Command)] = &managedTasks[i]
	}

	created := 0
	updated := 0
	deleted := 0
	adopted := 0

	if options.autoAdd {
		for command, candidate := range candidates {
			if existing, ok := managedByCommand[command]; ok {
				changes := map[string]interface{}{}
				if existing.Name != candidate.Name {
					changes["name"] = candidate.Name
					existing.Name = candidate.Name
				}
				if existing.CronExpression != candidate.CronExpression {
					changes["cron_expression"] = candidate.CronExpression
					existing.CronExpression = candidate.CronExpression
				}
				if len(changes) > 0 {
					database.DB.Model(existing).Updates(changes)
					GetSchedulerV2().UpdateJob(existing)
					updated++
					emit(fmt.Sprintf("[自动更新任务] %s (cron: %s)", candidate.Name, candidate.CronExpression))
				}
				continue
			}

			var existing model.Task
			if err := database.DB.Where("command = ?", command).First(&existing).Error; err == nil {
				labels := withLabel(existing.GetLabels(), label)
				existing.SetLabelsFromSlice(labels)
				database.DB.Model(&existing).Update("labels", existing.Labels)
				managedByCommand[command] = &existing
				adopted++
				emit(fmt.Sprintf("[关联已有任务] %s", existing.Name))
				continue
			}

			task := model.Task{
				Name:            candidate.Name,
				Command:         candidate.Command,
				CronExpression:  candidate.CronExpression,
				TaskType:        model.TaskTypeCron,
				Status:          model.TaskStatusEnabled,
				Timeout:         86400,
				NotifyOnFailure: true,
			}
			task.SetLabelsFromSlice([]string{label})
			if database.DB.Select("*").Create(&task).Error == nil {
				GetSchedulerV2().AddJob(&task)
				managedByCommand[command] = &task
				created++
				emit(fmt.Sprintf("[自动添加任务] %s (cron: %s)", candidate.Name, candidate.CronExpression))
			}
		}
	}

	if options.autoDelete {
		for _, task := range managedTasks {
			command := strings.TrimSpace(task.Command)
			if !strings.HasPrefix(command, "task ") {
				continue
			}
			if _, ok := candidates[command]; ok {
				continue
			}

			GetSchedulerV2().RemoveJob(task.ID)
			database.DB.Where("task_id = ?", task.ID).Delete(&model.TaskLog{})
			database.DB.Delete(&task)
			deleted++
			emit(fmt.Sprintf("[自动删除任务] %s", task.Name))
		}
	}

	if created > 0 {
		emit(fmt.Sprintf("[共自动添加 %d 个定时任务]", created))
	}
	if updated > 0 {
		emit(fmt.Sprintf("[共自动更新 %d 个定时任务]", updated))
	}
	if adopted > 0 {
		emit(fmt.Sprintf("[共关联 %d 个已有任务]", adopted))
	}
	if deleted > 0 {
		emit(fmt.Sprintf("[共自动删除 %d 个失效任务]", deleted))
	}
}

func getSubscriptionTaskSyncOptions(sub *model.Subscription) subscriptionTaskSyncOptions {
	defaultCron := strings.TrimSpace(model.GetRegisteredConfig("default_cron_rule"))
	if defaultCron != "" && !cron.Parse(defaultCron).Valid {
		defaultCron = ""
	}

	return subscriptionTaskSyncOptions{
		autoAdd:     sub.AutoAddTask || isConfigEnabled("auto_add_cron", true),
		autoDelete:  sub.AutoDelTask || isConfigEnabled("auto_del_cron", true),
		defaultCron: defaultCron,
		allowedExts: getSubscriptionAllowedExtensions(model.GetRegisteredConfig("repo_file_extensions")),
	}
}

func isConfigEnabled(key string, defaultValue bool) bool {
	if _, exists := model.GetSystemConfigDefinition(key); exists {
		return model.GetRegisteredConfigBool(key)
	}
	return model.GetConfigBool(key, defaultValue)
}

func getSubscriptionAllowedExtensions(raw string) map[string]bool {
	exts := make(map[string]bool)
	for _, token := range strings.FieldsFunc(raw, func(r rune) bool {
		return r == ',' || r == ';' || r == '\n' || r == '\r' || r == '\t' || r == ' '
	}) {
		token = strings.TrimSpace(strings.ToLower(token))
		token = strings.TrimPrefix(token, "*")
		if token == "" {
			continue
		}
		if !strings.HasPrefix(token, ".") {
			token = "." + token
		}
		exts[token] = true
	}
	if len(exts) > 0 {
		return exts
	}

	return map[string]bool{
		".js": true,
		".mjs": true,
		".ts": true,
		".py": true,
		".sh": true,
	}
}

func shouldManageSubscriptionFile(sub *model.Subscription, filename string, allowedExts map[string]bool) bool {
	ext := strings.ToLower(filepath.Ext(filename))
	if !allowedExts[ext] {
		return false
	}
	return matchesSubscriptionFilters(sub, filename)
}

func collectSubscriptionTaskCandidates(sub *model.Subscription, options subscriptionTaskSyncOptions) map[string]subscriptionTaskCandidate {
	candidates := make(map[string]subscriptionTaskCandidate)
	saveDir := subscriptionSaveDir(sub)
	scriptsDir := filepath.Join(config.C.Data.ScriptsDir, saveDir)

	if _, err := os.Stat(scriptsDir); err != nil {
		return candidates
	}

	filepath.Walk(scriptsDir, func(path string, info os.FileInfo, err error) error {
		if err != nil {
			return nil
		}
		if info.IsDir() {
			switch strings.ToLower(info.Name()) {
			case ".git", "node_modules", "__pycache__":
				return filepath.SkipDir
			}
			return nil
		}

		if !shouldManageSubscriptionFile(sub, info.Name(), options.allowedExts) {
			return nil
		}

		cronExpr := resolveCronForSubscriptionTask(path, options.defaultCron)
		if cronExpr == "" {
			return nil
		}

		relPath, err := filepath.Rel(config.C.Data.ScriptsDir, path)
		if err != nil {
			return nil
		}
		command := "task " + relPath
		taskName := resolveSubscriptionTaskName(path, strings.TrimSuffix(info.Name(), filepath.Ext(info.Name())))
		candidates[command] = subscriptionTaskCandidate{
			Name:           taskName,
			Command:        command,
			CronExpression: cronExpr,
		}
		return nil
	})

	return candidates
}

func queryTasksByLabel(label string) *gorm.DB {
	return database.DB.Where(
		"labels = ? OR labels LIKE ? OR labels LIKE ? OR labels LIKE ?",
		label,
		label+",%",
		"%,"+label,
		"%,"+label+",%",
	)
}

func resolveCronForSubscriptionTask(path string, defaultCron string) string {
	f, err := os.Open(path)
	if err != nil {
		return ""
	}
	defer f.Close()

	scanner := bufio.NewScanner(f)
	lineCount := 0
	scriptBase := strings.ToLower(filepath.Base(path))
	for scanner.Scan() {
		lineCount++
		if lineCount > 50 {
			break
		}
		line := scanner.Text()
		if expr := extractSubscriptionCronExpression(line, scriptBase); expr != "" {
			return expr
		}
	}
	return strings.TrimSpace(defaultCron)
}

func resolveSubscriptionTaskName(path, fallback string) string {
	fallback = strings.TrimSpace(fallback)

	f, err := os.Open(path)
	if err != nil {
		return fallback
	}
	defer f.Close()

	scanner := bufio.NewScanner(f)
	lineCount := 0
	for scanner.Scan() {
		lineCount++
		if lineCount > 120 {
			break
		}

		if matches := subscriptionTaskNameRe.FindStringSubmatch(scanner.Text()); len(matches) > 1 {
			name := strings.TrimSpace(matches[1])
			if name != "" {
				return name
			}
		}
	}

	return fallback
}

func extractSubscriptionCronExpression(line, scriptBase string) string {
	if matches := cronCommentRe.FindStringSubmatch(line); len(matches) > 1 {
		expr := strings.TrimSpace(matches[1])
		if cron.Parse(expr).Valid {
			return expr
		}
	}

	if matches := cronDirectiveLineRe.FindStringSubmatch(line); len(matches) > 2 && scriptBase != "" {
		expr := strings.TrimSpace(matches[1])
		fileToken := normalizeSubscriptionCronScriptToken(matches[2])
		if fileToken != "" &&
			strings.EqualFold(filepath.Base(fileToken), scriptBase) &&
			cron.Parse(expr).Valid {
			return expr
		}
	}

	return extractSubscriptionCronExpressionFromFilenameLine(line, scriptBase)
}

func extractSubscriptionCronExpressionFromFilenameLine(line, scriptBase string) string {
	trimmed := strings.TrimSpace(line)
	if trimmed == "" || scriptBase == "" {
		return ""
	}

	cleaned := strings.TrimSpace(strings.Trim(trimmed, `"'`))
	fields := strings.Fields(cleaned)
	if len(fields) < 6 {
		return ""
	}

	for _, cronFieldCount := range []int{6, 5} {
		if len(fields) <= cronFieldCount {
			continue
		}

		expr := strings.Join(fields[:cronFieldCount], " ")
		if !cron.Parse(expr).Valid {
			continue
		}

		fileToken := normalizeSubscriptionCronScriptToken(fields[cronFieldCount])
		if fileToken == "" {
			continue
		}

		if strings.EqualFold(filepath.Base(fileToken), scriptBase) {
			return expr
		}
	}

	return ""
}

func normalizeSubscriptionCronScriptToken(token string) string {
	token = strings.TrimSpace(token)
	token = strings.Trim(token, `"'`)
	token = strings.TrimRight(token, ",;:)")
	token = strings.TrimLeft(token, "(")
	return strings.TrimSpace(token)
}
