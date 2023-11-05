package cn.cnic.dataspace.api.model.release;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
public class ResourceDo implements Serializable {

    @NotBlank(message = "name 不能为空")
    private String name;

    private Object value;

    @NotBlank(message = "title 不能为空")
    private String title;

    @NotBlank(message = "type 不能为空")
    private String type;

    @NotBlank(message = "iri 不能为空")
    private String iri;

    private String formate;

    private String language;
}
