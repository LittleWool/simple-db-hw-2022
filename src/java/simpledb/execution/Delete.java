package simpledb.execution;

import simpledb.common.Database;
import simpledb.common.DbException;
import simpledb.common.Type;
import simpledb.storage.BufferPool;
import simpledb.storage.IntField;
import simpledb.storage.Tuple;
import simpledb.storage.TupleDesc;
import simpledb.transaction.TransactionAbortedException;
import simpledb.transaction.TransactionId;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * The delete operator. Delete reads tuples from its child operator and removes
 * them from the table they belong to.
 */
public class Delete extends Operator {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor specifying the transaction that this delete belongs to as
     * well as the child to read from.
     *
     * @param t     The transaction this delete runs in
     * @param child The child operator from which to read tuples for deletion
     */
    TransactionId t;
    OpIterator child;
    int count=0;
    TupleDesc resTupleDesc;

    List<Tuple> resTuples;
    Iterator<Tuple> iterator;
    public Delete(TransactionId t, OpIterator child) {
        // TODO: some code goes here
        // TODO: some code goes here
        this.t=t;
        this.child=child;
        resTupleDesc=new TupleDesc(new Type[]{Type.INT_TYPE});
        resTuples=new LinkedList<>();
    }

    public TupleDesc getTupleDesc() {
        // TODO: some code goes here
        return resTupleDesc;
    }

    public void open() throws DbException, TransactionAbortedException {
        // TODO: some code goes here
        super.open();
        child.open();
        while (child.hasNext()){
            Tuple tmp=child.next();
            count++;
            try {
                Database.getBufferPool().deleteTuple(t,tmp);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        Tuple tuple = new Tuple(resTupleDesc);
        tuple.setField(0,new IntField(count));
        resTuples.add(tuple);
        this.iterator=resTuples.iterator();
    }

    public void close() {
        // TODO: some code goes here
        super.close();
        child.close();
    }

    public void rewind() throws DbException, TransactionAbortedException {
        // TODO: some code goes here
        child.rewind();
        iterator=resTuples.iterator();
        count=0;
    }

    /**
     * Deletes tuples as they are read from the child operator. Deletes are
     * processed via the buffer pool (which can be accessed via the
     * Database.getBufferPool() method.
     *
     * @return A 1-field tuple containing the number of deleted records.
     * @see Database#getBufferPool
     * @see BufferPool#deleteTuple
     */
    protected Tuple fetchNext() throws TransactionAbortedException, DbException {
        // TODO: some code goes here
        if (iterator!=null&&iterator.hasNext()){
            return iterator.next();
        }
        return null;
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
