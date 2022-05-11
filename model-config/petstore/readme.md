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
