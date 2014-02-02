rem Stops the dev environment
ECHO off

echo "Stopping tomcat"
taskkill /F /T /FI  "WINDOWTITLE eq Tomcat"
taskkill /F /T /FI  "WINDOWTITLE eq Administrator: Tomcat"

echo "Stopping mysql" 
call  %ELYXOR_DEV_HOME%\windev\3rdParty\mysql\scripts\mysql-shutdown-win.bat

taskkill /F /T /FI  "WINDOWTITLE eq Administrator: xeros mysql*"
taskkill /F /T /FI  "WINDOWTITLE eq xeros mysql*"
