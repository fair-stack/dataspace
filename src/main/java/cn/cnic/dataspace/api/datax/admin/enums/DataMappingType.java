package cn.cnic.dataspace.api.datax.admin.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum DataMappingType {

    FILE("file", 0), DIR("dir", 1);

    DataMappingType(String name, Integer code) {
        this.name = name;
        this.code = code;
    }

    @EnumValue
    Integer code;

    @JsonValue
    String name;
}
