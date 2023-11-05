package cn.cnic.dataspace.api.middle;

import cn.cnic.dataspace.api.exception.CommonException;
import lombok.extern.slf4j.Slf4j;
import java.io.*;
import java.nio.file.*;
import static cn.cnic.dataspace.api.util.CommonUtils.*;

/**
 * file action operation handler impl
 *
 * @author wangCc
 * @date 2021-6-11 10:58:37
 */
@Slf4j
public class FileOperationHandlerImpl implements FileOperationHandler {

    public FileOperationHandlerImpl() {
    }

    /**
     * upload file
     */
    @Override
    public void uploadFile(Path path) throws IOException {
        log.info(" 进入了以前的类 : uploadFile -------------------");
        this.uploadFile(path, true);
    }

    @Override
    public void uploadFile(Path path, boolean record) throws IOException {
        log.info(" 进入了以前的类 : uploadFile (record)-------------------");
        try {
            Files.createFile(path);
        } catch (FileAlreadyExistsException e) {
            Files.delete(path);
            uploadFile(path);
        } catch (IOException e) {
            e.printStackTrace();
            throw new CommonException(500, messageInternational("INTERNAL_ERROR"));
        }
        if (record) {
            // logIntegrating(path, messageInternational("LOG_UPLOAD") + " " + path.getFileName(), FILE_UPLOAD);
        }
    }
}
