package cn.cnic.dataspace.api.model.file;

import cn.cnic.dataspace.api.model.space.child.Operator;
import lombok.Data;
import org.springframework.data.annotation.Id;
import java.util.Date;
import java.util.List;

/**
 * @ author chl
 */
@Data
public class FileEntity {

    @Id
    private String id;

    private String hash;

    private String fHash;

    private String name;

    // 0 files 1 folder
    private int type;

    private String suffix;

    private long size;

    private String path;

    private Operator author;

    private Date updateTime;

    private Date createTime;
}
