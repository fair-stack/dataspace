package cn.cnic.dataspace.api.datax.admin.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum SourceType {

    OFFLINE(0, "offline"), SPACE(1, "space"), DATASOURCE(2, "datasource"), COPY(3, "copy");

    @EnumValue
    int code;

    @JsonValue
    String desc;

    SourceType(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static SourceType codeOf(int code) {
        switch(code) {
            case 0:
                return OFFLINE;
            case 1:
                return SPACE;
            case 2:
                return DATASOURCE;
            case 3:
                return COPY;
            default:
                return OFFLINE;
        }
    }
}
