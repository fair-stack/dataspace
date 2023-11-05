package cn.cnic.dataspace.api.model.file;

import lombok.Data;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class FileDelete {

    @NotNull(message = "空间标识不能为空")
    private String spaceId;

    private List<String> hashList;
}
