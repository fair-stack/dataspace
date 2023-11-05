package cn.cnic.dataspace.api.model.center;

import lombok.Data;
import javax.validation.constraints.NotNull;

@Data
public class Paper {

    // For updating
    private String id;

    @NotNull(message = "中文名不能为空")
    private String zh_Name;

    @NotNull(message = "作者不能为空")
    private String personId;

    @NotNull(message = "摘要不能为空")
    private String introduction;

    @NotNull(message = "关键词不能为空")
    private String keyword;

    @NotNull(message = "论文类型不能为空")
    private String nature;

    // English name
    private String en_Name;

    // Discipline classification code
    private String categoryCode;

    // Funding Project ID
    private String projectId;

    // Journal name
    private String periodical;

    // Year of publication
    private String publication_year;

    // Volume number
    private String volume_number;

    // Issue number
    private String issue_number;

    // Permission account
    private String account;

    // password
    private String password;

    // Paper Release Status (Published/Under Review/Draft)
    private String publishStatus;

    private String doi;

    private String url;

    // Paper citation information
    private String referenceInfo;
}
