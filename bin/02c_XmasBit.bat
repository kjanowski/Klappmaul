@echo off
SETLOCAL EnableDelayedExpansion

cd ..

::-------------------------------------------------------------------
:: prepare the classpath
::-------------------------------------------------------------------
SET CLASSP=.;dist/KlappmaulEngine.jar;dist/lib/*.jar;

::-------------------------------------------------------------------
:: run the Klappmaul engine
::-------------------------------------------------------------------
echo.
echo.-------------------------------------------------
echo.Starting The Klappmaul Engine
echo.-------------------------------------------------
@echo on

SET CONFIG=res\config\appConfig_XmasBit.properties

::-------------------------------------------------------------------
:: use this if you want to use your default Java version:
::-------------------------------------------------------------------
::java -cp %CLASSP% de.kmj.robots.RobotEngineRemoteApplication %CONFIG%

::-------------------------------------------------------------------
:: use this if you want to enforce a particular Java version:
::-------------------------------------------------------------------
set JAVA_HOME=D:\Entwicklung\Java\jdk1.8.0_192
"%JAVA_HOME%\bin\java" -cp %CLASSP% de.kmj.robots.RobotEngineRemoteApplication %CONFIG%

ENDLOCAL

::-------------------------------------------------------------------
:: uncomment this for debugging purposes
::-------------------------------------------------------------------
::PAUSE

