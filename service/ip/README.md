# Light Hybrid 4J Service

This project is a testing platform for developing light-hybrid-4j services. You can build and test your service
here and then build a small jar file that contains only your handler classes. To deploy your service, just drop
the jar into a directory and start light-hybrid-4j server.

### ip，新增项目后添加到service/pom.xml，\<module>ip</module\>
java -jar codegen-cli.jar -f light-hybrid-4j-service -c service/ip/config.json -m service/ip/schema.json -o service/ip

### pom.xml，ip服务特有的依赖ip2region打包到ip-3.0.1.jar，多个gservice共用的依赖如mysql、redis等直接加到gserver即可
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
  <groupId>org.lionsoul</groupId>
  <artifactId>ip2region</artifactId>
  <version>1.7.2</version>
</dependency>

            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-shade-plugin</artifactId>
                <version>2.4.3</version>
                <executions>
                    <execution>
                        <phase>package</phase>
                        <goals>
                            <goal>shade</goal>
                        </goals>
                        <configuration>
                            <dependencyReducedPomLocation>${project.build.directory}/dependency-reduced-pom.xml</dependencyReducedPomLocation>
                            <artifactSet>
                                <includes>
                                    <include>org.lionsoul:ip2region</include>
                                </includes>
                            </artifactSet>
                            <transformers>
                                <transformer implementation="org.apache.maven.plugins.shade.resource.ServicesResourceTransformer"/>
                            </transformers>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
```

### vscode
gservice
```
{
    "type": "java",
    "name": "ip",
    "request": "launch",
    "mainClass": "com.networknt.server.Server",
    "projectName": "ip",
    "cwd": "${workspaceFolder}/light-service",
    "vmArgs": "-Dlight-4j-config-dir=gserver/src/main/resources/config -Dlogback.configurationFile=gserver/src/test/resources/logback-test.xml",
}
```
gserver
```
{
    "type": "java",
    "name": "gserver",
    "request": "launch",
    "mainClass": "com.networknt.server.Server",
    "projectName": "gserver",
    "cwd": "${workspaceFolder}/light-service",
    "vmArgs": "-Dlight-4j-config-dir=gserver/src/main/resources/config -Dlogback.configurationFile=gserver/src/test/resources/logback-test.xml",
    "classPaths": [
        "E:\\GITHUB\\xlongwei\\light-service\\gserver\\target\\gserver-3.0.1.jar",
        "E:\\GITHUB\\xlongwei\\light-service\\service\\ip\\target\\ip-3.0.1.jar",
    ],
}
```
git-bash，-cp需要绝对路径
```
java -Dlight-4j-config-dir=gserver/src/main/resources/config -Dlogback.configurationFile=gserver/src/test/resources/logback-test.xml -cp /e/GITHUB/xlongwei/light-service/service/ip/target/ip-3.0.1.jar:/e/GITHUB/xlongwei/light-service/gserver/target/gserver-3.0.1.jar com.networknt.server.Server
```
