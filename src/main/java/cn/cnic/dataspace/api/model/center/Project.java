package cn.cnic.dataspace.api.model.center;

import lombok.Data;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class Project {

    // For updating
    private String id;

    @NotNull(message = "中文名不能为空")
    private String zh_Name;

    @NotNull(message = "项目类型不能为空")
    private String fundType;

    @NotNull(message = "项目编号不能为空")
    private String identifier;

    @NotNull(message = "项目负责人不能为空")
    private String projectManager;

    private List<String> projectMember;

    // Old version of project management organization
    private String projectManagementInstitute;

    // New version of project management organization
    private List<String> projectManagementInstitutes;

    // Old version of project undertaking unit
    private String projectleadingUnit;

    // Project Undertaking Unit New Version
    private List<String> projectleadingUnits;

    private List<String> projectParticipants;

    // Project start time
    private String start_time;

    // Project End Time
    private String end_time;

    // Project Introduction
    private String description;

    // English name
    private String en_Name;

    // Crude stock
    private String size;

    // Number of files
    private String records;

    // Permission account
    private String account;

    // password
    private String password;
}
