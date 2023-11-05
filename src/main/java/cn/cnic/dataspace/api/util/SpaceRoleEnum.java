package cn.cnic.dataspace.api.util;

/**
 * Space permission enumeration
 */
public enum SpaceRoleEnum {

    // file right
    F_MAKE("MAKE", "新增", "F_MAKE", "创建文件/文件夹（包括新建、上传、复制、压缩、解压等操作而新增文件）"),
    // F_ CMD ("CMD", "View", "F_CMD", "View File/Folder"),
    F_EDIT_AM("EDIT", "编辑", "F_EDIT_AM", "编辑本人文件/文件夹"),
    F_EDIT_OT("EDIT", "编辑", "F_EDIT_OT", "编辑他人文件/文件夹"),
    F_DOWN_AM("DOWN", "下载", "F_DOWN_AM", "下载本人文件/文件夹"),
    F_DOWN_OT("DOWN", "下载", "F_DOWN_OT", "下载他人文件/文件夹"),
    F_SHAR_SPACE("SHAR", "分享", "F_SHAR_SPACE", "分享空间"),
    F_SHAR_FILE_AM("SHAR", "分享", "F_SHAR_FILE_AM", "分享本人文件/文件夹"),
    F_SHAR_FILE_OT("SHAR", "分享", "F_SHAR_FILE_OT", "分享他人文件/文件夹"),
    F_DEL_AM("DEL", "删除", "F_DEL_AM", "删除本人文件/文件夹"),
    F_DEL_OT("DEL", "删除", "F_DEL_OT", "删除他人文件/文件夹"),
    // form
    T_CREATE("CREATE", "新建", "T_CREATE", "新建表格"),
    T_EDIT("EDIT", "编辑", "T_EDIT", "编辑表格"),
    T_DELETE("DELETE", "删除", "T_DELETE", "删除表格"),
    // File Import
    F_OTHER_IM("OTHER", "其他", "F_OTHER_IM", "空间导入"),
    F_OTHER_BAIDU("OTHER", "其他", "F_OTHER_BAIDU", "百度云盘"),
    F_OTHER_FTP("OTHER", "其他", "F_OTHER_FTP", "FTP"),
    F_OTHER_WEBDAV("OTHER", "其他", "F_OTHER_WEBDAV", "空间挂载"),
    // F_ Other_ SAFE ("Other", "Other", "F_other_SAFE", "Safe"),
    // F_ Other_ RS ("Other", "Other", "F_other RS", "Recycle Bin"),
    // Member Management
    M_ADD("ADD", "新增", "M_ADD", "邀请成员"),
    M_EDIT("EDIT", "编辑", "M_EDIT", "变更成员角色"),
    M_DEL("DEL", "删除", "M_DEL", "删除成员"),
    // release
    P_ADD("ADD", "新增", "P_ADD", "新建发布"),
    // P_ LIST ("LIST", "View", "P_LIST", "View Published List"),
    // Space configuration
    S_CONF_INFO("CONF", "空间配置", "S_CONF_INFO", "基础信息"),
    S_CONF_PER("CONF", "空间配置", "S_CONF_PER", "权限配置"),
    S_CONF_BAK("CONF", "空间配置", "S_CONF_BAK", "空间备份"),
    S_AUDIT("AUDIT", "空间审核", "S_AUDIT", "审核");

    private String action;

    private String action_desc;

    private String role;

    private String role_desc;

    SpaceRoleEnum(String action, String action_desc, String role, String role_desc) {
        this.action = action;
        this.action_desc = action_desc;
        this.role = role;
        this.role_desc = role_desc;
    }

    public String getRole() {
        return role;
    }

    public String getRole_desc() {
        return role_desc;
    }

    public String getAction() {
        return action;
    }

    public String getAction_desc() {
        return action_desc;
    }
}
