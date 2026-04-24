package org.jahia.modules.external.tomcat.log;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.vfs2.*;
import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.util.ISO8601;
import org.jahia.api.Constants;
import org.jahia.modules.external.ExternalData;
import org.jahia.modules.external.ExternalDataSource;
import org.jahia.registries.ServicesRegistry;
import org.jahia.services.content.JCRContentUtils;
import org.jahia.services.content.decorator.JCRUserNode;
import org.jahia.services.sites.JahiaSitesService;
import org.jahia.services.usermanager.JahiaUserManagerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Binary;
import javax.jcr.ItemNotFoundException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import java.io.File;
import java.util.*;

public class TomcatLogDataSource implements ExternalDataSource, ExternalDataSource.Writable, ExternalDataSource.CanLoadChildrenInBatch, ExternalDataSource.SupportPrivileges {

    private static final List<String> JCR_CONTENT_LIST = List.of(Constants.JCR_CONTENT);
    private static final Set<String> SUPPORTED_NODE_TYPES = new HashSet<>(Arrays.asList(Constants.JAHIANT_FILE, Constants.JAHIANT_FOLDER, Constants.JCR_CONTENT));
    private static final Logger LOGGER = LoggerFactory.getLogger(TomcatLogDataSource.class);
    private static final String JCR_CONTENT_SUFFIX = FileSystem.SEPARATOR + Constants.JCR_CONTENT;
    private static final String UNKNOWN_FILE_TYPE = "Found non file or folder entry at path {}, maybe an alias. VFS file type: {}";
    private FileObject root;
    private String rootPath;
    private FileSystemManager manager;

    public static String getTomcatLogPath() {
        return System.getProperty("catalina.base") + File.separator + "logs";
    }

    public void setRoot() {
        final String tomcatLogPath = getTomcatLogPath();
        try {
            manager = VFS.getManager();
            root = manager.resolveFile(tomcatLogPath);
            rootPath = root.getName().getPath();
        } catch (FileSystemException ex) {
            throw new IllegalStateException("Cannot set root to " + tomcatLogPath, ex);
        }
    }

    protected FileObject getRoot() {
        return root;
    }

    protected String getRootPath() {
        return rootPath;
    }

    protected FileSystemManager getManager() {
        return manager;
    }

    @Override
    public boolean isSupportsUuid() {
        return false;
    }

    @Override
    public boolean isSupportsHierarchicalIdentifiers() {
        return true;
    }

    @Override
    public boolean itemExists(String path) {
        try {
            final FileObject file = getFile(path.endsWith(JCR_CONTENT_SUFFIX) ? StringUtils.substringBeforeLast(
                    path, JCR_CONTENT_SUFFIX) : path);
            return file.exists();
        } catch (FileSystemException e) {
            LOGGER.warn("Unable to check file existence for path " + path, e);
        }
        return false;
    }

    @Override
    public void order(String path, List<String> children) throws RepositoryException {
        // ordering is not supported in VFS
    }

    @Override
    public Set<String> getSupportedNodeTypes() {
        return Set.copyOf(SUPPORTED_NODE_TYPES);
    }

    @Override
    public ExternalData getItemByIdentifier(String identifier) throws ItemNotFoundException {
        if (identifier.startsWith(FileSystem.SEPARATOR)) {
            try {
                return getItemByPath(identifier);
            } catch (PathNotFoundException e) {
                throw new ItemNotFoundException(identifier, e);
            }
        }
        throw new ItemNotFoundException(identifier);
    }

    @Override
    public ExternalData getItemByPath(String path) throws PathNotFoundException {
        try {
            String unescapedPath = JCRContentUtils.unescapeLocalNodeName(path);
            if (path.endsWith(JCR_CONTENT_SUFFIX)) {
                FileObject fileObject = getFile(StringUtils.substringBeforeLast(unescapedPath, JCR_CONTENT_SUFFIX));
                if (!fileObject.exists()) {
                    throw new PathNotFoundException(path);
                }
                if(fileObject.getType() == FileType.FOLDER) {
                    return getFile(fileObject);
                }else {
                    FileContent content = fileObject.getContent();
                    return getFileContent(content);
                }
            } else {
                FileObject fileObject = getFile(unescapedPath);
                if (!fileObject.exists()) {
                    throw new PathNotFoundException(path);
                }
                return getFile(fileObject);
            }

        } catch (FileSystemException ex) {
            throw new PathNotFoundException("File system exception while trying to retrieve " + path, ex);
        }
    }

    public FileObject getFile(String path) throws FileSystemException {
        if (path == null || path.isEmpty() || FileSystem.SEPARATOR.equals(path)) {
            return root;
        }
        return root.resolveFile(path.charAt(0) == FileSystem.SEPARATOR_CHAR ? path.substring(1) : path);
    }

    @Override
    public List<String> getChildren(String path) throws RepositoryException {
        try {
            if (!path.endsWith(JCR_CONTENT_SUFFIX)) {
                final FileObject fileObject = getFile(path);
                if (null == fileObject.getType()) {
                    if (fileObject.exists()) {
                        LOGGER.warn(UNKNOWN_FILE_TYPE,
                                fileObject, fileObject.getType());
                    } else {
                        throw new PathNotFoundException(path);
                    }
                } else {
                    switch (fileObject.getType()) {
                        case FILE:
                            return new ArrayList<>(JCR_CONTENT_LIST);
                        case FOLDER:
                            final FileObject[] files = fileObject.getChildren();
                            if (files.length > 0) {
                                final List<String> children = new LinkedList<>();
                                for (FileObject object : files) {
                                    if (getSupportedNodeTypes().contains(getDataType(object))) {
                                        children.add(JCRContentUtils.escapeLocalNodeName(object.getName().getBaseName()));
                                    }
                                }
                                return children;
                            } else {
                                return Collections.emptyList();
                            }
                        default:
                            if (fileObject.exists()) {
                                LOGGER.warn(UNKNOWN_FILE_TYPE,
                                        fileObject, fileObject.getType());
                            } else {
                                throw new PathNotFoundException(path);
                            }
                            break;
                    }
                }
            }
        } catch (FileSystemException e) {
            LOGGER.error("Cannot get node children", e);
        }

        return Collections.emptyList();
    }

    @Override
    public List<ExternalData> getChildrenNodes(String path) throws RepositoryException {
        try {
            if (!path.endsWith(JCR_CONTENT_SUFFIX) && !path.contains("j:translation")) {
                final FileObject fileObject = getFile(path);
                if (null == fileObject.getType()) {
                    if (fileObject.exists()) {
                        LOGGER.warn(UNKNOWN_FILE_TYPE,
                                fileObject, fileObject.getType());
                    } else {
                        throw new PathNotFoundException(path);
                    }
                } else {
                    switch (fileObject.getType()) {
                        case FILE:
                            final FileContent content = fileObject.getContent();
                            return Collections.singletonList(getFileContent(content));
                        case FOLDER:
                            //in case of folder, refresh because it could be changed external
                            fileObject.refresh();
                            final FileObject[] files = fileObject.getChildren();
                            if (files.length > 0) {
                                final List<ExternalData> children = new LinkedList<>();
                                for (FileObject object : files) {
                                    if (getSupportedNodeTypes().contains(getDataType(object))) {
                                        children.add(getFile(object));
                                        if (object.getType() == FileType.FILE) {
                                            children.add(getFileContent(object.getContent()));
                                        }
                                    }
                                }
                                return children;
                            } else {
                                return Collections.emptyList();
                            }
                        default:
                            if (fileObject.exists()) {
                                LOGGER.warn(UNKNOWN_FILE_TYPE,
                                        fileObject, fileObject.getType());
                            } else {
                                throw new PathNotFoundException(path);
                            }
                            break;
                    }
                }
            }
        } catch (FileSystemException e) {
            LOGGER.error("Cannot get node children", e);
        }

        return Collections.emptyList();
    }

    @Override
    public void removeItemByPath(String path) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void saveItem(ExternalData data) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void move(String oldPath, String newPath) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String[] getPrivilegesNames(String username, String path) {
        final JahiaUserManagerService userManagerService = JahiaUserManagerService.getInstance();
        final JCRUserNode userNode = userManagerService.lookupUser(username);
        final String[] privileges;
        if (ServicesRegistry.getInstance().getJahiaGroupManagerService().isAdminMember(userNode.getName(), JahiaSitesService.SYSTEM_SITE_KEY)) {
            privileges = new String[1];
            privileges[0] = Constants.JCR_READ_RIGHTS + "_" + Constants.EDIT_WORKSPACE;
        } else {
            privileges = new String[0];
        }
        return privileges;
    }

    private ExternalData getFile(FileObject fileObject) throws FileSystemException {
        final String type = getDataType(fileObject);

        final Map<String, String[]> properties = new HashMap<>();
        final Map<String, Map<String, String[]>> i18nProperties = new HashMap<>();
        final List<String> addedMixins = new ArrayList<>();
        final FileContent content = fileObject.getContent();
        if (content != null) {
            final long lastModifiedTime = fileObject.getContent().getLastModifiedTime();
            if (lastModifiedTime > 0) {
                final Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(lastModifiedTime);
                final String[] timestamp = new String[]{ISO8601.format(calendar)};
                final String[] name = new String[]{fileObject.getName().getBaseName()};
                properties.put(Constants.JCR_CREATED, timestamp);
                properties.put(Constants.NODENAME, name);
                properties.put(Constants.JCR_LASTMODIFIED, timestamp);
                final Map<String, String[]> i18nProperty = new HashMap<>();
                i18nProperty.put(Constants.JCR_TITLE,name);
                i18nProperties.put("en", i18nProperty);
            }
            // Add jmix:image mixin in case of the file is a picture.
            if (content.getContentInfo() != null && content.getContentInfo().getContentType() != null
                    && fileObject.getContent().getContentInfo().getContentType().matches("image/(.*)")) {
                addedMixins.add(Constants.JAHIAMIX_IMAGE);
            }

        }

        String path = JCRContentUtils.escapeNodePath(fileObject.getName().getPath().substring(rootPath.length()));
        if (!path.startsWith(FileSystem.SEPARATOR)) {
            path = FileSystem.SEPARATOR + path;
        }

        final ExternalData result = new ExternalData(path, path, type, properties);
        result.setI18nProperties(i18nProperties);
        result.setMixin(addedMixins);
        return result;
    }

    public String getDataType(FileObject fileObject) throws FileSystemException {
        return fileObject.getType() == FileType.FILE ? Constants.JAHIANT_FILE
                : Constants.JAHIANT_FOLDER;
    }

    protected ExternalData getFileContent(final FileContent content) throws FileSystemException {
        final Map<String, String[]> properties = new HashMap<>(1);

        properties.put(Constants.JCR_MIMETYPE, new String[]{getContentType(content)});

        final String path = JCRContentUtils.escapeNodePath(content.getFile().getName().getPath().substring(rootPath.length()));
        final String jcrContentPath = path + FileSystem.SEPARATOR + Constants.JCR_CONTENT;
        final ExternalData externalData = new ExternalData(jcrContentPath, jcrContentPath, Constants.JAHIANT_RESOURCE, properties);
        final Map<String, Binary[]> binaryProperties = new HashMap<>(1);
        binaryProperties.put(Constants.JCR_DATA, new Binary[]{new TomcatLogBinaryImpl(content)});
        externalData.setBinaryProperties(binaryProperties);

        return externalData;
    }

    protected String getContentType(FileContent content) throws FileSystemException {
        String s1 = content.getContentInfo().getContentType();
        if (s1 == null) {
            s1 = JCRContentUtils.getMimeType(content.getFile().getName().getBaseName());
        }
        if (s1 == null) {
            s1 = "application/octet-stream";
        }
        return s1;
    }
}
