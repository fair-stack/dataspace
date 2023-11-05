package cn.cnic.dataspace.api.ftp.minimalftp.api;

import java.util.Map;

public interface SpaceListener {

    Map<String, String> saveSpaceId(String username, String spaceId);

    String auth(String username, String path);

    boolean renameFile(String username, String sourceFileName, String targetFileName);

    boolean mkdirFile(String username, String fileName);

    void deleteFile(String username, String fileName, String type);

    boolean uploadFile(String username, String fileName);

    void downloadFile(String username, String fileName);
}
