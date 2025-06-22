@echo off
@REM set JAVA_HOME=C:\Program Files\Java\jdk-1.8
@REM set PATH=%JAVA_HOME%\bin;%PATH%
@java -Dfile.encoding=UTF8 -Xms128m -Xmx1024m -cp "%~dp0\*" com.nanolaba.nrg.NRG %*