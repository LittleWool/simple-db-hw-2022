package simpledb.common;

import simpledb.storage.DbFile;
import simpledb.storage.TupleDesc;

/**
 * @ClassName: TableInfo
 * @Description:
 * @Author: LittleWool
 * @Date: 2025/9/16 10:01
 * @Version: 1.0
 **/

public class TableInfo {

    //表名
    String tableName;
   //表存储文件
    DbFile dbFile;
    //表主键
    String pkeyField;

    TupleDesc tupleDesc;

    public TupleDesc getTupleDesc() {
        return tupleDesc;
    }

    public void setTupleDesc(TupleDesc tupleDesc) {
        this.tupleDesc = tupleDesc;
    }

    public TableInfo( String tableName, DbFile dbFile, String pkeyField) {

        this.tableName = tableName;
        this.dbFile = dbFile;
        this.pkeyField = pkeyField;
    }



    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public DbFile getDbFile() {
        return dbFile;
    }

    public void setDbFile(DbFile dbFile) {
        this.dbFile = dbFile;
    }

    public String getPkeyField() {
        return pkeyField;
    }

    public void setPkeyField(String pkeyField) {
        this.pkeyField = pkeyField;
    }
}
