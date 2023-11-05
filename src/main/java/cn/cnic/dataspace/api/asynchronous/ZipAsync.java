package cn.cnic.dataspace.api.asynchronous;

import cn.cnic.dataspace.api.config.space.FileMappingManage;
import cn.cnic.dataspace.api.config.space.MsgUtil;
import cn.cnic.dataspace.api.config.space.SpaceControlConfig;
import cn.cnic.dataspace.api.model.space.child.Operator;
import cn.cnic.dataspace.api.model.space.child.Person;
import cn.cnic.dataspace.api.service.space.MessageService;
import cn.cnic.dataspace.api.util.FileSizeComputer;
import cn.cnic.dataspace.api.util.Token;
import cn.cnic.dataspace.api.util.ZipStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import static cn.cnic.dataspace.api.model.space.SpaceSvnLog.*;

@Slf4j
@Service
public class ZipAsync {

    @Autowired
    private MessageService messageService;

    @Autowired
    private MsgUtil msgUtil;

    @Autowired
    private FileMappingManage fileMappingManage;

    // @Autowired
    // private MongoTemplate mongoTemplate;
    @Async
    public void compress(String filePath, String tempPath, String targetPath, String spaceId, Token token, SpaceControlConfig spaceControlConfig) {
        log.info("---- 压缩文件 ----");
        ZipStream zipStream = new ZipStream();
        // Packaging folder directory
        File file = new File(filePath);
        // Temporary directory
        File tempFile = new File(tempPath);
        if (!tempFile.exists()) {
            tempFile.mkdirs();
        }
        File tempFileZip = new File(tempPath, file.getName() + ".zip");
        if (tempFileZip.exists()) {
            tempFileZip.delete();
        }
        File targetFile = new File(targetPath, file.getName() + ".zip");
        if (targetFile.exists()) {
            log.info("压缩包（" + tempFileZip.getName() + "）已存在。");
            sendMessage(token, "文件压缩通知!", "文件夹 " + file.getName() + " 压缩失败：压缩包（" + tempFileZip.getName() + "）已存在。");
            return;
        }
        try {
            zipStream.fileToZip(file, tempFileZip);
        } catch (Exception e) {
            log.info("{} 压缩失败 " + e.getMessage());
            sendMessage(token, "文件压缩通知!", "文件夹 " + file.getName() + " 压缩失败：" + e.getMessage());
            try {
                Files.delete(tempFileZip.toPath());
                Files.delete(tempFile.toPath());
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
            return;
        }
        log.info("{} 压缩结束 文件移到对应的空间目录下");
        try {
            Files.move(tempFileZip.toPath(), targetFile.toPath());
            Files.delete(tempFile.toPath());
        } catch (IOException ioException) {
            ioException.printStackTrace();
            return;
        }
        long length = targetFile.length();
        // Add File Mapping
        Operator operator = fileMappingManage.getOperator(token.getEmailAccounts());
        fileMappingManage.transit(FILE_CREATE, spaceId, targetFile.toPath(), false, false, ELFINDER, length, operator);
        // Statistics - Log+Data Size
        spaceControlConfig.dataFlow("zip", length, spaceId, true);
        spaceControlConfig.spaceLogSave(spaceId, "压缩了文件（夹）" + file.getName() + " 生成的压缩文件名称为 " + file.getName() + ".zip", token.getUserId(), new Operator(token), ACTION_FILE);
        sendMessage(token, "文件压缩通知!", "文件夹 " + file.getName() + " 压缩成功，请去空间查看！");
        log.info("---- 处理结束 压缩任务成功----");
        return;
    }

    @Async
    public void decompression(String filePath, String targetPath, String spaceId, Token token, SpaceControlConfig spaceControlConfig) {
        log.info("---- 解压文件 ----");
        // Packaging folder directory
        File file = new File(filePath);
        if (!file.exists()) {
            log.info(filePath + " 解压的文件已被删除");
            sendMessage(token, "文件解压通知!", "文件 " + file.getName() + " 解压失败：文件已被移除或者文件名或路径发生改变。");
            return;
        }
        try {
            ZipStream.unzip(filePath, targetPath);
        } catch (Exception e) {
            log.info("{} 解压失败 " + e.getMessage());
            sendMessage(token, "文件解压通知!", "文件 " + file.getName() + " 解压失败：" + e.getMessage());
            return;
        }
        File targetFile = new File(targetPath);
        // Calculate file size
        Long invoke = FileSizeComputer.FORK_JOIN_POOL.invoke(new FileSizeComputer(targetFile));
        if (targetFile.listFiles().length > 0) {
            Operator operator = fileMappingManage.getOperator(token.getEmailAccounts());
            fileMappingManage.saveSpaceFileMapping(spaceId, targetFile, operator);
            // List<FileMapping> spaceFileMappingList = fileMappingManage.getSpaceFileMappingList(spaceId, targetFile.listFiles(), operator);
            // mongoTemplate.insert(spaceFileMappingList, spaceId);
        }
        // Statistics - Log+Data Size
        spaceControlConfig.dataFlow("zip", invoke, spaceId, true);
        spaceControlConfig.spaceLogSave(spaceId, "解压了文件" + file.getName() + "", token.getUserId(), new Operator(token), ACTION_FILE);
        sendMessage(token, "文件解压通知!", "文件 " + file.getName() + " 解压成功，请去空间查看！");
        log.info("---- 处理结束 解压任务成功----");
        return;
    }

    /**
     * send message
     */
    private void sendMessage(Token userId, String title, String content) {
        // send message
        Map<String, Object> msgMap = new HashMap<>(2);
        msgMap.put("title", title);
        msgMap.put("content", content);
        String emailAccounts = userId.getEmailAccounts();
        msgUtil.sendMsg(emailAccounts, msgUtil.mapToString(msgMap));
        messageService.sendToApplicant(title, content, new Person(userId), 1);
    }
    // /**
    // * decompress log record
    // */
    // private void deCompressLog(String fileName,Token usersToken,String spaceId) {
    // SpaceStatisticConfig. spaceLogSave (spaceId, "extracted file"+fileName, usersToken. getUserId()),
    // new Operator(usersToken),ACTION_FILE);
    // }
}
