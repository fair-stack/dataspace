package cn.cnic.dataspace.api.datax.admin.tool.meta;

/**
 * SqlServer Database Meta Information Query
 */
public class SqlServerDatabaseMeta extends BaseDatabaseMeta implements DatabaseInterface {

    private volatile static SqlServerDatabaseMeta single;

    public static SqlServerDatabaseMeta getInstance() {
        if (single == null) {
            synchronized (SqlServerDatabaseMeta.class) {
                if (single == null) {
                    single = new SqlServerDatabaseMeta();
                }
            }
        }
        return single;
    }

    @Override
    public String getSQLQueryTables() {
        return "SELECT Name FROM SysObjects Where XType='U' ORDER BY Name";
    }

    @Override
    public String getSQLQueryTables(String... tableSchema) {
        return "select schema_name(schema_id)+'.'+object_name(object_id) from sys.objects \n" + "where type ='U' \n" + "and schema_name(schema_id) ='" + tableSchema[0] + "'";
    }

    @Override
    public String getSQLQueryPrimaryKey() {
        return "SELECT COLUMN_NAME,CONSTRAINT_NAME = (STUFF((SELECT ',' + COLUMN_NAME FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE WHERE TABLE_NAME = Test.TABLE_NAME FOR XML PATH ( '' ) ),1,1,'' )) FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE AS Test WHERE TABLE_NAME = ? GROUP BY COLUMN_NAME,CONSTRAINT_NAME,TABLE_NAME";
    }

    @Override
    public String getSQLQueryTableNameComment() {
        return "SELECT DISTINCT\n" + "    cast(d.name  as varchar(100)) as tableName,\n" + "    cast(f.value as varchar(500)) as tableComment\n" + "FROM\n" + "    syscolumns a\n" + "    LEFT JOIN systypes b ON a.xusertype= b.xusertype\n" + "    INNER JOIN sysobjects d ON a.id= d.id \n" + "    AND d.xtype= 'U' \n" + "    AND d.name<> 'dtproperties'\n" + "    LEFT JOIN syscomments e ON a.cdefault= e.id\n" + "    LEFT JOIN sys.extended_properties g ON a.id= G.major_id \n" + "    AND a.colid= g.minor_id\n" + "    LEFT JOIN sys.extended_properties f ON d.id= f.major_id \n" + "    AND f.minor_id= 0 where d.name = ?";
    }

    @Override
    public String getSQLQueryTableSchema(String... args) {
        return "select distinct schema_name(schema_id) from sys.objects where type ='U';";
    }

    @Override
    public String getDatas(String tableName) {
        return "select top 10 * from " + tableName;
    }
}
