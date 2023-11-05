package cn.cnic.dataspace.api.model.file;

import lombok.Data;
import javax.validation.constraints.NotNull;

/**
 * Parameter classes related to breakpoint upload and shard upload files
 */
@Data
public class FileRequest {

    @NotNull(message = "文件名称不能为空")
    private String name;

    @NotNull(message = "当前分配文件大小不能为空")
    private String size;

    @NotNull(message = "总文件大小不能为空")
    private String total;

    @NotNull(message = "分片总数不能为空")
    private String chunkCount;

    @NotNull(message = "当前分片数不能为空")
    private String chunkIndex;

    @NotNull(message = "文件md5不能为空")
    private String // First file+Last file+Modification time
    fileMd5;

    @NotNull(message = "空间标识不能为空")
    private String spaceId;

    @NotNull(message = "文件路径不能为空")
    private String hash;

    private Boolean replace;

    private String code;
}
