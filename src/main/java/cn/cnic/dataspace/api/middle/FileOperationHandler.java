package cn.cnic.dataspace.api.middle;

import java.io.IOException;
import java.nio.file.Path;

/**
 * file action operation handler
 * standard operation：
 * 1. create file/directory
 * 2. delete file/directory
 * 3. rename file/directory
 * 4. move file/directory
 * 5. modify file
 *
 * @author wangCc
 * @date 2021-6-11 10:58:37
 */
public interface FileOperationHandler {

    void uploadFile(Path path) throws IOException;

    void uploadFile(Path path, boolean record) throws IOException;
}
