### codegen-cli
curl -O http://t.xlongwei.com/windows/codegen-cli.jar

### gserver
java -jar codegen-cli.jar -f light-hybrid-4j-server -c model-config/gserver/config.json -o gserver

1. 修改service.yml切换至HybridRouter，支持UTF-8编码（RpcRouter仅支持ISO-8859-1），支持GET|PUT|POST|DELETE /hybrid/{service}/{action}
2. gservice添加依赖gserver，使用HybridUtils获取参数和响应内容（NioUtils响应中文时抛异常）
```
<dependency>
    <groupId>com.xlongwei</groupId>
    <artifactId>gserver</artifactId>
    <version>3.0.1</version>
</dependency>
```

### gservice
java -jar codegen-cli.jar -f light-hybrid-4j-service -c model-config/gservice/config.json -m model-config/gservice/schema.json -o gservice

1. hello服务的schema为空，请求json可以没有data，Test示例了get方式请求hello服务
2. echo服务的schema.type为object，请求json不可以缺少data，hybrid-security.yml可开启Authorization和scope验证

### vscode
mvn eclipse:eclipse
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

### Test
```
mvn package -DskipTests -Dmaven.javadoc.skip=true
# vscode方便调试，发版时gservice-3.0.1.jar可以动态控制服务列表
java -jar gserver/target/gserver-3.0.1.jar:/e/GITHUB/xlongwei/light-service/gservice/target/gservice-3.0.1.jar
# get请求时，cmd命令需encodeURIComponent转码：cmd={"host": "xlongwei.com","service": "gservice","action": "hello","version": "0.0.1"}
curl http://localhost:8083/api/json?cmd=%7B%22host%22%3A%20%22xlongwei.com%22%2C%22service%22%3A%20%22gservice%22%2C%22action%22%3A%20%22hello%22%2C%22version%22%3A%20%220.0.1%22%7D
# post请求时，方便编辑json正文，vscode优先考虑rest-client插件，参考示例rest.http
curl -X POST http://localhost:8083/api/json \
  -H 'cache-control: no-cache' \
  -H 'content-type: application/json' \
  -d '{
  "host": "xlongwei.com",
  "service": "gservice",
  "action": "echo",
  "version": "0.0.1",
  "data": { "hello":"world" }
}'
```
