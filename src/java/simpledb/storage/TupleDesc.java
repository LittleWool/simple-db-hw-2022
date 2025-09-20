package simpledb.storage;

import simpledb.common.Type;

import java.io.Serializable;
import java.util.*;

/**
 * TupleDesc describes the schema of a tuple.
 * 表模式描述信息
 */
public class TupleDesc implements Serializable {

    /**
     * A help class to facilitate organizing the information of each field
     * 存放每个字段的内部类，为什么要使用静态内部类
     *
     */
    public static class TDItem implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * The type of the field
         */
        public final Type fieldType;

        /**
         * The name of the field
         */
        public final String fieldName;

        public TDItem(Type t, String n) {
            this.fieldName = n;
            this.fieldType = t;
        }

        public String toString() {
            return fieldName + "(" + fieldType + ")";
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            TDItem tdItem = (TDItem) o;
            return fieldType == tdItem.fieldType && Objects.equals(fieldName, tdItem.fieldName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(fieldType, fieldName);
        }
    }

    /**
     * @return An iterator which iterates over all the field TDItems
     *         that are included in this TupleDesc
     */
    //存放字段的列表
    List<TDItem> tdItemList;
    public Iterator<TDItem> iterator() {
        // TODO: some code goes here
        if (tdItemList!=null){
            return tdItemList.iterator();
        }
        return null;
    }

    private static final long serialVersionUID = 1L;

    /**
     * Create a new TupleDesc with typeAr.length fields with fields of the
     * specified types, with associated named fields.
     *
     * @param typeAr  array specifying the number of and types of fields in this
     *                TupleDesc. It must contain at least one entry.
     * @param fieldAr array specifying the names of the fields. Note that names may
     *                be null.
     */
    public TupleDesc(Type[] typeAr, String[] fieldAr) {
        // TODO: some code goes here
        if (typeAr==null||fieldAr==null){
            throw new IllegalArgumentException("Arguments must not be null");
        }
        if (typeAr.length!=fieldAr.length){
            throw  new IllegalArgumentException("Two nums need have same length");
        }
        int len=typeAr.length;
        List<TDItem> tmp=new ArrayList<>(len);
        for (int i = 0; i < len; i++) {
            tmp.add(new TDItem(typeAr[i],fieldAr[i]));
        }
        this.tdItemList=tmp;
    }

    /**
     * Constructor. Create a new tuple desc with typeAr.length fields with
     * fields of the specified types, with anonymous (unnamed) fields.
     *
     * @param typeAr array specifying the number of and types of fields in this
     *               TupleDesc. It must contain at least one entry.
     */
    public TupleDesc(Type[] typeAr) {
        // TODO: some code goes here
        if(typeAr==null){
            throw new IllegalArgumentException("Arguments must not be null");
        } else{
            int len=typeAr.length;
            List<TDItem> tmp=new ArrayList<>(len);
            for (int i = 0; i < len; i++) {
                tmp.add(new TDItem(typeAr[i],""));
            }
            this.tdItemList=tmp;
        }
    }

    /**
     * 字段数量
     * @return the number of fields in this TupleDesc
     */
    public int numFields() {
        // TODO: some code goes here
        if (tdItemList==null){
            return 0;
        }else{
            return tdItemList.size();
        }
    }

    /**
     * Gets the (possibly null) field name of the ith field of this TupleDesc.
     *
     * @param i index of the field name to return. It must be a valid index.
     * @return the name of the ith field 字段名称
     * @throws NoSuchElementException if i is not a valid field reference.
     */
    public String getFieldName(int i) throws NoSuchElementException {
        // TODO: some code goes here
        if (i<numFields()){
            return tdItemList.get(i).fieldName;
        }else {
            throw new NoSuchElementException("Please enter a legal subscript");
        }

    }

    /**
     * Gets the type of the ith field of this TupleDesc.
     *
     * @param i The index of the field to get the type of. It must be a valid
     *          index.
     * @return the type of the ith field  字段类型
     * @throws NoSuchElementException if i is not a valid field reference.
     */
    public Type getFieldType(int i) throws NoSuchElementException {
        // TODO: some code goes here
        if (i<numFields()){
            return tdItemList.get(i).fieldType;
        }else {
            throw new NoSuchElementException("Please enter a legal subscript");
        }
    }

    /**
     * Find the index of the field with a given name.
     *
     * @param name name of the field.
     * @return the index of the field that is first to have the given name.  对应字段的索引
     * @throws NoSuchElementException if no field with a matching name is found.
     */
    public int indexForFieldName(String name) throws NoSuchElementException {
        // TODO: some code goes here
        if (name!=null){
            for (int i = 0; i < numFields(); i++) {
                if (name.equals(tdItemList.get(i).fieldName)){
                    return i;
                }
            }
        }

        throw new NoSuchElementException("There is no element whose name is "+name);
    }

    /**
     * @return The size (in bytes) of tuples corresponding to this TupleDesc.
     *         Note that tuples from a given TupleDesc are of a fixed size.
     *         在该表模式下的记录（元组）的固定字节大小
     */
    public int getSize() {
        // TODO: some code goes here
        int sumLen=0;
        if (tdItemList!=null){
            for (TDItem tdItem : tdItemList) {
                sumLen+=tdItem.fieldType.getLen();
            }
        }
        return sumLen;
    }

    /**
     * Merge two TupleDescs into one, with td1.numFields + td2.numFields fields,
     * with the first td1.numFields coming from td1 and the remaining from td2.
     *合并两个表的模式
     *
     * @param td1 The TupleDesc with the first fields of the new TupleDesc
     * @param td2 The TupleDesc with the last fields of the TupleDesc
     * @return the new TupleDesc
     */
    public static TupleDesc merge(TupleDesc td1, TupleDesc td2) {
        // TODO: some code goes here
        int size1=td1.numFields();
        int size2= td2.numFields();
        int k=0;
        Type[] types=new Type[size1+size2];
        String[] names =new String[size1+size2];
        for (int i = 0; i < size1; i++) {
            types[k]=td1.tdItemList.get(i).fieldType;
            names[k++]=td1.tdItemList.get(i).fieldName;
        }
        for (int i = 0; i < size2; i++) {
            types[k]=td2.tdItemList.get(i).fieldType;
            names[k++]=td2.tdItemList.get(i).fieldName;
        }
        return new TupleDesc(types,names);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        TupleDesc tupleDesc = (TupleDesc) o;
        return Objects.equals(tdItemList, tupleDesc.tdItemList);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(tdItemList);
    }

    /**
     * Returns a String describing this descriptor. It should be of the form
     * "fieldType[0](fieldName[0]), ..., fieldType[M](fieldName[M])", although
     * the exact format does not matter.
     *
     * @return String describing this descriptor.
     */
    public String toString() {
        // TODO: some code goes here
        StringBuilder tmp=new StringBuilder();
        for (TDItem tdItem : tdItemList) {
            tmp.append(tdItem.fieldType+"("+tdItem.fieldName+"),");
        }
        return tmp.delete(tmp.length()-1,tmp.length()).toString();
    }
}
