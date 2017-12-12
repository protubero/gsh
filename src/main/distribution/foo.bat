REM The only thing to do is to change the filename of this file and to set the APP_NAME below accordingly.
REM e.g. if your project is PRJX then rename the file ro prjx.bat and change
REM the line below to SET APP_NAME=prjx or SET APP_NAME="Project X"
REM The app name is displayed when entering terminal mode

@ECHO OFF
SET CURRENT_DIR=%~dp0
SET APP_NAME=foo
SET GSH_SCRIPTPATH=%CURRENT_DIR%..\src
rem echo %GSH_SCRIPTPATH%
SET GSH_CLASSPATH=%CURRENT_DIR%..\lib\*
rem echo %GSH_CLASSPATH%

IF NOT EXIST %CURRENT_DIR%..\gsh GOTO NOLOCALGSH
rem prefer local execution

%CURRENT_DIR%..\gsh\bin\gsh %*

goto :eof

:NOLOCALGSH
gsh %*

