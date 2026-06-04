@REM Maven Wrapper for Windows

@echo off

setlocal



set "MAVEN_PROJECTBASEDIR=%~dp0"

set "WRAPPER_JAR=%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.jar"



if not exist "%WRAPPER_JAR%" (

  echo Downloading Maven Wrapper...

  powershell -NoProfile -Command ^

    "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; ^

     Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar' -OutFile '%WRAPPER_JAR%'"

)



set "JAVA_EXE=java"

if defined JAVA_HOME set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"



"%JAVA_EXE%" -classpath "%WRAPPER_JAR%" "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" org.apache.maven.wrapper.MavenWrapperMain %*



endlocal

