@echo off
REM ==========================================
REM 前端构建脚本 (Windows)
REM ==========================================
REM 用途：在 Windows 上构建前端生产版本
REM 使用：双击运行或在命令行执行 build.bat
REM ==========================================

echo ================================================
echo    Sheep Music - 前端构建脚本 (Windows)
echo ================================================
echo.

REM 检查 Node.js
where node >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Node.js 未安装，请先安装 Node.js
    pause
    exit /b 1
)

echo [INFO] Node.js 版本:
node --version
echo [INFO] npm 版本:
npm --version
echo.

REM 检查 package.json
if not exist "package.json" (
    echo [ERROR] package.json 不存在，请在前端目录运行此脚本
    pause
    exit /b 1
)

REM 清理旧的构建文件
if exist "dist" (
    echo [INFO] 清理旧的 dist 目录...
    rmdir /s /q dist
)

REM 安装依赖
echo [INFO] 安装依赖...
call npm install --legacy-peer-deps
if %ERRORLEVEL% neq 0 (
    echo [ERROR] 依赖安装失败
    pause
    exit /b 1
)

REM 构建生产版本
echo [INFO] 构建生产版本...
call npm run build
if %ERRORLEVEL% neq 0 (
    echo [ERROR] 构建失败
    pause
    exit /b 1
)

REM 检查构建结果
if exist "dist" (
    echo [INFO] 构建成功！
    echo.
    echo [INFO] dist 目录内容:
    dir dist
    echo.
    echo [INFO] 下一步:
    echo 1. 检查 dist 目录内容是否正确
    echo 2. 提交到 Git: git add dist/ ^&^& git commit -m "Build frontend"
    echo 3. 推送到远程: git push
    echo 4. 在服务器上拉取并部署
) else (
    echo [ERROR] 构建失败，dist 目录未生成
    pause
    exit /b 1
)

echo ================================================
pause


