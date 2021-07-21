@echo off
SETLOCAL EnableDelayedExpansion

cd ..
echo.

::-------------------------------------------------------------------
:: run the control application
::-------------------------------------------------------------------
echo.
echo.-------------------------------------------------
echo.Starting The Control Application
echo.-------------------------------------------------
@echo on

SET JAVA_HOME=C:\Program Files\Java\jdk1.8.0_192
"%JAVA_HOME%\bin\java" -cp ".;lib/RobotEngine.jar" de.kmj.robots.controlApp.DefaultControlApplication res/ControlApp.config

ENDLOCAL

:: uncomment for debugging purposes
PAUSE

