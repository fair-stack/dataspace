package cn.cnic.dataspace.api.model.manage;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * ApproveSetting
 *
 * @author wangCc
 * @date 2021-11-16 18:16
 */
@Data
@Builder
@Document(collection = "approveSetting")
public class ApproveSetting {

    public static final String NOT_APPROVED = "0";

    public static final String NEED_APPROVED = "1";

    @Id
    private String approveId;

    // 0 does not require approval 1 requires approval
    private String approved;

    // Total system volume
    private Long storage;

    private long gb;
}
