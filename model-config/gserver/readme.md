### gserver
java -jar codegen-cli.jar -f light-hybrid-4j-server -c model-config/gserver/config.json -o gserver

### gserver已添加lombok支持（仅编译不打包），gservice可选使用lombok
```
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <version>1.18.8</version>
    <scope>provided</scope>
</dependency>
```
mvn eclipse:eclipse

### gserver需要被gservice依赖，因此需要install
mvn install -DskipTests -Dmaven.javadoc.skip=true

### gservice
java -jar codegen-cli.jar -f light-hybrid-4j-service -c model-config/gservice/config.json -m model-config/gservice/schema.json -o gservice

### add gserver to gservice, lombok可选
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
mvn eclipse:eclipse

### vscode调试gservice
```
{
    "type": "java",
    "name": "pinyin4j",
    "request": "launch",
    "mainClass": "com.networknt.server.Server",
    "projectName": "gservice",
    "cwd": "${workspaceFolder}/light-service",
    "vmArgs": "-Dlight-4j-config-dir=gserver/src/main/resources/config",
}
```

### vscode调试gserver
```
{
    "type": "java",
    "name": "gserver",
    "request": "launch",
    "mainClass": "com.networknt.server.Server",
    "projectName": "gserver",
    "vmArgs": "-Dlight-4j-config-dir=gserver/src/main/resources/config",
    "classPaths": ["E:\\GITHUB\\xlongwei\\light-service\\gserver\\target\\gserver-3.0.1.jar","E:\\GITHUB\\xlongwei\\light-service\\gservice\\target\\gservice-3.0.1.jar"]
}
```

### 应请求/hybrid/{service}/{action}，gservice应使用HybridUtils.toByteBuffer(String)
curl提交中文也有问题，在Windows下内容为GBK字节，gserver总是使用UTF-8解码，因而出现乱码，表单方式param1=value1&param2=value2提交时会进行UrlDecode因此可以解析%E5%80%BC为值，总之优先使用rest.http来请求测试
```
curl http://localhost:8083/hybrid/gservice/echo \
  -d 'param=%E5%80%BC'
```
