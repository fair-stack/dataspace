package cn.cnic.dataspace.api.model.manage;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Date;

/**
 * Publish account configuration
 */
@Data
@Document(collection = "release_account")
public class ReleaseAccount {

    @Id
    private String id;

    @NotBlank(message = "发布机构 不能为空")
    private String orgId;

    @NotBlank(message = "发布机构 不能为空")
    private String org;

    @NotBlank(message = "授权码 不能为空")
    private String authCode;

    private Date createTime;

    private Date updateTime;
}
