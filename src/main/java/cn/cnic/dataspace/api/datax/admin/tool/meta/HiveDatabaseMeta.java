package cn.cnic.dataspace.api.datax.admin.tool.meta;

/**
 * Hive metadata information
 */
public class HiveDatabaseMeta extends BaseDatabaseMeta implements DatabaseInterface {

    private volatile static HiveDatabaseMeta single;

    public static HiveDatabaseMeta getInstance() {
        if (single == null) {
            synchronized (HiveDatabaseMeta.class) {
                if (single == null) {
                    single = new HiveDatabaseMeta();
                }
            }
        }
        return single;
    }

    @Override
    public String getSQLQueryTables() {
        return "show tables";
    }
}
