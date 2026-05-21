package config

import (
	"crypto/rand"
	"encoding/hex"
	"os"
	"path/filepath"
	"strconv"
	"strings"
	"time"

	"gopkg.in/yaml.v3"
)

type Config struct {
	Server   ServerConfig   `yaml:"server"`
	Database DatabaseConfig `yaml:"database"`
	JWT      JWTConfig      `yaml:"jwt"`
	Data     DataConfig     `yaml:"data"`
	CORS     CORSConfig     `yaml:"cors"`
}

type ServerConfig struct {
	Port   int    `yaml:"port"`
	Mode   string `yaml:"mode"`
	WebDir string `yaml:"web_dir"`
}

type DatabaseConfig struct {
	Path string `yaml:"path"`
}

type JWTConfig struct {
	Secret             string        `yaml:"secret"`
	AccessTokenExpire  time.Duration `yaml:"access_token_expire"`
	RefreshTokenExpire time.Duration `yaml:"refresh_token_expire"`
}

type DataConfig struct {
	Dir        string `yaml:"dir"`
	ScriptsDir string `yaml:"scripts_dir"`
	LogDir     string `yaml:"log_dir"`
}

type CORSConfig struct {
	Origins []string `yaml:"origins"`
}

var C *Config

func Load(path string) (*Config, error) {
	data, err := os.ReadFile(path)
	if err != nil {
		return nil, err
	}

	cfg := &Config{}
	if err := yaml.Unmarshal(data, cfg); err != nil {
		return nil, err
	}

	if envPort := os.Getenv("SERVER_PORT"); envPort != "" {
		if p, err := strconv.Atoi(envPort); err == nil {
			cfg.Server.Port = p
		}
	}
	if envDBPath := os.Getenv("DB_PATH"); envDBPath != "" {
		cfg.Database.Path = envDBPath
	}
	if envWebDir := os.Getenv("WEB_DIR"); envWebDir != "" {
		cfg.Server.WebDir = envWebDir
	}

	if cfg.JWT.Secret == "" {
		cfg.JWT.Secret = loadOrGenerateSecret(cfg.Data.Dir)
	}

	if cfg.JWT.AccessTokenExpire == 0 {
		cfg.JWT.AccessTokenExpire = 480 * time.Hour
	}
	if cfg.JWT.RefreshTokenExpire == 0 {
		cfg.JWT.RefreshTokenExpire = 1440 * time.Hour
	}

	cfg.Data.Dir = resolveAbsoluteDataPath(cfg.Data.Dir)
	cfg.Data.ScriptsDir = resolveAbsoluteDataPath(cfg.Data.ScriptsDir)
	cfg.Data.LogDir = resolveAbsoluteDataPath(cfg.Data.LogDir)

	os.MkdirAll(cfg.Data.Dir, 0755)
	os.MkdirAll(cfg.Data.ScriptsDir, 0755)
	os.MkdirAll(cfg.Data.LogDir, 0755)

	C = cfg
	return cfg, nil
}

func resolveAbsoluteDataPath(raw string) string {
	trimmed := filepath.Clean(strings.TrimSpace(raw))
	if trimmed == "" || trimmed == "." {
		return trimmed
	}
	if filepath.IsAbs(trimmed) {
		return trimmed
	}
	abs, err := filepath.Abs(trimmed)
	if err != nil {
		return trimmed
	}
	return abs
}

func loadOrGenerateSecret(dataDir string) string {
	secretFile := filepath.Join(dataDir, ".jwt_secret")
	if data, err := os.ReadFile(secretFile); err == nil && len(data) > 0 {
		return string(data)
	}
	b := make([]byte, 32)
	rand.Read(b)
	secret := hex.EncodeToString(b)
	os.MkdirAll(dataDir, 0755)
	os.WriteFile(secretFile, []byte(secret), 0600)
	return secret
}
