echo off

call %ELYXOR_DEV_HOME%\windev\ant_env.bat

goto SKIP_KILLING_ENV

call stopSystem.bat

:SKIP_KILLING_ENV

set WAR_NAME="elyxor-starterwar-0.0.0.1.war"

echo "Delete old builds folder"

del ..\builds\%WAR_NAME%

echo "Remove old deployment"

del %CATALINA_HOME%\webapps\%WAR_NAME%

echo "clean commons"
pushd ..\elyxor-commons
call ant clean
popd

echo "Build SimpleServlet"
pushd ..\SimpleServlet
call ant clean
call ant
popd

echo "Deploy builds"

copy ..\builds\%WAR_NAME% %CATALINA_HOME%\webapps

rem call startSystem.bat