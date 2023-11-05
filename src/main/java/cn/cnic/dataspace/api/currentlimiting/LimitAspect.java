package cn.cnic.dataspace.api.currentlimiting;

import cn.cnic.dataspace.api.exception.OpenApiException;
import cn.cnic.dataspace.api.util.APICodeType;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * @ author chl
 */
@Slf4j
@Aspect
@Component
public class LimitAspect {

    private final Map<String, RateLimiter> limitMap = Maps.newConcurrentMap();

    @Around("@annotation(cn.cnic.dataspace.api.currentlimiting.Limit)")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
        // Annotate with limit
        Limit limit = method.getAnnotation(Limit.class);
        if (limit != null) {
            // Key function: different interfaces, different flow control
            String key = limit.key();
            RateLimiter rateLimiter;
            // Verify if there are hit keys in the cache
            if (!limitMap.containsKey(key)) {
                // Create token bucket
                rateLimiter = RateLimiter.create(limit.permitsPerSecond());
                limitMap.put(key, rateLimiter);
                log.info("新建了令牌桶={}，容量={}", key, limit.permitsPerSecond());
            }
            rateLimiter = limitMap.get(key);
            // Take a token
            boolean acquire = rateLimiter.tryAcquire(limit.timeout(), limit.timeunit());
            // Unable to obtain command, returning an exception prompt directly
            if (!acquire) {
                log.debug("令牌桶={}，获取令牌失败", key);
                throw new OpenApiException(APICodeType.Current_limiting.getCode(), limit.msg());
            }
        }
        return pjp.proceed();
    }
}
