### petstore
java -jar codegen-cli.jar -f openapi -m model-config/petstore/openapi.yaml -c model-config/petstore/config.json -o petstore

### openapi.yaml
优先定义接口，约定校验条件，生成接口文档。rest有面向资源的特性，请求方法也表达操作语义。

1. /pets，GET查询，POST新增（或全量更新/pets/{petId}）
2. /pets/{petId}，GET详情，DELETE删除（使用POST全量更新替代：PATCH部分更新，PUT覆盖更新）

### pom.xml，补充上fastscanner版本，可选lombok
```
<version.fastscanner>2.18.1</version.fastscanner>
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <version>1.18.8</version>
    <scope>provided</scope>
</dependency>
```

### vscode
mvn eclipse:eclipse
```
{
    "type": "java",
    "name": "petstore",
    "request": "launch",
    "mainClass": "com.networknt.server.Server",
    "projectName": "petstore",
    "cwd": "${workspaceFolder}/light-service",
    "vmArgs": "-Dlight-4j-config-dir=petstore/src/main/resources/config -Dlogback.configurationFile=gserver/src/test/resources/logback-test.xml",
}
```

### handler.yml
Handler的配置有三种方式，参考light-example-4j/middleware-performance。处理链chain里面，OpenApiHandler负责解析endpoint并保存到autid_info，ValidatorHandler负责根据规范校验参数，handler.yml负责路由请求。

1，endpoint-individual，默认
```
handlers:
  - com.xlongwei.petstore.handler.PetsGetHandler
paths:
  - path: '/v1/pets'
    method: 'GET'
    exec:
      - default
      - com.xlongwei.petstore.handler.PetsGetHandler
```

2，endpoint-source，OpenApiEndpointSource、OpenApiPathHandler，新增接口后只需补充case语句即可。可选扩展OpenapiHttpHandler，增加方法String getEndpoint()，即可通过包扫描定位业务Handler。
```
handlers:
  - com.networknt.petstore.handler.OpenApiPathHandler@openapi-handler
paths:
  - source: com.networknt.openapi.OpenApiEndpointSource
    exec: 
      - default
      - openapi-handler
public class OpenApiPathHandler implements HttpHandler {
    PetsGetHandler petsGetHandler = new PetsGetHandler();
    PetsPetIdGetHandler petsPetIdGetHandler = new PetsPetIdGetHandler();
    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        Map<String, Object> auditInfo = exchange.getAttachment(AttachmentConstants.AUDIT_INFO);
        String endpoint = (String)auditInfo.get(Constants.ENDPOINT_STRING);
        switch(endpoint) {
            case "/pets@get":
                petsGetHandler.handleRequest(exchange);
                break;
            case "/pets/{petId}@get":
                petsPetIdGetHandler.handleRequest(exchange);
                break;
            default:
                throw new Exception(String.format("Unsupported endpoint %s", endpoint));
        }
    }
}
```

3，service-config，在service.yml配置以下PathHandlerProvider
```
public class PathHandlerProvider implements HandlerProvider {
    @Override
    public HttpHandler getHandler() {
        return Handlers.routing()
            .add(Methods.GET, "/v1/health", new HealthGetHandler())
            .add(Methods.GET, "/v1/server/info", new ServerInfoGetHandler())
            .add(Methods.GET, "/v1/pets", new PetsGetHandler())
            .add(Methods.DELETE, "/v1/pets/{petId}", new PetsPetIdDeleteHandler())
        ;
    }
}
```
