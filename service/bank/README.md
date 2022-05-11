# Light Hybrid 4J Service

This project is a testing platform for developing light-hybrid-4j services. You can build and test your service
here and then build a small jar file that contains only your handler classes. To deploy your service, just drop
the jar into a directory and start light-hybrid-4j server.

### bank，新增项目后添加到service/pom.xml，\<module>bank</module\>
java -jar codegen-cli.jar -f light-hybrid-4j-service -c service/bank/config.json -m service/bank/schema.json -o service/bank

### pom.xml，mysql作为公共依赖已添加到gserver
mvn eclipse:eclipse -f service/pom.xml
```
<dependency>
    <groupId>com.xlongwei</groupId>
    <artifactId>gserver</artifactId>
    <version>3.0.1</version>
</dependency>
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <version>1.18.8</version>
    <scope>provided</scope>
</dependency>
```

### vscode
```
{
    "type": "java",
    "name": "bank",
    "request": "launch",
    "mainClass": "com.networknt.server.Server",
    "projectName": "bank",
    "cwd": "${workspaceFolder}/light-service",
    "vmArgs": "-Dlight-4j-config-dir=gserver/src/main/resources/config -Dlogback.configurationFile=gserver/src/test/resources/logback-test.xml",
}
```
