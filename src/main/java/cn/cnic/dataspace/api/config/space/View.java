package cn.cnic.dataspace.api.config.space;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * view annotation
 *
 * @author wangCc
 * @date 2021-10-9 16:25:44
 */
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface View {
}
