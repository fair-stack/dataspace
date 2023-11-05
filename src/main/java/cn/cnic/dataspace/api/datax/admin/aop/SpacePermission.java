package cn.cnic.dataspace.api.datax.admin.aop;

public enum SpacePermission {

    /**
     * Invalidate annotations
     */
    NO_VALID("NO_VALID", 0),
    T_READ("T_READ", 1),
    T_CREATE("T_CREATE", 2),
    T_EDIT("T_EDIT", 3),
    T_DELETE("T_DELETE", 4);

    String tPermission;

    int code;

    SpacePermission(String tPermission, int code) {
        this.tPermission = tPermission;
        this.code = code;
    }

    public String gettPermission() {
        return tPermission;
    }
}
