# Light Hybrid 4J Service

This project is a testing platform for developing light-hybrid-4j services. You can build and test your service
here and then build a small jar file that contains only your handler classes. To deploy your service, just drop
the jar into a directory and start light-hybrid-4j server.

### mobile 新增项目后添加到service/pom.xml，\<module>mobile</module\>
java -jar codegen-cli.jar -f light-hybrid-4j-service -c service/mobile/config.json -m service/mobile/schema.json -o service/mobile

### pom.xml 依赖[phone-number-geo](https://gitee.com/luoyf80/phone-number-geo)，参考ip将依赖一起打包
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
    <groupId>me.ihxq.projects</groupId>
    <artifactId>phone-number-geo</artifactId>
    <version>1.0.9-202108</version>
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
                                    <include>me.ihxq.projects:phone-number-geo</include>
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
```
{
    "type": "java",
    "name": "mobile",
    "request": "launch",
    "mainClass": "com.networknt.server.Server",
    "projectName": "mobile",
    "cwd": "${workspaceFolder}/light-service",
    "vmArgs": "-Dlight-4j-config-dir=gserver/src/main/resources/config -Dlogback.configurationFile=gserver/src/test/resources/logback-test.xml",
}
```
