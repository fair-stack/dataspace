package cn.cnic.dataspace.api.model.release;

import lombok.Data;
import org.springframework.data.annotation.Id;
import java.util.Date;
import java.util.List;

/**
 * Frontend resource display
 */
@Data
public class ResourceShow {

    @Id
    private String id;

    private String resourceId;

    // Publisher
    private String founder;

    private String founderName;

    // Status 0 Draft 1 Pending Review 2 Published 3 Rejected
    private int type;

    // Release version
    // Publishing agency
    private String orgName;

    // version
    private String version;

    // Basic information
    // title
    private String titleCH;

    private String image;

    private String resourceTypeShow;

    // Belonging space
    private String spaceId;

    // Name of the space it belongs to
    private String spaceName;

    // Template
    private String template;

    // Reason for rejection
    private String dismissReason;

    private String rejectApproval;

    // Reject Document
    private List<SendFile> rejectFile;

    private String detailsUrl;

    // Is the file sent successfully? 0 Transferring 1 succeeded. 2 failed
    private int fileSuccessOf;

    // Rejection time
    private Date dismissTime;

    private Date createTime;

    private Date updateTime;

    private Date publicTime;

    /**
     * Data type 0 Unstructured data 1 Structured data
     */
    private int dataType;
}
