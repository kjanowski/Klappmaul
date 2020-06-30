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


java -cp dist;dist/lib/* de.kmj.robots.controlApp.DefaultControlApplication res/ControlApp.config

ENDLOCAL

:: uncomment for debugging purposes
PAUSE

