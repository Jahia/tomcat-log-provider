package org.jahia.modules.external.tomcat.log;

import javax.jcr.RepositoryException;
import org.jahia.exceptions.JahiaInitializationException;
import org.jahia.modules.external.ExternalContentStoreProvider;
import org.jahia.services.SpringContextSingleton;
import org.jahia.services.content.*;

public final class TomcatLogProviderFactory implements ProviderFactory {

    @Override
    public String getNodeTypeName() {
        return "jnt:tomcatLogMountPoint";
    }

    @Override
    public JCRStoreProvider mountProvider(JCRNodeWrapper mountPoint) throws RepositoryException {
        final ExternalContentStoreProvider provider = (ExternalContentStoreProvider) SpringContextSingleton.getBean("ExternalStoreProviderPrototype");
        provider.setKey(mountPoint.getIdentifier());
        provider.setMountPoint(mountPoint.getPath());

        final TomcatLogDataSource dataSource = new TomcatLogDataSource();
        dataSource.setRoot();
        provider.setDataSource(dataSource);
        provider.setDynamicallyMounted(true);
        provider.setSessionFactory(JCRSessionFactory.getInstance());
        try {
            provider.start();
        } catch (JahiaInitializationException ex) {
            throw new RepositoryException(ex);
        }
        return provider;

    }
}
