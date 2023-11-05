package cn.cnic.dataspace.api.model.harvest;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;
import java.util.List;

@Data
@Document(collection = "task_mining")
public class MiningTask {

    @Id
    private String id;

    private String type;

    private String spaceId;

    private String userId;

    private String email;

    private String taskId;

    private String spaceName;

    private String desc;

    // The total number of files
    private long fileCount;

    private long size;

    private String showPath;

    private String sourceRootPath;

    private List<String> sourcePaths;

    private String targetRootPath;

    // schedule
    private double schedule;

    // 0 task waiting for processing 1 transfer in progress 2 completion 3 failure 4 folder (partial failure) 5 file statistics complete, preparing for transfer
    private int state;

    private String error;

    private String ftpHost;

    private int ftpPort;

    private String ftpUserName;

    private String ftpPassword;

    private String param;

    private String linkUrl;

    private Date createTime;
}
