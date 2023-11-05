package cn.cnic.dataspace.api.model.file;

import cn.cnic.dataspace.api.model.space.child.Operator;
import lombok.Data;
import java.util.Date;

@Data
public class FileResult {

    private String hash;

    private String path;

    private String phash;

    private String name;

    // 0 files 1 folder
    private int dirs;

    private Boolean isNull;

    private Boolean notFolder;

    private String mime;

    private String suffix;

    private long size;

    private Operator author;

    private Date updateTime;

    private Date createTime;
}
