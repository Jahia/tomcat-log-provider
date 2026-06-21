package org.jahia.community.external.tomcat.log.graphql;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import org.jahia.community.external.tomcat.log.TomcatLogDataSource;
import org.jahia.community.external.tomcat.log.TomcatLogMountPointService;
import org.jahia.modules.graphql.provider.dxm.security.GraphQLRequiresPermission;
import org.jahia.osgi.BundleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@GraphQLName("TomcatLogProviderQuery")
@GraphQLDescription("Tomcat Log Provider queries")
public class TomcatLogProviderQuery {

    private static final Logger LOGGER = LoggerFactory.getLogger(TomcatLogProviderQuery.class);
    private static final int DEFAULT_TAIL_LINES = 200;
    private static final int MAX_TAIL_LINES = 5000;
    // 256 KB is ample for 200 typical log lines (~200 bytes each)
    private static final int TAIL_CHUNK_BYTES = 256 * 1024;

    @GraphQLField
    @GraphQLName("settings")
    @GraphQLDescription("Returns the current Tomcat Log Provider settings")
    @GraphQLRequiresPermission("tomcatLogProviderAdmin")
    public GqlSettings settings() {
        final TomcatLogMountPointService service = BundleUtils.getOsgiService(TomcatLogMountPointService.class, null);
        final String mountPath = service != null ? service.getMountPath() : TomcatLogMountPointService.DEFAULT_MOUNT_PATH;
        return new GqlSettings(mountPath, TomcatLogDataSource.getTomcatLogPath());
    }

    @GraphQLField
    @GraphQLName("tail")
    @GraphQLDescription("Returns the last N lines of jahia.log in chronological order")
    @GraphQLRequiresPermission("tomcatLogProviderAdmin")
    public List<String> logTail(
            @GraphQLName("lines") @GraphQLDescription("Number of lines to return; defaults to 200") Integer lines) {
        final String logDir = TomcatLogDataSource.getTomcatLogPath();
        if (logDir == null) {
            LOGGER.warn("catalina.base system property is not set; cannot tail log file");
            return Collections.emptyList();
        }
        final File logFile = new File(logDir, "jahia.log");
        final int requestedLines = lines != null && lines > 0 ? lines : DEFAULT_TAIL_LINES;
        final int cappedLines = Math.min(requestedLines, MAX_TAIL_LINES);
        try {
            return tailFile(logFile, cappedLines);
        } catch (IOException e) {
            LOGGER.error("Cannot read {}", logFile.getAbsolutePath(), e);
            return Collections.emptyList();
        }
    }

    private static List<String> tailFile(File file, int maxLines) throws IOException {
        if (!file.exists() || file.length() == 0) {
            return Collections.emptyList();
        }
        long fileLength = file.length();
        int chunkSize = (int) Math.min(fileLength, TAIL_CHUNK_BYTES);
        byte[] buf = new byte[chunkSize];
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            raf.seek(fileLength - chunkSize);
            raf.readFully(buf);
        }
        String[] all = new String(buf, StandardCharsets.UTF_8).split("\n", -1);
        // When we start mid-file the first entry is a partial line — skip it
        int from = chunkSize < fileLength ? 1 : 0;
        from = Math.max(from, all.length - maxLines);
        return Arrays.asList(Arrays.copyOfRange(all, from, all.length));
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
