package cn.cnic.dataspace.api.webdav;

import cn.cnic.dataspace.api.cacheLoading.CacheLoading;
import cn.cnic.dataspace.api.filehandle.Control;
import cn.cnic.dataspace.api.filehandle.ControlImpl;
import cn.cnic.dataspace.api.model.space.SpaceSvnLog;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.fs.FileContentService;
import io.milton.http.fs.FileSystemResourceFactory;
import io.milton.http.fs.FsDirectoryResource;
import io.milton.resource.CollectionResource;
import io.milton.resource.Resource;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

/**
 * FsDirectoryResourceExt
 *
 * @author wangCc
 * @date 2021-06-29 15:31
 */
public class FsDirectoryResourceExt extends FsDirectoryResource {

    private Control control;

    private File file;

    private final FileContentService contentService;

    private final FileSystemResourceFactory factory;

    private final String host;

    public FsDirectoryResourceExt(String host, FileSystemResourceFactory factory, File dir, FileContentService contentService) {
        super(host, factory, dir, contentService);
        this.file = dir;
        this.contentService = contentService;
        this.factory = factory;
        this.host = host;
        this.control = new ControlImpl();
    }

    @Override
    @SneakyThrows
    public void moveTo(CollectionResource newParent, String newName) {
        if (newParent instanceof FsDirectoryResource) {
            File dest = new File(((FsDirectoryResource) newParent).getFile(), newName);
            try {
                if (StringUtils.equals(file.getParent(), dest.getParent())) {
                    control.rename(file.toPath(), dest.toPath(), SpaceSvnLog.WEBDAV);
                } else {
                    control.move(file.toPath(), dest.toPath(), SpaceSvnLog.WEBDAV);
                }
            } catch (RuntimeException e) {
                throw new NotAuthorizedException();
            }
        }
    }

    /**
     * create file
     *
     * @param name
     * @param in
     * @param length
     * @param contentType
     * @return
     * @throws IOException
     */
    @Override
    @SneakyThrows
    public Resource createNew(String name, InputStream in, Long length, String contentType) throws IOException {
        try {
            File dest = new File(this.getFile(), name);
            control.validateWebDavFileCreatePermission(dest.getPath());
            CacheLoading.fileCreateDate.put(dest.getPath(), new Date());
            return super.createNew(name, in, length, contentType);
        } catch (Exception e) {
            throw new NotAuthorizedException();
        }
    }

    /**
     * create dir
     *
     * @param name
     * @return
     */
    @SneakyThrows
    @Override
    public CollectionResource createCollection(String name) {
        File fNew = new File(file, name);
        try {
            control.createFolder(fNew.toPath(), false, true, SpaceSvnLog.WEBDAV);
        } catch (RuntimeException e) {
            throw new NotAuthorizedException();
        }
        return new FsDirectoryResourceExt(host, factory, fNew, contentService);
    }

    @Override
    @SneakyThrows
    public void delete() {
        try {
            control.delete(file.toPath(), SpaceSvnLog.WEBDAV);
        } catch (RuntimeException e) {
            throw new NotAuthorizedException();
        }
    }
}
