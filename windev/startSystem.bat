rem Starts a complete development environment
ECHO off

pushd "%ELYXOR_DEV_HOME%\windev\3rdParty\mysql\scripts\"

echo "Starting mysql"
start /MIN "xeros mysql" mysql-start-win.bat

popd

pushd "%CATALINA_HOME%\bin\"

echo "Starting Elyxor Tomcat"
call startup.bat --StdOutput "%ELYXOR_DEV_HOME%\windev\temp\stdOut.log"

popd

