package cn.cnic.dataspace.api.model.email;

public final class EmailRole {

    // Space Creation Request
    public static final String SPACE_CREATE_APPLY = "space_create_apply";

    // Space invitation
    public static final String SPACE_INVITE = "space_invite";

    // Space expansion application
    public static final String SPACE_CAPACITY_APPLY = "space_capacity_apply";

    // Space application to join
    public static final String SPACE_JOIN_APPLY = "space_join_apply";

    // Data release review
    public static final String DATA_PUBLIC_AUDIT = "data_public_audit";

    public static final String[] list = { SPACE_CREATE_APPLY, SPACE_INVITE, SPACE_CAPACITY_APPLY, SPACE_JOIN_APPLY, DATA_PUBLIC_AUDIT };

    public static final String[] listTo = { SPACE_CREATE_APPLY + ":空间创建申请", SPACE_INVITE + ":空间邀请", SPACE_CAPACITY_APPLY + ":空间扩容申请", SPACE_JOIN_APPLY + ":空间申请加入", DATA_PUBLIC_AUDIT + ":数据发布审核" };
}
