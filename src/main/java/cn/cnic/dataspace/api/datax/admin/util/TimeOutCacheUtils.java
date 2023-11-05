package cn.cnic.dataspace.api.datax.admin.util;

import lombok.extern.slf4j.Slf4j;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Consumer;

@Slf4j
public class TimeOutCacheUtils {

    private static final Long EXPIRE_TIME = 10L;

    private static final TimeUnit TIME_UNIT = TimeUnit.MINUTES;

    private static final Map<String, Long> CACHE = new ConcurrentHashMap<>();

    private static final ScheduledExecutorService EXECUTOR_SERVICE = new ScheduledThreadPoolExecutor(Runtime.getRuntime().availableProcessors(), new ThreadFactory() {

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            // thread.setDaemon(true);
            thread.setName("cn.cnic.dataspace.datax");
            return thread;
        }
    });

    public static boolean containsKey(String key) {
        return CACHE.containsKey(key);
    }

    public static Long get(String key) {
        return CACHE.get(key);
    }

    public static void set(String key, Long val, Consumer<Long> function) {
        CACHE.put(key, val);
        EXECUTOR_SERVICE.schedule(new Runnable() {

            @Override
            public void run() {
                Long remove = CACHE.remove(key);
                if (remove != null) {
                    function.accept(remove);
                    log.info(remove + " expire");
                }
            }
        }, EXPIRE_TIME, TIME_UNIT);
    }

    public static void remove(String key, Consumer<Long> function) {
        Long remove = CACHE.remove(key);
        if (remove != null) {
            function.accept(remove);
            log.info(remove + " remove");
        }
    }
}
