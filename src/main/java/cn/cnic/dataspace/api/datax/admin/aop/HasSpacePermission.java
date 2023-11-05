package cn.cnic.dataspace.api.datax.admin.aop;

import java.lang.annotation.*;

/**
 * Determine if there is space permission
 */
@Target({ ElementType.PARAMETER, ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface HasSpacePermission {

    SpacePermission value() default SpacePermission.T_READ;
}
