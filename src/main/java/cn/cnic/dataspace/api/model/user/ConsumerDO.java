package cn.cnic.dataspace.api.model.user;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

/**
 * @ author chl
 */
@Data
@Document(collection = "db_user")
public class ConsumerDO {

    @Id
    private String id;

    @ApiModelProperty(name = "emailAccounts", value = "邮箱账号", example = "admin", required = true)
    private String emailAccounts;

    @ApiModelProperty(name = "name", value = "姓名", example = "张**", required = true)
    private String name;

    @ApiModelProperty(name = "englishName", value = "英文名", example = "")
    private String englishName;

    @ApiModelProperty(name = "password", value = "密码", example = "123456")
    private String password;

    @ApiModelProperty(value = "头像")
    private String avatar;

    @ApiModelProperty(name = "orgChineseName", value = "机构中文名", example = "")
    private String orgChineseName;

    @ApiModelProperty(name = "orgEnglishName", value = "机构英文名", example = "")
    private String orgEnglishName;

    @ApiModelProperty(name = "telephone", value = "手机号", example = "")
    private String telephone;

    @ApiModelProperty(name = "introduction", value = "简介")
    private String introduction;

    @ApiModelProperty(name = "roles", value = "角色logo集合")
    private List<String> roles;

    @ApiModelProperty(name = "createTime", value = "创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty(name = "updateTime", value = "修改时间", hidden = true)
    private LocalDateTime updateTime;

    // 0 not activated 1 activated 2 not registered
    @ApiModelProperty(name = "state", value = "用户是否激活(0/1)")
    private int state;

    @ApiModelProperty(name = "spareEmail", value = "备用邮箱")
    private String spareEmail;

    // Add Method
    private String addWay;

    // Number of password errors - disabled
    private boolean disablePwd = false;

    // Password error count disable time
    private Date disablePwdTime;

    // WeChat binding user ID
    private String wechatId;

    private String appKey;

    // 0 enable 1 disable
    private int disable;

    // Whether online true online false offline
    private Boolean online;

    // Last login time
    private Date lastLoginTime;

    // Last Exit Time
    private Date lastLogoutTime;
}
