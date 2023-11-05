package cn.cnic.dataspace.api.model.network;

import lombok.Data;
import java.util.List;

@Data
public class BaiduData {

    private int errno;

    private String guid_info;

    private long request_id;

    private int guid;

    private List<File> list;

    @Data
    public static class File {

        private long fs_id;

        private String path;

        private String server_filename;

        private long size;

        private long isdir;

        private long local_ctime;

        private long server_ctime;
    }
}
