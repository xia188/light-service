
package com.xlongwei.graphql;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.networknt.graphql.router.SchemaProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import graphql.schema.idl.TypeRuntimeWiring;

/**
 * Created by steve on 25/03/17.
 */
public class GraphqlSchema implements SchemaProvider {
    private static Logger logger = LoggerFactory.getLogger(SchemaProvider.class);
    private static String schemaName = "schema.graphqls";

    @Override
    public GraphQLSchema getSchema() {
        SchemaParser schemaParser = new SchemaParser();
        TypeDefinitionRegistry typeRegistry = null;

        try (InputStream is = getClass().getClassLoader().getResourceAsStream(schemaName)) {
            typeRegistry = schemaParser.parse(new InputStreamReader(is));
        } catch (IOException e) {
            logger.error("IOException:", e);
        }

        final NumberHolder root = new NumberHolder(5);
        return new SchemaGenerator().makeExecutableSchema(typeRegistry, RuntimeWiring.newRuntimeWiring()
                .type(TypeRuntimeWiring.newTypeWiring("Query")
                        .dataFetcher("hello", (env) -> "world")
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
    }
}
