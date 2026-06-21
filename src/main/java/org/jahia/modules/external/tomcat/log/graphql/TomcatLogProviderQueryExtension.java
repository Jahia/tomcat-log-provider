package org.jahia.modules.external.tomcat.log.graphql;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLTypeExtension;
import org.jahia.modules.graphql.provider.dxm.DXGraphQLProvider;

@GraphQLTypeExtension(DXGraphQLProvider.Query.class)
@GraphQLDescription("Tomcat Log Provider queries")
public class TomcatLogProviderQueryExtension {

    private TomcatLogProviderQueryExtension() {
    }

    @GraphQLField
    @GraphQLName("tomcatLog")
    @GraphQLDescription("Tomcat Log Provider query namespace")
    public static TomcatLogProviderQuery tomcatLog() {
        return new TomcatLogProviderQuery();
    }
}
