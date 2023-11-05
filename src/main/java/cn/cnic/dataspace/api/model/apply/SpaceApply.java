package cn.cnic.dataspace.api.model.apply;

import cn.cnic.dataspace.api.model.space.child.AuthorizationPerson;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;

@Data
@Document(collection = "space_apply")
public class SpaceApply {

    @Id
    private String id;

    private String userId;

    private String spaceId;

    private String reason;

    private String title;

    // applicant
    private AuthorizationPerson applicant;

    private AuthorizationPerson approver;

    private String work;

    // 0 pending approval 1 approved 2 rejected
    private int state;

    private String rejectReason;

    private String applyId;

    private Date createTime;
}
