package config

import (
	"os"
	"path/filepath"
	"testing"
)

func TestLoadResolvesRelativeDataPathsToAbsolute(t *testing.T) {
	tmp := t.TempDir()
	t.Chdir(tmp)

	configPath := filepath.Join(tmp, "config.yaml")
	body := `server:
  port: 5701
  mode: test
database:
  path: data/test.db
jwt:
  secret: unit-test-secret
data:
  dir: ./data
  scripts_dir: data/scripts
  log_dir: ./data/logs
cors:
  origins: []
`
	if err := os.WriteFile(configPath, []byte(body), 0o644); err != nil {
		t.Fatalf("write config: %v", err)
	}

	t.Cleanup(func() { C = nil })

	cfg, err := Load(configPath)
	if err != nil {
		t.Fatalf("load config: %v", err)
	}

	if !filepath.IsAbs(cfg.Data.Dir) {
		t.Fatalf("expected Data.Dir to be absolute, got %q", cfg.Data.Dir)
	}
	if !filepath.IsAbs(cfg.Data.ScriptsDir) {
		t.Fatalf("expected Data.ScriptsDir to be absolute, got %q", cfg.Data.ScriptsDir)
	}
	if !filepath.IsAbs(cfg.Data.LogDir) {
		t.Fatalf("expected Data.LogDir to be absolute, got %q", cfg.Data.LogDir)
	}

	expectedScripts := filepath.Join(tmp, "data", "scripts")
	if cfg.Data.ScriptsDir != expectedScripts {
		t.Fatalf("expected ScriptsDir=%q, got %q", expectedScripts, cfg.Data.ScriptsDir)
	}
}

func TestResolveAbsoluteDataPathLeavesAbsoluteUntouched(t *testing.T) {
	tmp := t.TempDir()
	resolved := resolveAbsoluteDataPath(tmp)
	if resolved != filepath.Clean(tmp) {
		t.Fatalf("expected absolute path to stay as %q, got %q", filepath.Clean(tmp), resolved)
	}
}
