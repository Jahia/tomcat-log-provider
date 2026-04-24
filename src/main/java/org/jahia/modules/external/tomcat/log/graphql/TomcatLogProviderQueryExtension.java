package org.jahia.modules.external.tomcat.log.graphql;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLTypeExtension;
import org.jahia.modules.external.tomcat.log.TomcatLogDataSource;
import org.jahia.modules.external.tomcat.log.TomcatLogMountPointService;
import org.jahia.modules.graphql.provider.dxm.DXGraphQLProvider;
import org.jahia.modules.graphql.provider.dxm.security.GraphQLRequiresPermission;
import org.jahia.osgi.BundleUtils;

@GraphQLTypeExtension(DXGraphQLProvider.Query.class)
@GraphQLName("TomcatLogProviderQueries")
@GraphQLDescription("Tomcat Log Provider queries")
public class TomcatLogProviderQueryExtension {

    private TomcatLogProviderQueryExtension() {
    }

    @GraphQLField
    @GraphQLName("tomcatLogSettings")
    @GraphQLDescription("Returns the current Tomcat Log Provider settings")
    @GraphQLRequiresPermission("admin")
    public static GqlSettings settings() {
        final TomcatLogMountPointService service = BundleUtils.getOsgiService(TomcatLogMountPointService.class, null);
        final String mountPath = service != null ? service.getMountPath() : TomcatLogMountPointService.DEFAULT_MOUNT_PATH;
        return new GqlSettings(mountPath, TomcatLogDataSource.getTomcatLogPath());
    }

    @GraphQLName("TomcatLogSettings")
    @GraphQLDescription("Tomcat Log Provider settings")
    public static class GqlSettings {

        private final String mountPath;
        private final String logPath;

        public GqlSettings(String mountPath, String logPath) {
            this.mountPath = mountPath;
            this.logPath = logPath;
        }

        @GraphQLField
        @GraphQLName("mountPath")
        @GraphQLDescription("JCR path where Tomcat log files are mounted")
        public String getMountPath() {
            return mountPath;
        }

        @GraphQLField
        @GraphQLName("logPath")
        @GraphQLDescription("Resolved Tomcat log directory path on disk")
        public String getLogPath() {
            return logPath;
        }
    }
}
