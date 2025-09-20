package simpledb.execution;

import simpledb.common.DbException;
import simpledb.common.Type;
import simpledb.storage.*;
import simpledb.transaction.TransactionAbortedException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Knows how to compute some aggregate over a set of StringFields.
 */
public class StringAggregator implements Aggregator {

    private static final long serialVersionUID = 1L;

    // 分组字段，int NO_GROUPING = -1;
    private final int gbField;

    // 分组字段类型，若是null则是NO_GROUPING
    private final Type gbFieldtype;

    // 聚集字段
    private final int afield;

    // 聚集符号
    private final Op op;

    // 元组描述符
    private  TupleDesc tupleDesc;
    //返回的结果描述符
    private  TupleDesc resTupleDesc;

    // 聚合信息映射
    private final Map<Field, AggInfo> aggInfoMap;

    private static final Field DEFAULT_FIELD = new IntField(-1);

    /**
     * Aggregate constructor
     *
     * @param gbfield     the 0-based index of the group-by field in the tuple, or NO_GROUPING if there is no grouping
     * @param gbfieldtype the type of the group by field (e.g., Type.INT_TYPE), or null if there is no grouping
     * @param afield      the 0-based index of the aggregate field in the tuple
     * @param what        aggregation operator to use -- only supports COUNT
     * @throws IllegalArgumentException if what != COUNT
     */
    public StringAggregator(int gbfield, Type gbfieldtype, int afield, Op what) {
        this.gbField = gbfield;
        this.gbFieldtype = gbfieldtype;
        this.afield = afield;
        this.op = what;
        this.aggInfoMap = new HashMap<>();

        // String类型只支持COUNT操作
        if (what != Op.COUNT) {
            throw new IllegalArgumentException("StringAggregator only supports COUNT operation");
        }
    }

    /**
     * Merge a new tuple into the aggregate, grouping as indicated in the constructor
     *
     * @param tup the Tuple containing an aggregate field and a group-by field
     */
    public void mergeTupleIntoGroup(Tuple tup) {
        if (resTupleDesc == null) {
            tupleDesc = tup.getTupleDesc();
            resTupleDesc = buildTupleDesc(tupleDesc);
        }

        Field gField = gbField == NO_GROUPING ? DEFAULT_FIELD : tup.getField(gbField);
        Field aField = tup.getField(afield);

        doAggregate(gField, aField);
    }

    /**
     * 创建结果TupleDesc
     * @param tupleDesc 原始元组描述符
     * @return 结果元组描述符
     */
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
     * 执行聚合操作
     * @param gField 分组字段
     * @param aField 聚合字段
     */
    private void doAggregate(Field gField, Field aField) {
        aggInfoMap.computeIfAbsent(gField, k -> new AggInfo())
                .count++;
    }

    /**
     * Create a OpIterator over group aggregate results.
     *
     * @return a OpIterator whose tuples are the pair (groupVal,
     *         aggregateVal) if using group, or a single (aggregateVal) if no
     *         grouping. The aggregateVal is determined by the type of
     *         aggregate specified in the constructor.
     */
    public OpIterator iterator() {
        List<Tuple> tuples = new ArrayList<>();

        for (Map.Entry<Field, AggInfo> entry : aggInfoMap.entrySet()) {
            Tuple tuple = new Tuple(resTupleDesc);
            int index = 0;

            // 如果有分组字段，设置分组值
            if (gbFieldtype != null) {
                tuple.setField(index++, entry.getKey());
            }

            // 设置聚合值（COUNT结果）
            tuple.setField(index, new IntField(entry.getValue().count));
            tuples.add(tuple);
        }

        return new TupleIterator(resTupleDesc, tuples);
    }


}
