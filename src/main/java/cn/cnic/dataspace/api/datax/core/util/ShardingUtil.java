package cn.cnic.dataspace.api.datax.core.util;

/**
 * sharding vo
 *
 * @author xuxueli 2017-07-25 21:26:38
 */
public class ShardingUtil {

    private static InheritableThreadLocal<ShardingVO> contextHolder = new InheritableThreadLocal<>();

    public static class ShardingVO {

        // sharding index
        private int index;

        // sharding total
        private int total;

        public ShardingVO(int index, int total) {
            this.index = index;
            this.total = total;
        }

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public int getTotal() {
            return total;
        }

        public void setTotal(int total) {
            this.total = total;
        }
    }

    public static void setShardingVo(ShardingVO shardingVo) {
        contextHolder.set(shardingVo);
    }

    public static ShardingVO getShardingVo() {
        return contextHolder.get();
    }
}
