package cn.cnic.dataspace.api.webdav;

import cn.cnic.dataspace.api.filehandle.Control;
import cn.cnic.dataspace.api.filehandle.ControlImpl;
import io.milton.http.fs.FileContentService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import java.io.*;

/**
 * CustomizeContentService
 *
 * @author wangCc
 * @date 2021-07-02 18:18
 */
@Slf4j
public class CustomizeContentServiceImpl implements FileContentService {

    private final Control control;

    public CustomizeContentServiceImpl() {
        this.control = new ControlImpl();
    }

    @Override
    public void setFileContent(File dest, InputStream in) throws IOException {
        control.createAndCopyFileNoAuth(dest, in);
    }

    @SneakyThrows
    @Override
    public /**
     * FsFileResourceExt.sendContent Control Permissions
     */
    InputStream getFileContent(File file) throws FileNotFoundException {
        return new FileInputStream(file);
    }
}
