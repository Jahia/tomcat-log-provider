package org.jahia.modules.external.tomcat.log.factory;

import java.io.Serializable;
import java.util.Locale;
import javax.jcr.RepositoryException;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.VFS;
import org.jahia.modules.external.admin.mount.AbstractMountPointFactoryHandler;
import org.jahia.modules.external.tomcat.log.TomcatLogDataSource;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRTemplate;
import org.jahia.utils.i18n.Messages;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.binding.message.MessageBuilder;
import org.springframework.binding.message.MessageContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.webflow.execution.RequestContext;

public final class TomcatLogMountPointFactoryHandler extends AbstractMountPointFactoryHandler<TomcatLogMountPointFactory> implements Serializable {

    private static final long serialVersionUID = 7189210242067838479L;
    private static final Logger LOGGER = LoggerFactory.getLogger(TomcatLogMountPointFactoryHandler.class);
    private static final String BUNDLE = "resources.tomcat-log-provider";
    private TomcatLogMountPointFactory tomcatLogMountPointFactory;
    private String stateCode;
    private String messageKey;

    public void init(RequestContext requestContext) {
        tomcatLogMountPointFactory = new TomcatLogMountPointFactory();
        try {
            super.init(requestContext, tomcatLogMountPointFactory);
        } catch (RepositoryException ex) {
            LOGGER.error("Error retrieving mount point", ex);
        }
        requestContext.getFlowScope().put("tomcatLogFactory", tomcatLogMountPointFactory);
    }

    public String getFolderList() {
        final JSONObject result = new JSONObject();
        try {
            final JSONArray folders = JCRTemplate.getInstance().doExecuteWithSystemSession((JCRSessionWrapper session) -> getSiteFolders(session.getWorkspace()));

            result.put("folders", folders);
        } catch (RepositoryException ex) {
            LOGGER.error("Error trying to retrieve local folders", ex);
        } catch (JSONException ex) {
            LOGGER.error("Error trying to construct JSON from local folders", ex);
        }

        return result.toString();
    }

    public Boolean save(MessageContext messageContext, RequestContext requestContext) {
        stateCode = "SUCCESS";
        final Locale locale = LocaleContextHolder.getLocale();
        final String tomcatLogPath = TomcatLogDataSource.getTomcatLogPath();
        final boolean validVFSPoint = validateVFS(tomcatLogPath);
        if (!validVFSPoint) {
            LOGGER.error("Error saving mount point : " + tomcatLogMountPointFactory.getName() + " with the root : " + tomcatLogPath);
            final MessageBuilder messageBuilder = new MessageBuilder().error().defaultText(Messages.get(BUNDLE, "serverSettings.tomcatLogMountPointFactory.save.error", locale));
            messageContext.addMessage(messageBuilder.build());
            requestContext.getConversationScope().put("adminURL", getAdminURL(requestContext));
            return Boolean.FALSE;
        }
        try {
            final boolean available = super.save(tomcatLogMountPointFactory);
            if (available) {
                stateCode = "SUCCESS";
                messageKey = "serverSettings.tomcatLogMountPointFactory.save.success";
                requestContext.getConversationScope().put("adminURL", getAdminURL(requestContext));
                return Boolean.TRUE;
            } else {
                LOGGER.warn("Mount point availability problem : " + tomcatLogMountPointFactory.getName() + " with the root : " + tomcatLogPath + " the mount point is created but unmounted");
                stateCode = "WARNING";
                messageKey = "serverSettings.tomcatLogMountPointFactory.save.unavailable";
                requestContext.getConversationScope().put("adminURL", getAdminURL(requestContext));
                return Boolean.TRUE;
            }
        } catch (RepositoryException e) {
            LOGGER.error("Error saving mount point : " + tomcatLogMountPointFactory.getName(), e);
            final MessageBuilder messageBuilder = new MessageBuilder().error().defaultText(Messages.get(BUNDLE, "serverSettings.tomcatLogMountPointFactory.save.error", locale));
            messageContext.addMessage(messageBuilder.build());
        }
        return Boolean.FALSE;
    }

    private boolean validateVFS(String tomcatLogPath) {
        try {
            VFS.getManager().resolveFile(tomcatLogPath);
        } catch (FileSystemException ex) {
            LOGGER.warn("VFS mount point " + tomcatLogPath + " has validation problem " + ex.getMessage());
            return false;
        }
        return true;
    }

    @Override
    public String getAdminURL(RequestContext requestContext) {
        final StringBuilder builder = new StringBuilder(super.getAdminURL(requestContext));
        if (stateCode != null && messageKey != null) {
            builder.append("?stateCode=").append(stateCode).append("&messageKey=").append(messageKey).append("&bundleSource=").append(BUNDLE);
        }
        return builder.toString();
    }
}
