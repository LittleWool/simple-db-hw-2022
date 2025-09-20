package simpledb.execution;

import simpledb.common.DbException;
import simpledb.common.Type;
import simpledb.execution.Aggregator.Op;
import simpledb.storage.Tuple;
import simpledb.storage.TupleDesc;
import simpledb.transaction.TransactionAbortedException;

import java.util.NoSuchElementException;

/**
 * The Aggregation operator that computes an aggregate (e.g., sum, avg, max,
 * min). Note that we only support aggregates over a single column, grouped by a
 * single column.
 */
//使用具体的聚合器来进行聚合
public class Aggregate extends Operator {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor.
     * <p>
     * Implementation hint: depending on the type of afield, you will want to
     * construct an {@link IntegerAggregator} or {@link StringAggregator} to help
     * you with your implementation of readNext().
     *
     * @param child  The OpIterator that is feeding us tuples.
     * @param afield The column over which we are computing an aggregate.
     * @param gfield The column over which we are grouping the result, or -1 if
     *               there is no grouping
     * @param aop    The aggregation operator to use
     */
    //获取数据
    private OpIterator child;
    //聚合字段
    private int afield;
    //分组字段 -1则不分组
    private int gfield;
    //聚合的操作符
    private Aggregator.Op aop;
    //当前聚合器（字符串或者数字）
    private Aggregator curAggregator;

    private OpIterator opIterator;
    //MIN, MAX, SUM, AVG, COUNT只有这五种，String只有count
    public Aggregate(OpIterator child, int afield, int gfield, Aggregator.Op aop) {
        // TODO: some code goes here
        this.afield=afield;
        this.child=child;
        this.aop=aop;
        this.gfield=gfield;
    }
    private void doAggregate() throws DbException, TransactionAbortedException {
        if (child.getTupleDesc().getFieldType(aggregateField())==Type.STRING_TYPE){
            curAggregator=new StringAggregator(gfield,gfield==-1?null:child.getTupleDesc().getFieldType(gfield),afield,aop);
        }else {
            curAggregator=new IntegerAggregator(gfield,gfield==-1?null:child.getTupleDesc().getFieldType(gfield),afield,aop);
        }
        while (child.hasNext()){
            curAggregator.mergeTupleIntoGroup(child.next());
        }
        this.opIterator=curAggregator.iterator();
    }
    /**
     * @return If this aggregate is accompanied by a groupby, return the groupby
     *         field index in the <b>INPUT</b> tuples. If not, return
     *         {@link Aggregator#NO_GROUPING}
     */
    public int groupField() {
        // TODO: some code goes here
        return gfield;
    }

    /**
     * @return If this aggregate is accompanied by a group by, return the name
     *         of the groupby field in the <b>OUTPUT</b> tuples. If not, return
     *         null;
     */
    public String groupFieldName() {
        // TODO: some code goes here
        return child.getTupleDesc().getFieldName(gfield);
    }

    /**
     * @return the aggregate field
     */
    public int aggregateField() {
        // TODO: some code goes here
        return afield;
    }

    /**
     * @return return the name of the aggregate field in the <b>OUTPUT</b>
     *         tuples
     */
    public String aggregateFieldName() {
        // TODO: some code goes here
        return child.getTupleDesc().getFieldName(afield);
    }

    /**
     * @return return the aggregate operator
     */
    public Aggregator.Op aggregateOp() {
        // TODO: some code goes here
        return aop;
    }

    public static String nameOfAggregatorOp(Aggregator.Op aop) {
        return aop.toString();
    }

    public void open() throws NoSuchElementException, DbException,
            TransactionAbortedException {
        // TODO: some code goes here
        super.open();
        child.open();
        doAggregate();
        opIterator.open();
    }

    /**
     * Returns the next tuple. If there is a group by field, then the first
     * field is the field by which we are grouping, and the second field is the
     * result of computing the aggregate. If there is no group by field, then
     * the result tuple should contain one field representing the result of the
     * aggregate. Should return null if there are no more tuples.
     */


    protected Tuple fetchNext() throws TransactionAbortedException, DbException {
        // TODO: some code goes here
        while (opIterator.hasNext()){
            return opIterator.next();
        }
        return null;
    }


    public void rewind() throws DbException, TransactionAbortedException {
        // TODO: some code goes here
        child.rewind();
        opIterator.rewind();
    }

    /**
     * Returns the TupleDesc of this Aggregate. If there is no group by field,
     * this will have one field - the aggregate column. If there is a group by
     * field, the first field will be the group by field, and the second will be
     * the aggregate value column.
     * <p>
     * The name of an aggregate column should be informative. For example:
     * "aggName(aop) (child_td.getFieldName(afield))" where aop and afield are
     * given in the constructor, and child_td is the TupleDesc of the child
     * iterator.
     */
    public TupleDesc getTupleDesc() {
        // TODO: some code goes here

        String[] names=new String[]{groupFieldName(),aggregateFieldName()};
        Type[] types=new Type[]{child.getTupleDesc().getFieldType(gfield),child.getTupleDesc().getFieldType(afield)};
        return new TupleDesc(types,names);
    }

    public void close() {
        // TODO: some code goes here
        super.close();
        child.close();
        opIterator.close();
    }

    @Override
    public OpIterator[] getChildren() {
        // TODO: some code goes here
        return new OpIterator[]{child};
    }

    @Override
    public void setChildren(OpIterator[] children) {
        // TODO: some code goes here
        child=children[0];
    }

}
