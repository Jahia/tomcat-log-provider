package org.jahia.modules.external.tomcat.log.graphql;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLTypeExtension;
import org.jahia.modules.graphql.provider.dxm.DXGraphQLProvider;
import org.jahia.modules.graphql.provider.dxm.security.GraphQLRequiresPermission;
import org.jahia.osgi.BundleUtils;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.Hashtable;

@GraphQLTypeExtension(DXGraphQLProvider.Mutation.class)
@GraphQLName("TomcatLogProviderMutations")
@GraphQLDescription("Tomcat Log Provider mutations")
public class TomcatLogProviderMutationExtension {

    private static final Logger LOGGER = LoggerFactory.getLogger(TomcatLogProviderMutationExtension.class);

    private TomcatLogProviderMutationExtension() {
    }

    @GraphQLField
    @GraphQLName("tomcatLogSaveSettings")
    @GraphQLDescription("Saves the Tomcat Log Provider settings. The provider will remount at the new JCR path.")
    @GraphQLRequiresPermission("admin")
    public static Boolean saveSettings(
            @GraphQLName("mountPath") @GraphQLDescription("JCR path where Tomcat log files should be mounted") String mountPath) {
        try {
            final ConfigurationAdmin configAdmin = BundleUtils.getOsgiService(ConfigurationAdmin.class, null);
            if (configAdmin == null) {
                return Boolean.FALSE;
            }
            final Configuration config = configAdmin.getConfiguration("org.jahia.modules.tomcatlogprovider", null);
            Dictionary<String, Object> props = config.getProperties();
            if (props == null) {
                props = new Hashtable<>();
            }
            if (mountPath != null && !mountPath.isEmpty()) {
                props.put("mountPath", mountPath);
            }
            config.update(props);
            return Boolean.TRUE;
        } catch (Exception e) {
            LOGGER.error("Failed to save Tomcat log provider settings", e);
            return Boolean.FALSE;
        }
    }
}
