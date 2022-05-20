scp with password
=================================
No need to confirm fingerprint,
no need to type password in keyboard,
just put password as command-line argument to scp,
suit for using in scripts.

The recursive (-r) option is not supported yet.

Usage as standalone run
=======================
0. Install Java 11 or above.
1. Download, unzip from the Releases page.
2. Run in command prompt, arguments are same as the standard ssh command, plus --password option, like
```bat
scp-password --password OpenSesame c:\hello.txt alibaba@192.168.0.1:/tmp
```
or
```bat
scp-password -l alibaba --password OpenSesame -P 22 c:\hello.txt alibaba@192.168.0.1:/tmp
```

Specify connection timeout in seconds:
```bat
scp-password -o ConnectTimeout=10 --password OpenSesame c:\hello.txt alibaba@192.168.0.1:/tmp
```
or using a new option name to keep the style:
```bat
scp-password --connect-timeout 10 --password OpenSesame c:\hello.txt alibaba@192.168.0.1:/tmp
```

Get full command-line usage:
```bat
scp-password --help
```


Usage as Java package
=====================
+ In Maven pom.xml:  
See Packages page.

+ In Java code:
```java
import cn.nhcqc.sshex.Scp;

var config = new Scp.Config ();
//  set config field values
new Scp ().run (config);
```


License
==============
LGPL-2.1 (GNU Lesser General Public License).

See file LICENSE for details.


How to compile
==============
0. Install Apache Maven 3
1. Run
```bat
mvn package
```
2. In the target folder, those listed below are for standalone run:
   + *.bat
   + *.java
   + *.ini
   + lib


Development Notes
=================
(Dependencies)

sshj
----
examples  
https://github.com/hierynomus/sshj/tree/master/examples/src/main/java/net/schmizz/sshj/examples

com.hierynomus:sshj:0.33.0  
https://search.maven.org/artifact/com.hierynomus/sshj

```xml
<dependency>
  <groupId>com.hierynomus</groupId>
  <artifactId>sshj</artifactId>
  <version>0.33.0</version>
</dependency>
```

Apache Commons CLI
------------------
Using  
https://commons.apache.org/proper/commons-cli/usage.html

```xml
<dependency>
  <groupId>commons-cli</groupId>
  <artifactId>commons-cli</artifactId>
  <version>1.5.0</version>
</dependency>
```

SLF4J
-----
user manual  
https://www.slf4j.org/manual.html

java.util.logging
-----------------
config file  
http://www.javapractices.com/topic/TopicAction.do?Id=143
