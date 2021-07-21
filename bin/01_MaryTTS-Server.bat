@echo off

SET BINDIR=%~dp0
SET MARY_BASE="%BINDIR%\..\lib"
SET CLASSPATH=".;%MARY_BASE%\*"

::------------------------------------------------------------
:: use this to enforce a specific Java version
::------------------------------------------------------------
set JAVA_HOME=C:\Program Files\Java\jdk1.8.0_192
"%JAVA_HOME%\bin\java" -showversion -ea -Xms40m -Xmx1g -cp %CLASSPATH% "-Dmary.base=%MARY_BASE%" marytts.server.Mary

