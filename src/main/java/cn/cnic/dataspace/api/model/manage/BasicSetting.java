package cn.cnic.dataspace.api.model.manage;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

/**
 * BasicSetting
 *
 * @author wangCc
 * @date 2021-04-08 16:44
 */
@Data
@Document(collection = "setting")
public class BasicSetting {

    @Id
    private String settingId;

    private String dataSpaceName;

    private String logo;

    private String topic;

    private List<Link> links;

    private String copyright;

    private String banner;

    private List<String> banners;

    private String version;

    private String indexTitle;

    private String indexDescription;

    private String clause;

    @Data
    private static class Link {

        private String key;

        private String value;
    }
}
