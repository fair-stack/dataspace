package cn.cnic.dataspace.api.model.release.template;

import lombok.Data;
import java.util.List;

/**
 * @Auther: wdd
 * @Date: 2021/03/19/20:04
 * @Description:
 */
@Data
public class Template {

    // Template Name
    private String templateName;

    // Template Description
    private String templateDesc;

    // Template author
    private String templateAuthor;

    // Template author
    private String version;

    private List<Group> group;

    /**
     * Metadata information
     */
    @Data
    public static class Resource {

        // Corresponding key
        private String name;

        // Chinese title
        private String title;

        private String placeholder;

        private String type;

        private String check;

        private String url;

        // 1: 1 to many can add multiple 0: 1, 0 to 1, not mandatory, can fill in at most one 1:1 mandatory
        private String multiply;

        // iri url
        private String iri;

        private String language;

        private String isAll;

        private Object value;

        // Generally used for formatting restrictions on dates
        private String formate;

        // Corresponding children
        private List<Options> options;

        private List<Options> operation;

        // Inquiry inquiry
        private String mode;

        private List<Options> show;
    }

    /**
     * options
     */
    @Data
    public static class Options {

        private String name;

        private String url;

        private String title;

        private String type;

        private String isAll;

        private String formate;

        private String mode;

        private String placeholder;

        private List<Children> children;
    }

    @Data
    public static class Group {

        private String name;

        private String desc;

        // data set
        private List<Resource> resources;
    }

    /**
     * children
     */
    @Data
    public static class Children {

        private String name;

        private String title;

        private String type;

        private String isAll;

        private String formate;

        private String multiply;

        private String placeholder;

        private String url;

        private List<Options> options;
    }
}
