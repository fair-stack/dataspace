package cn.cnic.dataspace.api.model.release;

import lombok.Data;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * Soft connection
 */
@Data
public class RequestLn {

    @NotNull(message = "原空间 不能为空")
    private String sourceSpaceId;

    @NotNull(message = "去向空间 不能为空")
    private String targetSpaceId;

    @NotNull(message = "空间路径 不能为空")
    private String spaceHash;

    private List<String> hashList;

    @NotNull(message = "复制方式 不能为空")
    private String type;
}
