package cn.cnic.dataspace.api.model.release.stemcells;

import lombok.Data;

@Data
public class SampleCore {

    private String name;

    private Object value;

    private Boolean perfect;

    private String annotation;
}
