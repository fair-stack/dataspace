package cn.cnic.dataspace.api.model.file;

import lombok.Data;
import javax.validation.constraints.NotNull;

@Data
public class FileCheck {

    @NotNull(message = "文件名称不能为空")
    private String name;

    @NotNull(message = "文件大小不能为空")
    private String total;

    @NotNull(message = "空间标识不能为空")
    private String spaceId;

    @NotNull(message = "文件路径不能为空")
    private String hash;
}
