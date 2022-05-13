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
mvn install -DskipTests -Dmaven.javadoc.skip=true -f gserver/pom.xml

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

### enableRegistry与DirectRegistry
direct直连相比consul要简单一些，代码已调整为优先使用直连。环境变量控制说明：gserver网关，rpc-router.registerService=true；gservice直连，默认false即可；gservice注册consul，rpc-router.registerService=true，server.enableRegistry=true。

1. rpc-router.yml，配置registerService: true，注册Handler到Server，由后者注册到registry，可通过环境变量rpc-router.registerService控制
2. server.yml，配置enableRegistry: true，注册服务到registry，可通过环境变量server.enableRegistry控制
3. client.yml，配置verifyHostname: false和loadTrustStore: false，否则需提供正确的client.truststore文件
4. consul.yml，配置consulUrl: ${consul.consulUrl:http://localhost:8500}，通过环境变量consul.consulUrl指定consul
5. service.yml，配置Registry、ConsulClient、Cluster等，到这里gservice就可以注册成功了
6. hybrid定义的serviceId=host/service/action/version，注册时serviceId=host.service.action.version，consul里id=ip:serviceId:port
7. HybridHandler，通过serviceId没有找到Handler时，再通过registry查找可用服务，cluster.serviceToUrl(protocol,serviceId,tag=environment,requestKey=null)，最后通过Http2Client转发请求，暂支持json转发，不支持文件上传
8. secret.yml，配置consulToken=，默认consul不要求token。service.yml可以配置直连服务，这样就可以不依赖consul

测试：
consul运行，直连时不需要
```
setsid consul agent -server -bootstrap-expect 1 -ui -node=dc1 -bind 127.0.0.1 -client=0.0.0.0 -data-dir /soft/consul/data -config-dir /soft/consul/config &> /var/log/consul.log &
```
service.yml配置，gservice时绑定到gserver一起运行的，它们可以使用同一套配置，但是独立运行时需避免端口重复
```
- com.networknt.registry.URL:
  - com.networknt.registry.URLImpl:
      protocol: light
      host: localhost
      port: 8080
      path: consul
      parameters:
        registryRetryPeriod: '30000'
        # 直连时手动配置服务端点，逗号分隔多个地址
        xlongwei.com.gservice.echo.0.0.1: http://localhost:8083
        xlongwei.com.gservice.hello.0.0.1: http://localhost:8083,http://localhost:8084
- com.networknt.consul.client.ConsulClient:
  - com.networknt.consul.client.ConsulClientImpl
- com.networknt.registry.Registry:
  # - com.networknt.consul.ConsulRegistry
  - com.networknt.registry.support.DirectRegistry
- com.networknt.balance.LoadBalance:
  - com.networknt.balance.RoundRobinLoadBalance
- com.networknt.cluster.Cluster:
  - com.networknt.cluster.LightCluster
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
        "rpc-router.registerService":true, //gserver作网关时，没有Handler需要注册，打开此开关可以通过cluster调用其他服务
        // "consul.consulUrl":"http://115.28.229.158:8500", //直连时不需要consul
        "server.httpPort":8082 //修改../rest.http里@host=http://localhost:8082即可通过gserver访问8083的gservice
    }
}
```
gservice注册服务：监听8083端口（默认值），配置consul地址（直连时不需要），开启registry注册
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
        // "consul.consulUrl":"http://115.28.229.158:8500", //直连时不需要consul
        // "rpc-router.registerService":true, //是否委托Server将Handler注册到consul，直连时不需要
        // "server.enableRegistry":true //直连时不需要，使用consul时则这两个开关都打开
        // "server.httpPort":8084 //默认8083运行一个gservice，然后8084再运行一个，访问8082的gserver时即可两个gservice轮流调。如果没有运行8084的gservice，则请求会有半数失败
    }
}
```
请求gserver：cmd={"hello":"中文"}，注意：/api/json处理中文会抛异常，应请求/hybrid
```
curl http://localhost:8082/hybrid/gservice/echo?cmd=%7B%22hello%22%3A%22%E4%B8%AD%E6%96%87%22%7D
```

### mysql
mysql作为公共依赖建议在gserver引入，有三种方案：1，service.yml配置HikariDataSource；2，在datasource.yml配置多个数据源，参考light-example-4j/common/multidb/dbconfig里DbStartupHookProvider加载多个数据源；3，引入data-source构件配置MysqlDataSource等数据源。本示例参考方案2（方案1配置多个数据源时抛异常SQLFeatureNotSupportedException），并调整了依赖版本。
```
<version.hikaricp>4.0.3</version.hikaricp>
<version.mysql>5.1.49</version.mysql>
<dependency>
    <groupId>com.zaxxer</groupId>
    <artifactId>HikariCP</artifactId>
    <version>${version.hikaricp}</version>
</dependency>
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <version>${version.mysql}</version>
</dependency>
```
service.yml，增加了DbStartupHookProvider，增加了HybridHandler.init()支持初始化数据
```
- com.networknt.server.StartupHookProvider:
  # registry all service handlers by from annotations
  - com.networknt.rpc.router.RpcStartupHookProvider
  - com.networknt.rpc.router.DbStartupHookProvider
```
Handler.java
```
class Handler implements HybridHandler{
  DataSource ds = null;
    @Override
    public void init() {
        ds = DbStartupHookProvider.dbMap.get("apijson");
    }
    @Override
    public ByteBuffer handle(HttpServerExchange exchange, Object input) {
        try (Connection connection = ds.getConnection();
                PreparedStatement statement = connection
                        .prepareStatement("select name from idcard where code in (?,?,?)")) {
            statement.setString(1, area1);
            statement.setString(2, area2);
            statement.setString(3, area3);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                list.add(resultSet.getString("name"));
            }
            log.info("bank_card={} is ok", resultSet.getRow());
        } catch (Exception e) {
            log.warn("fail to load bank_card", e);
        }
    }    
}
```
