package cn.cnic.dataspace.api.model.backup;

import cn.cnic.dataspace.api.model.space.child.AuthorizationPerson;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;

@Data
@Document(collection = "backup_space_main")
public class BackupSpaceMain {

    @Id
    private String id;

    private String jobId;

    private String jobStatus;

    private AuthorizationPerson person;

    private String spaceId;

    private String spaceName;

    private String spacePath;

    private long backup_total;

    private int success;

    private int error;

    // strategy
    private String strategy;

    // Execution cycle
    private String executionCycle;

    private String executionTime;

    private String executionValue;

    private String corn;

    // Recently executed
    private Date recentlyTime;

    // Next execution
    private Date nextTime;

    // Start and stop
    private String status;

    private Date lastUpdateTime;

    private Date createTime;
}
