package cn.cnic.dataspace.api.webdav;

import io.milton.cache.LocalCacheManager;
import io.milton.http.LockManager;
import io.milton.http.SecurityManager;
import io.milton.http.fs.FileContentService;
import io.milton.http.fs.FileSystemResourceFactory;
import io.milton.http.fs.FsResource;
import io.milton.http.fs.SimpleLockManager;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.io.File;

/**
 * FileSystemResourceFactoryExt
 *
 * @author wangCc
 * @date 2021-06-29 15:27
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class FileSystemResourceFactoryExt extends FileSystemResourceFactory {

    private String ssoPrefix;

    private FileContentService contentService = new CustomizeContentServiceImpl();

    private LockManager lockManager = new SimpleLockManager(new LocalCacheManager());

    public FileSystemResourceFactoryExt(File root, SecurityManager securityManager, String contextPath) {
        super(root, securityManager, contextPath);
        setLockManager(lockManager);
        // super.setLockManager(this.lockManager);
    }

    @Override
    public FsResource resolveFile(String host, File file) {
        FsResource r;
        if (!file.exists()) {
            return null;
        } else if (file.isDirectory()) {
            r = new FsDirectoryResourceExt(host, this, file, contentService);
        } else {
            r = new FsFileResourceExt(host, this, file, contentService);
        }
        if (r != null) {
            this.ssoPrefix = getSsoPrefix();
        }
        return r;
    }
}
