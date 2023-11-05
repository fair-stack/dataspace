package cn.cnic.dataspace.api.webdav;

import cn.cnic.dataspace.api.cacheLoading.CacheLoading;
import cn.cnic.dataspace.api.filehandle.Control;
import cn.cnic.dataspace.api.filehandle.ControlImpl;
import cn.cnic.dataspace.api.model.space.SpaceSvnLog;
import io.milton.common.RangeUtils;
import io.milton.common.ReadingException;
import io.milton.common.WritingException;
import io.milton.http.Range;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.http.fs.FileContentService;
import io.milton.http.fs.FileSystemResourceFactory;
import io.milton.http.fs.FsDirectoryResource;
import io.milton.http.fs.FsFileResource;
import io.milton.resource.CollectionResource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import java.io.*;
import java.util.Date;
import java.util.Map;

/**
 * FsFileResourceExt
 *
 * @author wangCc
 * @date 2021-06-30 10:35
 */
@Slf4j
public class FsFileResourceExt extends FsFileResource {

    private Control control;

    private File file;

    private FileContentService fileContentService;

    public FsFileResourceExt(String host, FileSystemResourceFactory factory, File file, FileContentService contentService) {
        super(host, factory, file, contentService);
        this.file = file;
        this.control = new ControlImpl();
        this.fileContentService = contentService;
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
            } catch (RuntimeException r) {
                throw new NotAuthorizedException();
            }
        }
    }

    /**
     * Modify file content
     */
    @Override
    @SneakyThrows
    public void replaceContent(InputStream in, Long length) {
        try {
            if (CacheLoading.fileCreateDate.getIfPresent(file.getPath()) != null) {
                CacheLoading.fileCreateDate.put(file.getPath(), new Date());
            } else {
                control.validateWebDavFileEditPermission(file.getPath());
            }
            super.replaceContent(in, length);
        } catch (Exception e) {
            throw new NotAuthorizedException();
        }
    }

    @Override
    @SneakyThrows
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotFoundException {
        InputStream in = null;
        try {
            control.validateWebDavFileDownPermissions(file.getPath());
            control.stateDown(control.spaceId(file.toPath()), file.length(), "webDav");
            in = fileContentService.getFileContent(file);
            if (range != null) {
                log.debug("sendContent: ranged content: " + file.getAbsolutePath());
                RangeUtils.writeRange(in, range, out);
            } else {
                log.debug("sendContent: send whole file " + file.getAbsolutePath());
                IOUtils.copy(in, out);
            }
            out.flush();
        } catch (FileNotFoundException e) {
            throw new NotFoundException("Couldnt locate content");
        } catch (ReadingException e) {
            throw new IOException(e);
        } catch (WritingException e) {
            throw new IOException(e);
        } catch (Exception e) {
            // If ("Insufficient permissions!". equals (e.getMessage ())){
            // throw new NotAuthorizedException();
            // }
            throw new NotAuthorizedException();
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    @SneakyThrows
    @Override
    public void delete() {
        try {
            control.delete(file.toPath(), SpaceSvnLog.WEBDAV);
        } catch (RuntimeException r) {
            throw new NotAuthorizedException();
        }
    }
}
