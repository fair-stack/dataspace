package cn.cnic.dataspace.api.model.manage;

import lombok.Data;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/**
 * Install Component Information - Update Configurable Information
 */
@Data
public class ComponentUpdate {

    @NotNull(message = "id is not null")
    private String id;

    private List<Map<String, Object>> parameters;
}
