# Light Hybrid 4J Service

This project is a testing platform for developing light-hybrid-4j services. You can build and test your service
here and then build a small jar file that contains only your handler classes. To deploy your service, just drop
the jar into a directory and start light-hybrid-4j server.

### apijson，新增项目后添加到service/pom.xml，\<module>apijson</module\>
java -jar codegen-cli.jar -f light-hybrid-4j-service -c service/apijson/config.json -m service/apijson/schema.json -o service/apijson

### pom.xml，mysql作为公共依赖已添加到gserver，若使用APIJSON-DEMO则git clone https://gitee.com/APIJSON/APIJSON-Demo，进入APIJSON-Java-Server/APIJSONBoot，注释pom.xml里的spring-boot-maven-plugin然后mvn install
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
<dependency>
    <groupId>com.github.APIJSON</groupId>
    <artifactId>apijson-framework</artifactId>
    <version>5.2.0</version>
</dependency>
<dependency>
    <groupId>apijson.boot</groupId>
    <artifactId>apijson-boot</artifactId>
    <version>5.2.0</version>
    <exclusions>
        <exclusion>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

### vscode
```
{
    "type": "java",
    "name": "apijson",
    "request": "launch",
    "mainClass": "com.networknt.server.Server",
    "projectName": "apijson",
    "cwd": "${workspaceFolder}/light-service",
    "vmArgs": "-Dlight-4j-config-dir=gserver/src/main/resources/config -Dlogback.configurationFile=gserver/src/test/resources/logback-test.xml",
    "env": {
        "apijson.enabled":true,
        "apijson.debug":false,
    }
}
```

### apijson.yml
```
ds: ${apijson.ds:apijson}
enabled: ${apijson.enabled:false}
debug: ${apijson.debug:false}
```

### session序列化，以便支持redis存储
```
1，使用JDK序列化机制，将DemoSession序列化为byte[]
2，使用json序列化，需要将相关类加入autoType
ParserConfig.getGlobalInstance().addAccept("apijson.demo.model.");
JSON.toJSONString(session, SerializerFeature.WriteClassName);
```