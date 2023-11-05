package cn.cnic.dataspace.api.model.manage;

import lombok.Data;
import org.springframework.data.annotation.Id;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Installation Component Information - Return Fields
 */
@Data
public class ComponentShow {

    @Id
    private String id;

    @NotNull(message = "name is not null")
    private String name;

    @NotNull(message = "logo is not null")
    private String logo;

    @NotNull(message = "description is not null")
    private String description;

    // private String size;
    @NotNull(message = "authorName is not null")
    private String authorName;

    @NotNull(message = "category is not null")
    private String category;

    private Date installTime;

    private String webPath;

    private List<Map<String, Object>> parameters;

    private Map<String, Object> parameterMap;
}
