These are the SPL assignments.

Topics:
    1. Pointers and memory.
    2. Concurrency.
    3. Servers.

Compiling and running:
    1. make; ./bin/warehouse configFileExample.txt;
    2. mvn clean compile exec:java
    3. server: mvn compile; mvn exec:java -Dexec.mainClass="bgu.spl.net.impl.tftp.TftpServer" -Dexec.args="7777";
       client: mvn compile; mvn exec:java -Dexec.mainClass="bgu.spl.net.impl.tftp.TftpClient" -Dexec.args="127.0.0.1 7777"
