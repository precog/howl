@echo off

set JUNIT_HOME=c:\java\junit3.8.1

set java_opts=-cp bin;%EMMA_HOME%\lib\emma.jar;%JUNIT_HOME%\bin
set java_opts=%java_opts% -showversion -Xnoclassgc -ea -esa
set java_opts=%java_opts% -server

setlocal
set JAVA_HOME=C:\java\j2sdk1.4.2_05
set mode=1.4

if "%1"=="tiger" (
set JAVA_HOME=C:\java\jdk1.5.0
set mode=1.5
)

:: JRockit options that may be valuable
:: -Xmanagement 
:: -Djrockit.lockprofiling -XXjra:delay=10,recordingtime=30

if "%1"=="jrockit" (
set JAVA_HOME=C:\java\jrockit-j2sdk1.4.2_04
set mode=1.4
set PATH=%java_home%\bin;%path%
set java_opts=%java_opts% -Djrockit.lockprofiling -XXjra:delay=10,recordingtime=30
)

if "%1"=="ibm" (
set JAVA_HOME=C:\java\IBM\Java141\jre
set mode=1.4
set PATH=%java_home%\jre\bin;%path%
)

set emma=
if "%1"=="emma" (
set emma=emmarun -r html -sp ./src/java;./src/test -ix -org.objectweb.howl.log.*Test* -ix -org.objectweb.howl.log.xa.*Worker* -ix -org.objectweb.howl.log.xa.*Test* -ix -junit.* -cp bin;%junit_home%/bin
)

pushd ..
echo on
%java_home%\bin\java %java_opts% %emma%  org.objectweb.howl.log.allTest
popd
