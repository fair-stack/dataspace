package cn.cnic.dataspace.api.model.center;

import lombok.Data;
import javax.validation.constraints.NotBlank;

/**
 * Cstr registered entity
 */
@Data
public class CSTR {

    @NotBlank(message = "中文名称 不能为空")
    private String // Chinese name
    titleZh;

    @NotBlank(message = "英文名称 不能为空")
    private String // English name
    titleEn;

    @NotBlank(message = "CSTR 不能为空")
    private String // CSTR
    cstr;

    @NotBlank(message = "五位机构编码 不能为空")
    private String // Five digit institution code
    number;

    @NotBlank(message = "资源类型 不能为空")
    private String // Resource Type
    resourceType;

    @NotBlank(message = "关键词 不能为空")
    private String // keyword
    keywords;

    @NotBlank(message = "描述信息 不能为空")
    private String // Descriptive information
    description;

    @NotBlank(message = "资源生成日期 不能为空")
    private String // Resource Generation Date
    resourceDate;

    @NotBlank(message = "资源信息链接地址 不能为空")
    private String // Resource information link address
    url;

    @NotBlank(message = "共享途径 不能为空")
    private String // Sharing pathways
    shareChannel;

    @NotBlank(message = "共享范围 不能为空")
    private String // Shared Scope
    shareRange;

    @NotBlank(message = "申请流程 不能为空")
    private String // Application process
    process;

    @NotBlank(message = "学科分类 不能为空")
    private String // Discipline classification
    categoryName;

    @NotBlank(message = "主题分类 不能为空")
    private String // Topic classification
    standardName;

    @NotBlank(message = "服务机构名称 不能为空")
    private String // Service Institution Name
    serviceOrgName;

    @NotBlank(message = "服务机构通信地址 不能为空")
    private String // Service Institution Correspondence Address
    serviceOrgAddress;

    @NotBlank(message = "服务机构电子信箱 不能为空")
    private String // Service Institution Email
    serviceOrgEmail;

    @NotBlank(message = "服务机构联系电话 不能为空")
    private String // Service organization contact number
    serviceOrgPhone;

    @NotBlank(message = "服务机构邮政编码 不能为空")
    private String // Postal Code of Service Institution
    serviceOrgPostcode;
}
