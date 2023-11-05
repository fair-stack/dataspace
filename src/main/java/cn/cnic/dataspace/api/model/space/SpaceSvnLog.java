package cn.cnic.dataspace.api.model.space;

import cn.cnic.dataspace.api.model.space.child.Operator;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;

/**
 * SpaceSvnLog
 *
 * @author wangCc
 * @date 2021-04-12 09:41
 */
@Data
@Builder
@Document(collection = "spaceSvn")
public class SpaceSvnLog {

    public static final String METHOD = "method";

    public static final String ELFINDER = "web";

    public static final String WEBDAV = "webDAV";

    public static final String TABLE = "TABLE";

    public static final String FTP = "ftp";

    public static final int ACTION_VALUE = -2;

    public static final String FILE_UPLOAD = "upload";

    public static final String FILE_DOWNLOAD = "download";

    public static final String FILE_DELETE = "delete";

    public static final String FILE_CREATE = "create";

    public static final String FILE_MOVE = "move";

    public static final String FILE_COPY = "copy";

    public static final String FILE_RENAME = "rename";

    public static final String FILE_MODIFY = "modify";

    public static final String FILE_PASS_ON = "pass_on";

    public static final String ACTION_FILE = "file";

    public static final String ACTION_PUBLISH = "publish";

    public static final String ACTION_MEMBER = "member";

    public static final String ACTION_VERSION = "version";

    // Structured Data Operations
    public static final String ACTION_TABLE = "table";

    public static final String ACTION_OTHER = "other";

    @Id
    private String spaceSvnId;

    private String spaceId;

    /**
     * -2 User behavior
     */
    private long version;

    private Operator operator;

    private String description;

    private Date createTime;

    private String updateDateTime;

    private String method;

    private String tag;

    private String action;

    private String fileAction;

    private String operatorId;
}
