package cn.cnic.dataspace.api.datax.admin.tool.meta;

/**
 * PostgreSQL database meta information query
 */
public class PostgresqlDatabaseMeta extends BaseDatabaseMeta implements DatabaseInterface {

    private volatile static PostgresqlDatabaseMeta single;

    public static PostgresqlDatabaseMeta getInstance() {
        if (single == null) {
            synchronized (PostgresqlDatabaseMeta.class) {
                if (single == null) {
                    single = new PostgresqlDatabaseMeta();
                }
            }
        }
        return single;
    }

    @Override
    public String getSQLQueryPrimaryKey() {
        return "SELECT\n" + "    pg_attribute.attname AS colname\n" + "FROM\n" + "    pg_constraint\n" + "INNER JOIN pg_class ON pg_constraint.conrelid = pg_class.oid\n" + "INNER JOIN pg_attribute ON pg_attribute.attrelid = pg_class.oid\n" + "AND pg_attribute.attnum = pg_constraint.conkey [ 1 ]\n" + "INNER JOIN pg_type ON pg_type.oid = pg_attribute.atttypid\n" + "WHERE\n" + "    pg_class.relname = ?\n" + "AND pg_constraint.contype = 'p';";
    }

    @Override
    public String getSQLQueryTableNameComment() {
        return "select relname as table_name,cast(obj_description(relfilenode,'pg_class') as varchar) as table_comment from pg_class c \n" + "where relname = ? and relkind = 'r' and relname not like 'pg_%' and relname not like 'sql_%' order by relname ";
    }

    @Override
    public String getSQLQueryTables() {
        return "select relname as tabname from pg_class c \n" + "where  relkind = 'r' and relname not like 'pg_%' and relname not like 'sql_%' group by relname order by relname limit 500";
    }

    @Override
    public String getSQLQueryTables(String... tableSchema) {
        return "SELECT concat_ws('.',\"table_schema\",\"table_name\") FROM information_schema.tables \n" + "where (\"table_name\" not like 'pg_%' AND \"table_name\" not like 'sql_%') \n" + "and table_type='BASE TABLE' and table_schema='" + tableSchema[0] + "'";
    }

    @Override
    public String getSQLQueryTableSchema(String... args) {
        return "select table_schema FROM information_schema.tables where \"table_name\" not like 'pg_%' or \"table_name\" not like 'sql_%' group by table_schema;";
    }

    @Override
    public String getSQLQueryColumns(String... args) {
        return "SELECT a.attname as name \n" + "FROM pg_class as c,pg_attribute as a where c.relname = ? and a.attrelid = c.oid and a.attnum>0";
    }

    @Override
    public String getSQLQueryComment(String schemaName, String tableName, String columnName) {
        return null;
    }
}
