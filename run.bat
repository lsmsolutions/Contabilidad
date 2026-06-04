@echo off
set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.10.7-hotspot"
set "PATH=%JAVA_HOME%\bin;%PATH%"
set "MAVEN_REPO=%CD%\.m2\repository"
".tools\apache-maven-3.9.9\bin\mvn.cmd" "-Dmaven.repo.local=%MAVEN_REPO%" javafx:run
pause
