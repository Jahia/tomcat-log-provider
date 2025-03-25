package org.jahia.modules.external.tomcat.log.factory;

import java.io.Serializable;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.validation.constraints.NotEmpty;
import org.jahia.modules.external.admin.mount.AbstractMountPointFactory;
import org.jahia.modules.external.admin.mount.validator.LocalJCRFolder;
import org.jahia.services.content.JCRNodeWrapper;

public final class TomcatLogMountPointFactory extends AbstractMountPointFactory implements Serializable {

    private static final long serialVersionUID = -7193843633565652369L;

    @NotEmpty
    private String name;
    @LocalJCRFolder
    private String localPath;
    
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void populate(JCRNodeWrapper nodeWrapper) throws RepositoryException {
        super.populate(nodeWrapper);
        this.name = getName(nodeWrapper.getName());
        try {
            this.localPath = nodeWrapper.getProperty("mountPoint").getNode().getPath();
        } catch (PathNotFoundException ex) {
            // no local path defined for this mount point
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getLocalPath() {
        return localPath;
    }

    @Override
    public String getMountNodeType() {
        return "jnt:tomcatLogMountPoint";
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

    @Override
    public void setProperties(JCRNodeWrapper mountPoint) {
    }
}
