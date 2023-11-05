package cn.cnic.dataspace.api.datax.admin.util;

/**
 * Special note: It is best to provide a toString() implementation. For example:
 */
public interface ErrorCode {

    // Error code number
    String getCode();

    // Error code description
    String getDescription();

    /**
     * An implementation of toString must be provided
     */
    String toString();
}
