package cn.cnic.dataspace.api.model.harvest;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;

@Data
@Document(collection = "task_file_imp")
public class TaskFileImp {

    @Id
    private String id;

    private String taskId;

    private String rootId;

    private String fileName;

    private String link;

    private long size;

    private String showPath;

    private String path;

    private long sort;

    // schedule
    private long schedule;

    private double showSchedule;

    // 0 waiting for 1 transmission, 2 completion, 3 failure
    private int state;

    private String error;

    // 0 files 1 folder
    private int type;

    private Date createTime;
}
