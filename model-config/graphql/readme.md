### graphql
java -jar codegen-cli.jar -f light-graphql-4j -c model-config/graphql/config.json -m model-config/graphql/schema.graphqls -o graphql
```
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
    "name": "graphql",
    "request": "launch",
    "mainClass": "com.networknt.server.Server",
    "projectName": "graphql",
    "cwd": "${workspaceFolder}/light-service",
    "vmArgs": "-Dlight-4j-config-dir=graphql/src/main/resources/config",
}
```

### hello
```
return GraphQLSchema.newSchema()
        .query(GraphQLObjectType.newObject().name("helloWorldQuery")
                .field(GraphQLFieldDefinition.newFieldDefinition().type(Scalars.GraphQLString).name("hello")
                        .staticValue("world").build()))
        .build();
curl -k -H 'Content-Type:application/json' -X POST http://localhost:8083/graphql -d '{"query":"{ hello }"}'
```

### query and mutation
```
final NumberHolder root = new NumberHolder(5);
return new SchemaGenerator().makeExecutableSchema(typeRegistry, RuntimeWiring.newRuntimeWiring()
        .type(TypeRuntimeWiring.newTypeWiring("Query")
                .dataFetcher("hello", (env) -> "world") // hello: String
                .dataFetcher("numberHolder", (env) -> root))
        .type(TypeRuntimeWiring.newTypeWiring("Mutation")
                .dataFetcher("changeTheNumber", (env) -> {
                    root.setTheNumber(env.getArgument("newNumber"));
                    return root;
                })
                .dataFetcher("failToChangeTheNumber", (env) -> {
                    throw new RuntimeException("Simulate failing to change the number.");
                }))
        .build());
```

### rest client
```
POST http://localhost:8083/graphql HTTP/1.1
X-Request-Type: GraphQL

query {
  numberHolder {
    theNumber
  }
}
```
