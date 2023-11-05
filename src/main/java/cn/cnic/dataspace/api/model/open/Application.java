package cn.cnic.dataspace.api.model.open;

import cn.cnic.dataspace.api.model.space.child.Person;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import javax.validation.constraints.NotNull;
import java.util.Date;

/**
 * Authorization distribution record
 */
@Data
@Document(collection = "application")
public class Application {

    @Id
    private String id;

    @NotNull(message = "申请机构名称不能为空")
    private String appName;

    @NotNull(message = "描述不能为空")
    private String description;

    // @Notnull (message="Applicant cannot be empty")
    // Private String application// applicant
    // 
    // @Notnull (message="The applicant's contact information cannot be empty")
    // Private String contact// Applicant's contact information
    private Person person;

    private Long appKey;

    private String appSecret;

    // 0 enable 1 disable
    private int state;

    private Date updateTime;

    private Date createTime;
}
