@echo off
REM ============================================================
REM  一键重置数据库 + 运行接口冒烟测试
REM  用法: 双击本文件, 或在命令行执行 sql\reset-and-test.cmd
REM  前置: 后端已在 http://localhost:9090 运行; Node.js 可用
REM ============================================================
setlocal

set MYSQL="C:\Program Files\MySQL\MySQL Server 5.7\bin\mysql.exe"
REM 数据库密码从环境变量读取（与后端 application.yml 使用同一个 DB_PASSWORD）
REM 若未设置，请在命令行执行一次:  setx DB_PASSWORD "你的MySQL密码"
if "%DB_PASSWORD%"=="" (
  echo [错误] 未检测到环境变量 DB_PASSWORD。
  echo        请先执行:  setx DB_PASSWORD "你的MySQL密码"
  echo        然后重新打开一个命令行窗口再运行本脚本。
  pause
  exit /b 1
)
set DBPASS=%DB_PASSWORD%
set ROOT=%~dp0..
set TMPDIR=%TEMP%\mall_sql

echo [1/3] 生成种子数据...
node "%ROOT%\sql\generate-seed.js" || goto :error

echo [2/3] 重置数据库 shopping_mall...
if not exist "%TMPDIR%" mkdir "%TMPDIR%"
copy /Y "%ROOT%\sql\schema.sql" "%TMPDIR%\schema.sql" >nul
copy /Y "%ROOT%\sql\seed.sql" "%TMPDIR%\seed.sql" >nul
%MYSQL% -uroot --password=%DBPASS% --default-character-set=utf8mb4 -e "source %TMPDIR%/schema.sql" 2>nul || goto :error
%MYSQL% -uroot --password=%DBPASS% --default-character-set=utf8mb4 -e "source %TMPDIR%/seed.sql" 2>nul || goto :error

echo [3/3] 运行接口冒烟测试...
node "%ROOT%\sql\smoke-test.js"
if errorlevel 1 goto :failed

echo.
echo ============================================
echo   全部通过
echo ============================================
exit /b 0

:failed
echo.
echo ============================================
echo   存在失败用例, 见上方 [FAIL] 明细
echo ============================================
exit /b 1

:error
echo 执行失败, 请检查 MySQL 是否启动、密码是否正确
exit /b 1
