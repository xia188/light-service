# light-service

#### Description
light-hybrid-4j and light-graphql-4j service example

#### gserver + gservice*N
单体项目使用gserver就够了，多模块项目可以通过gserver加载多个gservice。hybrid其实是json-rpc，HybridRouter的bodyMap在body的基础上合并了query参数并支持了UTF-8。Handler可以register到Consul，Client可通过host.service.action.version访问已注册服务，这样部分服务就可以实现动态扩容。

#### graphql
graphql是一种新的接口风格