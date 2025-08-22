@rem
@rem Copyright 2015 the original author or authors.
@rem
@rem Licensed under the Apache License, Version 2.0 (the "License");
@rem you may not use this file except in compliance with the License.
@rem You may obtain a copy of the License at
@rem
@rem      https://www.apache.org/licenses/LICENSE-2.0
@rem
@rem Unless required by applicable law or agreed to in writing, software
@rem distributed under the License is distributed on an "AS IS" BASIS,
@rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@rem See the License for the specific language governing permissions and
@rem limitations under the License.
@rem
@rem SPDX-License-Identifier: Apache-2.0
@rem

@if "%DEBUG%"=="" @echo off
@rem ##########################################################################
@rem
@rem  VOYAGER_BE startup script for Windows
@rem
@rem ##########################################################################

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
@rem This is normally unused
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%..

@rem Resolve any "." and ".." in APP_HOME to make it shorter.
for %%i in ("%APP_HOME%") do set APP_HOME=%%~fi

@rem Add default JVM options here. You can also use JAVA_OPTS and VOYAGER_BE_OPTS to pass JVM options to this script.
set DEFAULT_JVM_OPTS=

@rem Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if %ERRORLEVEL% equ 0 goto execute

echo. 1>&2
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH. 1>&2
echo. 1>&2
echo Please set the JAVA_HOME variable in your environment to match the 1>&2
echo location of your Java installation. 1>&2

goto fail

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

if exist "%JAVA_EXE%" goto execute

echo. 1>&2
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME% 1>&2
echo. 1>&2
echo Please set the JAVA_HOME variable in your environment to match the 1>&2
echo location of your Java installation. 1>&2

goto fail

:execute
@rem Setup the command line

set CLASSPATH=%APP_HOME%\lib\VOYAGER_BE-0.0.1-SNAPSHOT-plain.jar;%APP_HOME%\lib\spring-boot-devtools-3.4.3.jar;%APP_HOME%\lib\spring-boot-starter-websocket-3.4.3.jar;%APP_HOME%\lib\spring-boot-starter-web-3.4.3.jar;%APP_HOME%\lib\spring-boot-starter-data-jpa-3.4.3.jar;%APP_HOME%\lib\spring-boot-starter-thymeleaf-3.4.3.jar;%APP_HOME%\lib\spring-boot-starter-security-3.4.3.jar;%APP_HOME%\lib\spring-boot-starter-validation-3.4.3.jar;%APP_HOME%\lib\spring-boot-starter-webflux-3.4.3.jar;%APP_HOME%\lib\spring-boot-starter-json-3.4.3.jar;%APP_HOME%\lib\spring-boot-starter-jdbc-3.4.3.jar;%APP_HOME%\lib\spring-boot-starter-3.4.3.jar;%APP_HOME%\lib\commons-fileupload-1.5.jar;%APP_HOME%\lib\commons-io-2.14.0.jar;%APP_HOME%\lib\querydsl-jpa-5.0.0-jakarta.jar;%APP_HOME%\lib\modelmapper-2.3.9.jar;%APP_HOME%\lib\service-0.18.2.jar;%APP_HOME%\lib\client-0.18.2.jar;%APP_HOME%\lib\jjwt-jackson-0.11.5.jar;%APP_HOME%\lib\converter-jackson-2.9.0.jar;%APP_HOME%\lib\mbknor-jackson-jsonschema_2.12-1.0.34.jar;%APP_HOME%\lib\api-0.18.2.jar;%APP_HOME%\lib\jackson-datatype-jsr310-2.18.2.jar;%APP_HOME%\lib\jackson-annotations-2.18.2.jar;%APP_HOME%\lib\jackson-datatype-jdk8-2.18.2.jar;%APP_HOME%\lib\jackson-module-parameter-names-2.18.2.jar;%APP_HOME%\lib\jackson-core-2.18.2.jar;%APP_HOME%\lib\jackson-databind-2.18.2.jar;%APP_HOME%\lib\thymeleaf-extras-springsecurity6-3.1.3.RELEASE.jar;%APP_HOME%\lib\thymeleaf-layout-dialect-3.1.0.jar;%APP_HOME%\lib\jjwt-impl-0.11.5.jar;%APP_HOME%\lib\jjwt-api-0.11.5.jar;%APP_HOME%\lib\mysql-connector-j-9.1.0.jar;%APP_HOME%\lib\spring-boot-autoconfigure-3.4.3.jar;%APP_HOME%\lib\spring-boot-3.4.3.jar;%APP_HOME%\lib\spring-boot-starter-logging-3.4.3.jar;%APP_HOME%\lib\spring-boot-starter-tomcat-3.4.3.jar;%APP_HOME%\lib\spring-data-jpa-3.4.3.jar;%APP_HOME%\lib\jakarta.annotation-api-2.1.1.jar;%APP_HOME%\lib\spring-webmvc-6.2.3.jar;%APP_HOME%\lib\spring-security-web-6.4.3.jar;%APP_HOME%\lib\spring-websocket-6.2.3.jar;%APP_HOME%\lib\spring-webflux-6.2.3.jar;%APP_HOME%\lib\spring-web-6.2.3.jar;%APP_HOME%\lib\spring-security-config-6.4.3.jar;%APP_HOME%\lib\spring-security-core-6.4.3.jar;%APP_HOME%\lib\spring-context-6.2.3.jar;%APP_HOME%\lib\spring-aop-6.2.3.jar;%APP_HOME%\lib\spring-messaging-6.2.3.jar;%APP_HOME%\lib\spring-orm-6.2.3.jar;%APP_HOME%\lib\spring-jdbc-6.2.3.jar;%APP_HOME%\lib\spring-data-commons-3.4.3.jar;%APP_HOME%\lib\spring-tx-6.2.3.jar;%APP_HOME%\lib\spring-beans-6.2.3.jar;%APP_HOME%\lib\spring-expression-6.2.3.jar;%APP_HOME%\lib\spring-core-6.2.3.jar;%APP_HOME%\lib\snakeyaml-2.3.jar;%APP_HOME%\lib\hibernate-core-6.6.8.Final.jar;%APP_HOME%\lib\spring-aspects-6.2.3.jar;%APP_HOME%\lib\querydsl-core-5.1.0.jar;%APP_HOME%\lib\thymeleaf-spring6-3.1.3.RELEASE.jar;%APP_HOME%\lib\adapter-rxjava2-2.9.0.jar;%APP_HOME%\lib\retrofit-2.9.0.jar;%APP_HOME%\lib\thymeleaf-expression-processor-3.1.0.jar;%APP_HOME%\lib\thymeleaf-3.1.3.RELEASE.jar;%APP_HOME%\lib\logback-classic-1.5.16.jar;%APP_HOME%\lib\log4j-to-slf4j-2.24.3.jar;%APP_HOME%\lib\jul-to-slf4j-2.0.16.jar;%APP_HOME%\lib\HikariCP-5.1.0.jar;%APP_HOME%\lib\slf4j-api-2.0.16.jar;%APP_HOME%\lib\tomcat-embed-el-10.1.36.jar;%APP_HOME%\lib\hibernate-validator-8.0.2.Final.jar;%APP_HOME%\lib\groovy-extensions-2.0.0.jar;%APP_HOME%\lib\groovy-4.0.25.jar;%APP_HOME%\lib\spring-boot-starter-reactor-netty-3.4.3.jar;%APP_HOME%\lib\spring-jcl-6.2.3.jar;%APP_HOME%\lib\tomcat-embed-websocket-10.1.36.jar;%APP_HOME%\lib\tomcat-embed-core-10.1.36.jar;%APP_HOME%\lib\micrometer-observation-1.14.4.jar;%APP_HOME%\lib\jakarta.persistence-api-3.1.0.jar;%APP_HOME%\lib\jakarta.transaction-api-2.0.1.jar;%APP_HOME%\lib\jboss-logging-3.6.1.Final.jar;%APP_HOME%\lib\hibernate-commons-annotations-7.0.3.Final.jar;%APP_HOME%\lib\jandex-3.2.0.jar;%APP_HOME%\lib\classmate-1.7.0.jar;%APP_HOME%\lib\byte-buddy-1.15.11.jar;%APP_HOME%\lib\jaxb-runtime-4.0.5.jar;%APP_HOME%\lib\jaxb-core-4.0.5.jar;%APP_HOME%\lib\jakarta.xml.bind-api-4.0.2.jar;%APP_HOME%\lib\jakarta.inject-api-2.0.1.jar;%APP_HOME%\lib\antlr4-runtime-4.13.0.jar;%APP_HOME%\lib\aspectjweaver-1.9.22.1.jar;%APP_HOME%\lib\mysema-commons-lang-0.2.4.jar;%APP_HOME%\lib\rxjava-2.0.0.jar;%APP_HOME%\lib\reactor-netty-http-1.2.3.jar;%APP_HOME%\lib\reactor-netty-core-1.2.3.jar;%APP_HOME%\lib\reactor-core-3.7.3.jar;%APP_HOME%\lib\reactive-streams-1.0.4.jar;%APP_HOME%\lib\scala-library-2.12.8.jar;%APP_HOME%\lib\validation-api-2.0.1.Final.jar;%APP_HOME%\lib\classgraph-4.8.21.jar;%APP_HOME%\lib\okhttp-3.14.9.jar;%APP_HOME%\lib\jtokkit-0.5.1.jar;%APP_HOME%\lib\jakarta.validation-api-3.0.2.jar;%APP_HOME%\lib\ognl-3.3.4.jar;%APP_HOME%\lib\attoparser-2.0.7.RELEASE.jar;%APP_HOME%\lib\unbescape-1.1.6.RELEASE.jar;%APP_HOME%\lib\logback-core-1.5.16.jar;%APP_HOME%\lib\log4j-api-2.24.3.jar;%APP_HOME%\lib\micrometer-commons-1.14.4.jar;%APP_HOME%\lib\angus-activation-2.0.2.jar;%APP_HOME%\lib\jakarta.activation-api-2.1.3.jar;%APP_HOME%\lib\okio-1.17.2.jar;%APP_HOME%\lib\spring-security-crypto-6.4.3.jar;%APP_HOME%\lib\javassist-3.29.0-GA.jar;%APP_HOME%\lib\netty-codec-http2-4.1.118.Final.jar;%APP_HOME%\lib\netty-handler-proxy-4.1.118.Final.jar;%APP_HOME%\lib\netty-codec-http-4.1.118.Final.jar;%APP_HOME%\lib\netty-resolver-dns-native-macos-4.1.118.Final-osx-x86_64.jar;%APP_HOME%\lib\netty-resolver-dns-classes-macos-4.1.118.Final.jar;%APP_HOME%\lib\netty-resolver-dns-4.1.118.Final.jar;%APP_HOME%\lib\netty-transport-native-epoll-4.1.118.Final-linux-x86_64.jar;%APP_HOME%\lib\txw2-4.0.5.jar;%APP_HOME%\lib\istack-commons-runtime-4.1.2.jar;%APP_HOME%\lib\netty-handler-4.1.118.Final.jar;%APP_HOME%\lib\netty-codec-dns-4.1.118.Final.jar;%APP_HOME%\lib\netty-codec-socks-4.1.118.Final.jar;%APP_HOME%\lib\netty-codec-4.1.118.Final.jar;%APP_HOME%\lib\netty-transport-classes-epoll-4.1.118.Final.jar;%APP_HOME%\lib\netty-transport-native-unix-common-4.1.118.Final.jar;%APP_HOME%\lib\netty-transport-4.1.118.Final.jar;%APP_HOME%\lib\netty-buffer-4.1.118.Final.jar;%APP_HOME%\lib\netty-resolver-4.1.118.Final.jar;%APP_HOME%\lib\netty-common-4.1.118.Final.jar


@rem Execute VOYAGER_BE
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %VOYAGER_BE_OPTS%  -classpath "%CLASSPATH%" com.planty.PlantyApplication %*

:end
@rem End local scope for the variables with windows NT shell
if %ERRORLEVEL% equ 0 goto mainEnd

:fail
rem Set variable VOYAGER_BE_EXIT_CONSOLE if you need the _script_ return code instead of
rem the _cmd.exe /c_ return code!
set EXIT_CODE=%ERRORLEVEL%
if %EXIT_CODE% equ 0 set EXIT_CODE=1
if not ""=="%VOYAGER_BE_EXIT_CONSOLE%" exit %EXIT_CODE%
exit /b %EXIT_CODE%

:mainEnd
if "%OS%"=="Windows_NT" endlocal

:omega
