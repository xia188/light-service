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

### hybrid的bodyMap与schema.json
hybrid的bodyMap是将FormData表单或json正文处理为Map<String,Object>，再合并queryParameters而来，同样使用host、service、action、version来定位serviceId，扩展在于service、action可以通过请求路径提供，host、version有默认值，也可以通过请求参数或正文提供。

schema.json可以严格地校验json正文里的boolean、number等类型，但混合queryParameter之后的bodyMap，param对应的value类型可能是string，因此通常需使用oneOf:[{"type":"boolean"},{"type":"string"}]来校验，若仅使用其中一种类型则另一种类型的值会校验失败。

### enableRegistry

1. rpc-router.yml，配置registerService: true，注册Handler到Server，由后者注册到registry，可通过环境变量rpc-router.registerService控制
2. server.yml，配置enableRegistry: true，注册服务到registry，可通过环境变量server.enableRegistry控制
3. client.yml，配置verifyHostname: false和loadTrustStore: false，否则需提供正确的client.truststore文件
4. consul.yml，配置consulUrl: ${consul.consulUrl:http://localhost:8500}，通过环境变量consul.consulUrl指定consul
5. service.yml，配置Registry、ConsulClient、Cluster等，到这里gservice就可以注册成功了
6. hybrid定义的serviceId=host/service/action/version，注册时serviceId=host.service.action.version，consul里id=ip:serviceId:port
7. HybridHandler，通过serviceId没有找到Handler时，再通过registry查找可用服务，cluster.serviceToUrl(protocol,serviceId,tag=environment,requestKey=null)，最后通过Http2Client转发请求，暂支持json转发，不支持文件上传
8. secret.yml，配置consulToken=，默认consul不要求token。service.yml可以配置直连服务，这样就可以不依赖consul

测试：
consul运行，
```
setsid consul agent -server -bootstrap-expect 1 -ui -node=dc1 -bind 127.0.0.1 -client=0.0.0.0 -data-dir /soft/consul/data -config-dir /soft/consul/config &> /var/log/consul.log &
```
gserver转发请求：监听8082端口，配置consul地址（直连时不需要）
```
{
    "type": "java",
    "name": "gserver",
    "request": "launch",
    "mainClass": "com.networknt.server.Server",
    "projectName": "gserver",
    "cwd": "${workspaceFolder}/light-service",
    "vmArgs": "-Dlight-4j-config-dir=gserver/src/main/resources/config -Dlogback.configurationFile=gserver/src/test/resources/logback-test.xml",
    "env": {
        "rpc-router.registerService":true,
        "consul.consulUrl":"http://115.28.229.158:8500",
        "server.httpPort":8082
    }
}
```
gservice注册服务：监听8083端口（默认值），配置consul地址，开启registry注册
```
{
    "type": "java",
    "name": "gservice",
    "request": "launch",
    "mainClass": "com.networknt.server.Server",
    "projectName": "gservice",
    "cwd": "${workspaceFolder}/light-service",
    "vmArgs": "-Dlight-4j-config-dir=gserver/src/main/resources/config -Dlogback.configurationFile=gserver/src/test/resources/logback-test.xml",
    "env": {
        "consul.consulUrl":"http://115.28.229.158:8500",
        "rpc-router.registerService":true,
        "server.enableRegistry":true
    }
}
```
请求gserver：cmd={"hello":"中文"}，注意：/api/json处理中文会抛异常，应请求/hybrid
```
curl http://localhost:8082/hybrid/gservice/echo?cmd=%7B%22hello%22%3A%22%E4%B8%AD%E6%96%87%22%7D
```
