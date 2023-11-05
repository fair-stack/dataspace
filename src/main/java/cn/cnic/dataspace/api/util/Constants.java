package cn.cnic.dataspace.api.util;

/**
 * Service Application Constants
 */
public final class Constants {

    // drafts
    public static final Integer DRAFT = 0;

    // Pending review
    public static final Integer AUDIT = 1;

    // release
    public static final Integer PUBLISHED = 2;

    // Reject
    public static final Integer REJECT = 3;

    // Initial version
    public static final String VERSION = "V1";

    // role
    // administrators
    public static final String ADMIN = "role_admin";

    // Advanced Users
    public static final String SENIOR = "role_senior";

    // General users
    public static final String GENERAL = "role_general";

    // Template mandatory identification
    public static final String CHECK = "check";

    // At least one unlimited limit
    public static final String LEAST = "1:*";

    public static final String INS_TOKEN = "INS_authorization_";

    // System organization type identification
    public static final String INSERT = "insertDB";

    public static final String SCI = "scienceDB";

    // Institution Address Identification
    // revoke
    public static final String DATASET_CANCEL = "DATASET_CANCEL";

    // Obtain template interface
    public static final String GET_TEMPLATES = "GET_TEMPLATES";

    // Publish submission interface
    public static final String DATASET_PUBLISH = "DATASET_PUBLISH";

    // Push successful notification
    public static final String UPLOAD_COMPLETED = "UPLOAD_COMPLETED";

    // Refresh
    public static final String REFRESH = "refresh";

    // obtain
    public static final String GAIN = "gain";

    // 
    public static final String TOKEN = "DS_TOKEN";

    public static final String WAY = "WAY";

    public static final String SYSTEM_STARTUP = "system_startup";

    /**
     * System cache identification
     */
    public static final class CaffeType {

        public static final String ACC_OPEN = "accOpen";

        public static final String ACC = "acc";

        public static final String PUBLIC_ORG = "publicOrg";

        public static final String BASIS = "basis";

        public static final String DATA_SIZE = "dataSize";

        public static final String SYS_EMAIL = "sys_email";

        public static final String SYS_DEF_SPACE_ROLE = "sys_def_space_role";
    }

    /**
     * Space permissions
     */
    public static final class SpaceRole {

        public static final String OWNER = "拥有者";

        public static final String SENIOR = "高级";

        public static final String GENERAL = "普通";

        public static final String LEVEL_ADMIN = "ADMIN";

        public static final String LEVEL_SENIOR = "SENIOR";

        public static final String LEVEL_OTHER = "OTHER";

        public static final String ALL = "all";

        public static final String MANAGE = "manage";
    }

    public static final class LoginWay {

        // Technology Cloud Login
        public static final String UMP = "UMP_";

        // System login
        public static final String SYS = "SYS_";

        // Stem cell login
        public static final String CHANNEL = "CHANNEL_";

        // WeChat login
        public static final String WECHAT = "WECHAT_";

        // Shared Network
        public static final String ESCIENCE = "ESCIENCE_";

        // Baidu Netdisk
        public static final String NETWORK = "NETWORK_";

        public static final String OPEN = "OPEN_";

        // user
        public static final String NETWORK_USER = "NETWORK_USER";
    }

    public static final class SocketType {

        // Task added and waiting for processing
        public static final String TS_DRAW = "ts_draw";

        // Current file download progress message
        public static final String TS_FILE = "ts_file";

        // Current task progress message
        public static final String TS_TASK_MAIN = "ts_task_main";

        // Task preparation ends and begins processing
        public static final String TS_START = "ts_start";

        // Task End
        public static final String TS_END = "ts_end";

        // End of Subtask
        public static final String TS_LEVEL_END = "ts_level_end";

        // Task failed
        public static final String TS_ERROR = "ts_error";
    }

    public static final class TaskType {

        // Share Import
        public static final String SHARE_IMP = "share_imp";

        // Network disk import
        public static final String NET_IMP = "net_imp";

        // Internal system import
        public static final String SYS_IMP = "SYS_IMP";
    }

    /**
     * Disaster recovery constant
     */
    public static final class Backup {

        // state
        // open
        public static final String START = "start";

        // close
        public static final String STOP = "stop";

        // cycle
        // Every day
        public static final String DAY = "day";

        // weekly
        public static final String WEEK = "week";

        // monthly
        public static final String MONTH = "month";

        // strategy
        // Keep All
        public static final String ALL = "all";

        // Keep for the last three times
        public static final String THREE = "three";

        // Keep Latest
        public static final String NEW = "new";
    }

    public static final class Image {

        // Identification - Parent
        public static final String image = "image";

        // space
        public static final String SPACE = "space";

        // user
        public static final String USER = "user";

        // release
        public static final String RESOURCE = "resource";
    }

    public static final class OpenApiState {

        // Go Live
        public static final String online = "online";

        // Offline
        public static final String offline = "offline";

        // long-term
        public static final String Long_Time = "LT";

        // short-term
        public static final String Short_Time = "ST";
    }

    public static final class // System file spare space address
    Release {

        // File sharding temporary storage directory
        public static final String FILE_SHARD = "file_shard";

        // File compression temporary directory
        public static final String FILE_ZIP = "file_zip";

        // Reject File Staging
        public static final String PUBLIC_REJECTED = "public_rejected";
    }

    /**
     * Short time email sending verification category
     */
    public static final class EmailSendType {

        public static final String EMAIL_CODE = "email_code";

        // register
        public static final String REGISTER = "register_";

        // register
        public static final String REGISTER_TO = "register_to_";

        // Forgot password
        public static final String FORGET_PWD = "forget_pwd_";

        // Forgot password
        public static final String FORGET_PWD_TO = "forget_pwd_to_";

        // Change password
        public static final String UPDATE_PWD = "update_pwd_";
    }
}
