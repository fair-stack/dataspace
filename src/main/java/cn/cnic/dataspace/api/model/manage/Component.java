package cn.cnic.dataspace.api.model.manage;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Installation component information
 */
@Data
@Document(collection = "component")
public class Component {

    @Id
    private String id;

    @NotNull(message = "name is not null")
    private String name;

    @NotNull(message = "logo is not null")
    private String logo;

    @NotNull(message = "description is not null")
    private String description;

    private String size;

    @NotNull(message = "authorName is not null")
    private String authorName;

    @NotNull(message = "category is not null")
    private String category;

    @NotNull(message = "componentId is not null")
    private String componentId;

    @NotNull(message = "bundle is not null")
    private String bundle;

    private List<Map<String, Object>> parameters;

    private List<String> fileTypes;

    private Date installTime;

    private String sourcePath;

    private String webPath;
}
