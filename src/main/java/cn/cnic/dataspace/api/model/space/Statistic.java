// package cn.cnic.dataspace.api.model.disk;
// 
// import lombok.Builder;
// import lombok.Data;
// import org.springframework.data.annotation.Id;
// import org.springframework.data.mongodb.core.mapping.Document;
// 
// /**
// * Statistic
// *
// * @author wangCc
// * @date 2021-11-05 10:55
// */
// @Data
// @Builder
// @Document(collection = "statistic")
// public class Statistic {
// 
// public static final String TYPE_DOWNLOAD = "download";
// public static final String TYPE_VIEW = "view";
// 
// @Id
// private String id;
// private String spaceId;
// private String homeUrl;
// private String type;
// private double count;
// }
