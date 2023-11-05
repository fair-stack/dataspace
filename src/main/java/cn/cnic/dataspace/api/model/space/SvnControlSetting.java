package cn.cnic.dataspace.api.model.space;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * SvnControlSetting
 *
 * @author wangCc
 * @date 2021-11-21 14:34
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "svnSetting")
public class SvnControlSetting {

    public static final String CLOSED = "0";

    public static final String OPEN = "1";

    @Id
    private String svnId;

    // 0 off 1 on
    private String need;
}
