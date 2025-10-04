package simpledb.execution;

import simpledb.common.Type;
import simpledb.storage.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Knows how to compute some aggregate over a set of IntFields.
 */
public class IntegerAggregator implements Aggregator {

    private static final long serialVersionUID = 1L;

    /**
     * Aggregate constructor
     *
     * @param gbfield     the 0-based index of the group-by field in the tuple, or
     * NO_GROUPING if there is no grouping
     * @param gbfieldtype the type of the group by field (e.g., Type.INT_TYPE), or null
     * if there is no grouping
     * @param afield      the 0-based index of the aggregate field in the tuple
     * @param what        the aggregation operator
     */

    // 分组字段，int NO_GROUPING = -1;
    private final int gbField;

    // 分组字段类型，若是null则是NO_GROUPING
    private final Type gbFieldtype;

    // 聚集字段
    private final int afield;

    // 聚集符号
    private final Op op;

    // 元组描述符
    private TupleDesc tupleDesc;
    //返回的结果描述符
    private TupleDesc resTupleDesc;

    // 聚合信息映射
    private final Map<Field, AggInfo> aggInfoMap;

    private static final Field DEFAULT_FIELD = new IntField(-1);

    public IntegerAggregator(int gbfield, Type gbfieldtype, int afield, Op what) {
        // TODO: some code goes here
        this.gbField = gbfield;
        this.gbFieldtype = gbfieldtype;
        this.afield = afield;
        this.op = what;
        aggInfoMap = new HashMap<>();
    }

    /**
     * Merge a new tuple into the aggregate, grouping as indicated in the
     * constructor
     *
     * @param tup the Tuple containing an aggregate field and a group-by field
     */
    //tup只有聚合字段和分组字段
    public void mergeTupleIntoGroup(Tuple tup) {
        // TODO: some code goes here
        if (tupleDesc == null) {
            tupleDesc = tup.getTupleDesc();
            resTupleDesc = buildTupleDesc(tupleDesc);
        }
        Field gField = gbField == NO_GROUPING ? DEFAULT_FIELD : tup.getField(gbField);
        Field tmp = tup.getField(afield);
        if (tmp instanceof IntField){
            IntField aField=(IntField) tmp;
            doAggregate1(gField, aField);
        }else{
            throw new IllegalArgumentException("The aggregate field type is non-integer");
        }

    }

    private void doAggregate1(Field gField, IntField aField) {
        AggInfo aggInfo = aggInfoMap.get(gField);
        if (aggInfo == null) {
            aggInfo = new AggInfo();
            aggInfoMap.put(gField, aggInfo);
        }
        switch (op) {
            case MIN:
                aggInfo.min=Math.min(aggInfo.min,aField.getValue());
                break;
            case MAX:
                aggInfo.max=Math.max(aggInfo.max,aField.getValue());
                break;
            case AVG:
                aggInfo.sum+=aField.getValue();
                aggInfo.count++;
                break;
            case SUM:
                aggInfo.sum+=aField.getValue();
                break;
            case COUNT:
                aggInfo.count++;
                break;
        }

    }

    private TupleDesc buildTupleDesc(TupleDesc tupleDesc) {
        if (gbFieldtype == null) {
            // 无分组情况
            return new TupleDesc(
                    new Type[]{Type.INT_TYPE},
                    new String[]{tupleDesc.getFieldName(afield)}
            );
        } else {
            // 有分组情况
            Type[] types = new Type[]{gbFieldtype, Type.INT_TYPE};
            String[] names = new String[]{
                    tupleDesc.getFieldName(gbField),
                    tupleDesc.getFieldName(afield)
            };
            return new TupleDesc(types, names);
        }
    }


    /**
     * Create a OpIterator over group aggregate results.
     *
     * @return a OpIterator whose tuples are the pair (groupVal, aggregateVal)
     * if using group, or a single (aggregateVal) if no grouping. The
     * aggregateVal is determined by the type of aggregate specified in
     * the constructor.
     */
    //大概是获得分过组之后的迭代器
    public OpIterator iterator() {
        // TODO: some code goes here
        //throw new UnsupportedOperationException("please implement me for lab2");
        List<Tuple> tuples=new ArrayList<>();
        for (Map.Entry<Field, AggInfo> entry : aggInfoMap.entrySet()) {
            Tuple tmp=new Tuple(resTupleDesc);
            int index=0;
            // 如果有分组字段，设置分组值
            if (gbFieldtype != null) {
                tmp.setField(index++, entry.getKey());
            }
            int value=0;
            switch (op){
                case MIN:
                    value=entry.getValue().min;
                    break;
                case MAX:
                    value=entry.getValue().max;
                    break;
                case AVG:
                    value=entry.getValue().sum/entry.getValue().count;
                    break;
                case SUM:
                    value=entry.getValue().sum;
                    break;
                case COUNT:
                    value=entry.getValue().count;
                    break;
            }
            int fieldIndex = (gbFieldtype != null) ? 1 : 0;
            tmp.setField(fieldIndex, new IntField(value));

            tuples.add(tmp);
        }

        return new TupleIterator(resTupleDesc,tuples);
    }

}
