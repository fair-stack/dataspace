package cn.cnic.dataspace.api.model.release;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;
import java.util.List;

@Data
@Document(collection = "resource_v2")
public class ResourceV2 {

    @Id
    private String id;

    private String resourceId;

    // version
    private String version;

    // insertId
    private String traceId;

    private Boolean latest;

    private String plan;

    // address
    // Institution domain name filled in
    private String orgUrl;

    // Activation code corresponding to the institution
    private String orgCode;

    // Data revocation address
    private String undoUrl;

    // InstDB Details Address
    private String detailsUrl;

    // Data push success notification interface
    private String updateFtpStatusUrl;

    // Release version
    private String orgId;

    // Publishing agency
    private String orgName;

    private String templateId;

    // Template
    private String template;

    // Basic information
    // title
    private String titleCH;

    private String titleEN;

    private String image;

    // keyword
    private List<String> keywordCH;

    private List<String> keywordEN;

    // Resource Type Dataset/Software
    private String resourceType;

    private String resourceTypeShow;

    private List<RequestFile> viewFileList;

    // metadata
    private List<ResourceDo> metadata;

    // Status 0 Draft 1 Pending Review 2 Published 3 Rejected
    private int type;

    // Belonging space
    private String spaceId;

    /**
     * userId
     */
    private String founderId;

    private String founder;

    private String founderName;

    private String founderOrg;

    private String realPath;

    // data size
    private long dataSize;

    // Modification comments
    private String dismissReason;

    // Reason for rejection
    private String rejectApproval;

    // Rejection time
    private Date dismissTime;

    // Reject Document
    private List<SendFile> rejectFile;

    // Modify Type
    private String updateType;

    // Release destination
    private String publishTarget;

    // Is the data submitted successfully true false
    private boolean successOf;

    private String errorMessage;

    // Real time sending status of files
    private Boolean fileSend;

    // Whether the file was successfully sent (page judgment) 0. Transmission in progress 1. Success 2. Failure
    private int fileSuccessOf;

    private String ftpUsername;

    private String ftpPassword;

    private String ftpHost;

    // Is the push completed (scidb submission for review) (insertDB notification file upload successful)
    private boolean auditSend;

    // error message
    private String auditError;

    private Date createTime;

    private Date updateTime;

    private Date publicTime;

    /**
     * Data type 0 Unstructured (with file) 1 Structured (representing grid) 2 (file+table)
     */
    private int dataType;

    private List<String> tableList;
}
