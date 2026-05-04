@echo off
chcp 65001 >nul
title 打包 Electron 桌面应用

echo ========================================
echo   打包任务管理系统为桌面应用
echo ========================================
echo.

echo [1/4] 打包 Vue 前端...
cd todolist
call npm run build
if %errorlevel% neq 0 (
    echo 前端打包失败
    pause
    exit /b
)
echo 前端打包完成
echo.

echo [2/4] 打包 Electron 应用...
call npm run electron:build
if %errorlevel% neq 0 (
    echo Electron 打包失败
    pause
    exit /b
)
echo Electron 打包完成
echo.

echo [3/4] 复制配置文件...
if exist ..\release\app.jar (
    echo app.jar 已存在
) else (
    echo 警告: 未找到 app.jar
)
echo.

echo [4/4] 打包完成！
echo.
echo 安装程序位置: todolist\dist_electron\TaskManager_Setup.exe
echo.

pause