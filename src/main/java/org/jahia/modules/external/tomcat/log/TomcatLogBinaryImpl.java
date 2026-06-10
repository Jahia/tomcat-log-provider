package org.jahia.modules.external.tomcat.log;

import java.io.IOException;
import java.io.InputStream;
import javax.jcr.Binary;
import javax.jcr.RepositoryException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.vfs2.FileContent;
import org.apache.commons.vfs2.FileSystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TomcatLogBinaryImpl implements Binary {
    
    private static final Logger logger = LoggerFactory.getLogger(TomcatLogBinaryImpl.class);

    private final FileContent fileContent;

    public TomcatLogBinaryImpl(FileContent fileContent) {
        super();
        this.fileContent = fileContent;
    }

    @Override
    public void dispose() {
        try {
            fileContent.close();
        } catch (FileSystemException ex) {
            logger.warn("Impossible to close file content", ex);
        }
    }

    @Override
    public long getSize() throws RepositoryException {
        try {
            return fileContent.getSize();
        } catch (FileSystemException e) {
            throw new RepositoryException(e);
        }
    }

    @Override
    public InputStream getStream() throws RepositoryException {
        try {
            return fileContent.getInputStream();
        } catch (FileSystemException e) {
            throw new RepositoryException(e);
        }
    }

    @Override
    public int read(byte[] b, long position) throws IOException, RepositoryException {
        if (b == null) {
            throw new NullPointerException("destination buffer must not be null");
        }
        if (position < 0) {
            throw new IOException("position must not be negative: " + position);
        }
        InputStream is = null;
        int read = 0;
        try {
            is = getStream();
            // Skip to the requested position before reading into the buffer.
            // IOUtils.skip is used instead of InputStream.skip because some
            // implementations do not guarantee skipping the full amount.
            long remaining = position;
            while (remaining > 0) {
                long skipped = IOUtils.skip(is, remaining);
                if (skipped == 0) {
                    // EOF reached before position; nothing to read
                    return -1;
                }
                remaining -= skipped;
            }
            read = is.read(b, 0, b.length);
        } finally {
            IOUtils.closeQuietly(is);
        }
        return read;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj != null && this.getClass() == obj.getClass()) {
            TomcatLogBinaryImpl other = (TomcatLogBinaryImpl)obj;
            return this.fileContent == other.fileContent;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return fileContent.hashCode();
    }
}
