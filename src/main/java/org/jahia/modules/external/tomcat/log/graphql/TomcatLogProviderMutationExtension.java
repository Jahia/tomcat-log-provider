package org.jahia.modules.external.tomcat.log.graphql;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLTypeExtension;
import org.jahia.modules.graphql.provider.dxm.DXGraphQLProvider;

@GraphQLTypeExtension(DXGraphQLProvider.Mutation.class)
@GraphQLDescription("Tomcat Log Provider mutations")
public class TomcatLogProviderMutationExtension {

    private TomcatLogProviderMutationExtension() {
    }

    @GraphQLField
    @GraphQLName("tomcatLog")
    @GraphQLDescription("Tomcat Log Provider mutation namespace")
    public static TomcatLogProviderMutation tomcatLog() {
        return new TomcatLogProviderMutation();
    }
}
