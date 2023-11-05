package cn.cnic.dataspace.api.model.harvest;

import lombok.Data;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class TaskImpRequest {

    @NotNull(message = "linkUrl is not null")
    private String linkUrl;

    private String password;

    @NotNull(message = "spaceId is not null")
    private String spaceId;

    @NotNull(message = "way is not null")
    private String way;

    private List<String> hashList;
}
