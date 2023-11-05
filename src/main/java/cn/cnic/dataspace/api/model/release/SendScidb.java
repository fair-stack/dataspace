package cn.cnic.dataspace.api.model.release;

import lombok.Data;
import java.util.List;

@Data
public class SendScidb {

    private String dataSetId;

    private String version;

    // The corresponding author is usually the email of the dataset creator
    private String correspondent;

    // Image dataset cover image base6 encoding
    private String coverBase64;

    private String titleZh;

    private String titleEn;

    private String introductionEn;

    private String introductionZh;

    // Protection period (0-12)
    private String protectMonth;

    // (PUBLIC/EMBARGO This value should not be set to 0 for protectMonth if it is not PUBLIC)
    private String shareStatus;

    private List<String> keywordEn;

    private List<String> keywordZh;

    // author
    private List<Author> author;

    // license agreement
    private CopyRight copyRight;

    // paper
    private List<Paper> papers;

    // subject
    private List<Taxonomy> taxonomy;

    /*Write to death*/
    private String dataSetType = "personal";

    private String source = "dataSpace";

    // language
    private String language = "zh_CN";

    private String status = "-1";

    // Enumeration values can be found in the file type interface
    private String fileType = "001";

    private String publisher = "Science Data Bank";

    private String dataSetTypeCode = "";

    /* null */
    private String username;

    private String onLineUrl;

    // Reference Link
    private List<String> referenceLink;

    private Funding funding;

    // author
    @Data
    public static class Author {

        private String id;

        private String email;

        private String nameEn;

        private String nameZh;

        private String orcId;

        private List<Organization> organizations;

        @Data
        public static class Organization {

            private String nameZh;

            private String nameEn;

            private int order;
        }
    }

    // license agreement
    @Data
    public static class CopyRight {

        private String code;

        private String did;

        private String explain;

        private String explainEn;

        private String img;

        private String name;

        private String url;
    }

    // funding
    @Data
    public static class Funding {

        private String funding_code;

        private String funding_nameEn;

        private String funding_nameZh;

        private String type;
    }

    // paper
    @Data
    public static class Paper {

        private String citationEn;

        private String citationZh;

        private String doi;

        private String journalCode;

        private String journalEn;

        private String journalZh;

        private String manuscriptNo;

        private String state;

        private String titleEn;

        private String titleZh;

        private String url;
    }

    // Discipline classification
    @Data
    public static class Taxonomy {

        private String code;

        private String nameEn;

        private String nameZh;
    }
}
