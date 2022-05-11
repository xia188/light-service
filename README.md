# light-service

#### Description
light-hybrid-4j、light-graphql-4j、light-rest-4j service example

#### gserver + gservice*N
单体项目使用gserver就够了，多模块项目可以通过gserver加载多个gservice。hybrid其实是json-rpc，HybridRouter的bodyMap在body的基础上合并了query参数并支持了UTF-8。Handler可以register到Consul，Client可通过host.service.action.version访问已注册服务，这样部分服务就可以实现动态扩容。

#### graphql
graphql是一种新的接口风格，一切从设计graphql的Schema规范开始，请求内容非常灵活。

### petstore
petstore是restful接口风格，一切从设计openapi.yaml规范开始，通过请求方法GET POST DELETE等表达语义（PUT PATCH使用POST实现可以适当简化设计）