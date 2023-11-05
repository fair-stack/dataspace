package cn.cnic.dataspace.api.model.user;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * User model modification transfer object
 */
@Data
public class ConsumerInfoDTO {

    private String userId;

    @ApiModelProperty(name = "emailAccounts", value = "邮箱账号", example = "admin", required = true)
    private String emailAccounts;

    @ApiModelProperty(name = "name", value = "姓名", example = "张**", required = true)
    private String name;

    @ApiModelProperty(name = "englishName", value = "英文名", example = "")
    private String englishName;

    @ApiModelProperty(name = "orgChineseName", value = "机构中文名", example = "")
    private String orgChineseName;

    @ApiModelProperty(name = "orgEnglishName", value = "机构英文名", example = "")
    private String orgEnglishName;

    @ApiModelProperty(name = "orcId", value = "ORCID", example = "")
    private String orcId;

    @ApiModelProperty(name = "telephone", value = "手机号", example = "")
    private String telephone;

    @ApiModelProperty(name = "introduction", value = "简介")
    private String introduction;

    private String avatar;
}
