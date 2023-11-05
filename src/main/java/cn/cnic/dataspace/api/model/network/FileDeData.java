package cn.cnic.dataspace.api.model.network;

import lombok.Data;
import java.util.List;

@Data
public class FileDeData {

    private int errno;

    private String errmsg;

    private long request_id;

    private String names;

    private List<FileDe> list;

    @Data
    public static class FileDe {

        private long fs_id;

        private int category;

        private String dlink;

        private String path;

        private String filename;

        private long isdir;

        private long size;
    }
}
