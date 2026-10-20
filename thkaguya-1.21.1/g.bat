@echo off
rem ---------------------------------------------------------------------------
rem Build entry for the 1.21.1 / NeoForge 21.1 port.
rem Usage: g.bat build | g.bat runClient | g.bat runServer | g.bat runData
rem
rem Why the proxy sits on the command line:
rem   %USERPROFILE%\.gradle\gradle.properties pins systemProp.http(s).proxyPort=10195,
rem   which is appended to the daemon JVM args and overrides anything set in this
rem   project's gradle.properties. Command-line -D wins over it. If your proxy port
rem   changes, edit PROXY below only.
rem
rem JAVA_HOME here is process-local (JDK 21); the system PATH / JAVA_HOME are left
rem alone, so the thkaguya-1.20.1 build chain (jdk-17 + gradle 8.1.1) is unaffected.
rem Keep this file ASCII-only: cmd reads .bat in the OEM codepage.
rem ---------------------------------------------------------------------------
setlocal
set "JAVA_HOME=C:\Java\jdk-21"
set "PROXY=-Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=16888 -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=16888"
call "C:\Gradle\gradle-8.14.3\bin\gradle.bat" -p "%~dp0." %PROXY% %*
endlocal
