@echo off
chcp 65001 >nul
echo ========================================
echo 标签打印工具 - Windows 打包脚本
echo ========================================
echo.

REM 检查 Python 是否安装
python --version >nul 2>&1
if %errorlevel% neq 0 (
    echo 错误: 未找到 Python，请先安装 Python 3.8+
    echo 下载地址: https://www.python.org/downloads/
    pause
    exit /b 1
)

REM 检查 pip 是否可用
pip --version >nul 2>&1
if %errorlevel% neq 0 (
    echo 错误: 未找到 pip
    pause
    exit /b 1
)

echo [1/4] 安装依赖...
pip install pyinstaller -q
if %errorlevel% neq 0 (
    echo 错误: 安装 PyInstaller 失败
    pause
    exit /b 1
)

echo [2/4] 清理旧文件...
if exist build rmdir /s /q build
if exist dist rmdir /s /q dist
if exist __pycache__ rmdir /s /q __pycache__

echo [3/4] 打包应用...
pyinstaller --onefile --windowed --name "标签打印工具" --icon=NONE label_printer.py
if %errorlevel% neq 0 (
    echo 错误: 打包失败
    pause
    exit /b 1
)

echo [4/4] 完成!
echo.
echo ========================================
echo 打包成功！
echo 可执行文件位置: dist\标签打印工具.exe
echo ========================================
echo.

REM 打开输出目录
explorer dist

pause
