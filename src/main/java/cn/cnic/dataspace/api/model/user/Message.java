package cn.cnic.dataspace.api.model.user;

import cn.cnic.dataspace.api.model.space.child.Person;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * message
 *
 * @author wangCc
 * @date 2021-04-06 17:11
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "message")
public class Message {

    public static final String TITLE_PENDING = "待审批提醒";

    public static final String TITLE_PASSED = "申请已通过";

    public static final String TITLE_NOT_PASSED = "申请未通过审批";

    public static final String TITLE_INVITED = "邀请加入通知";

    public static final String TITLE_PUBLISH_PASSED = "您提交的数据资源已通过审核";

    public static final String TITLE_PUBLISH_NOT_PASSED = "您提交的数据资源未通过审核";

    public static final String TITLE_DISK_SUCCESS = "百度网盘导入成功";

    public static final String TITLE_DISK_FAIL = "百度网盘导入失败";

    public static final String TITLE_SPACE_SUCCESS = "空间导入成功";

    public static final String TITLE_SPACE_FAIL = "空间导入失败";

    public static final String TITLE_APPLY = "申请加入通知";

    @Id
    private String messageId;

    private String title;

    // Message content
    private String content;

    // Operation time
    private String msgDate;

    // Applicant
    private Person applicant;

    // Approved by
    private Person approver;

    private String linkUrl;

    // Result 1 passed 0 failed
    private Integer result;

    // Is it an administrator message 0 not 1 yes
    private Integer adminMsg;

    // 0 unread 1 read
    private Integer haveRead;

    // Extension field user jsonObject display
    private String extension;
}
