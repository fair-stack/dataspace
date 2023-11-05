package cn.cnic.dataspace.api.model.release;

import lombok.Data;

@Data
public class ResultData {

    private String message;

    private int code;

    private Object data;
}
