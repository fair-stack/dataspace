package cn.cnic.dataspace.api.currentlimiting;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
@Documented
public @interface Limit {

    // Resource key
    String key() default "";

    // Maximum Visits
    double permitsPerSecond();

    // time
    long timeout();

    // Time type
    TimeUnit timeunit() default TimeUnit.MILLISECONDS;

    // Reminder Information
    String msg() default "系统繁忙,请稍后再试";
}
