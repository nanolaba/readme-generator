@echo off
set JAVA_HOME=C:\Program Files\Java\jdk-19.0.1
set PATH=%JAVA_HOME%\bin;%PATH%
@java -Dfile.encoding=UTF8 -Xms128m -Xmx1024m -cp "%~dp0\*" com.nanolaba.nrg.NRG %*