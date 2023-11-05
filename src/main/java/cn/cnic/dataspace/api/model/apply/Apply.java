package cn.cnic.dataspace.api.model.apply;

import cn.cnic.dataspace.api.model.space.child.Person;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.io.Serializable;
import java.util.List;

/**
 * apply
 *
 * @author wangCc
 * @date 2021-04-06 16:49
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "apply")
public class Apply implements Serializable {

    // Approval status
    public static final String APPROVED_NOT = "待审批";

    public static final String APPROVED_PASS = "已审批";

    // Application type
    public static final String TYPE_SPACE_APPLY = "申请空间";

    public static final String TYPE_SPACE_EXPAND = "空间扩容";

    public static final String TYPE_SPACE_JOIN = "加入空间";

    // Approval results
    public static final String RESULT_PASS = "通过";

    public static final String RESULT_NOT_PASS = "不通过";

    @Id
    @ApiModelProperty(hidden = true)
    private String applyId;

    // Application content
    private String content;

    // Application Description
    private String description;

    @ApiModelProperty(name = "type", value = "类型-空间扩容-申请空间", required = true)
    private String // Type space expansion//Apply for space
    type;

    @ApiModelProperty(hidden = true)
    private String // Approval status pending approval approved
    approvedStates;

    @ApiModelProperty(hidden = true)
    private String // Approval result passed but not passed
    approvedResult;

    @ApiModelProperty(hidden = true)
    private Person // applicant
    applicant;

    @ApiModelProperty(hidden = true)
    private Person // Approved by
    approver;

    @ApiModelProperty(hidden = true)
    private String // Submission date
    submitDate;

    @ApiModelProperty(hidden = true)
    private String // Completion Date
    completedDate;

    @ApiModelProperty(hidden = true)
    private String // Reason for rejection
    reject;

    @ApiModelProperty(name = "spaceId", value = "操作的空间")
    private String // Space for operation
    spaceId;

    // Space for operation
    private String spaceName;

    // Cover of operation
    private String spaceLogo;

    // Spatial description
    private String spaceDescription;

    // Space tag
    private List<String> spaceTag;

    // Application size
    private long size;
}
