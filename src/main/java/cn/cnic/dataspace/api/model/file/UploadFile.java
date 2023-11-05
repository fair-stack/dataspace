package cn.cnic.dataspace.api.model.file;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.io.Serializable;
import java.util.Date;

@Data
@Document(collection = "upload_file")
public class UploadFile implements Serializable {

    @Id
    private String id;

    private String filePath;

    private String targetPath;

    private String spaceId;

    private String userId;

    private long fileSize;

    private long fileTotal;

    private String fileName;

    private String fileMd5;

    private int checkCount;

    private int checkIndex;

    private int fileStatus;

    private Date createTime;
}
