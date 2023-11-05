package cn.cnic.dataspace.api.model.backup;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;

@Data
@Document(collection = "backup_space_subtasks")
public class BackupSpaceSubtasks {

    @Id
    private String id;

    private String jobId;

    private String desc;

    // 0 waiting for 1 transmission, 2 completion, 3 failure
    private int state;

    // Backup volume
    private long totalSize;

    private long totalFileNum;

    // Duration
    private String duration;

    // Number of files
    private long fileNum;

    // schedule
    private long schedule;

    private double showSchedule;

    private String error;

    private Date startTime;

    private Date endTime;

    private Date createTime;
}
