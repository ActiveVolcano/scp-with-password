@echo off
if defined JAVA_HOME (
	set JAVA=%JAVA_HOME%\bin\java
) else if defined JRE_HOME (
	set JAVA=%JRE_HOME%\bin\java
) else (
	set JAVA=java
)
"%JAVA%" --class-path lib\* -Djava.util.logging.config.file=java.util.logging.config Scp.java %*
