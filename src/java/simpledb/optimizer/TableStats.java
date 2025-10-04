package simpledb.optimizer;

import simpledb.common.Database;
import simpledb.common.DbException;
import simpledb.common.Type;
import simpledb.execution.Predicate;
import simpledb.storage.*;
import simpledb.transaction.TransactionAbortedException;
import simpledb.transaction.TransactionId;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * TableStats represents statistics (e.g., histograms) about base tables in a
 * query.
 * <p>
 * This class is not needed in implementing lab1 and lab2.
 */
public class TableStats {

    //为每个表创建一个TableStats
    private static final ConcurrentMap<String, TableStats> statsMap = new ConcurrentHashMap<>();

    static final int IOCOSTPERPAGE = 1000;

    public static TableStats getTableStats(String tablename) {
        return statsMap.get(tablename);
    }

    public static void setTableStats(String tablename, TableStats stats) {
        statsMap.put(tablename, stats);
    }

    public static void setStatsMap(Map<String, TableStats> s) {
        try {
            java.lang.reflect.Field statsMapF = TableStats.class.getDeclaredField("statsMap");
            statsMapF.setAccessible(true);
            statsMapF.set(null, s);
        } catch (NoSuchFieldException | IllegalAccessException | IllegalArgumentException | SecurityException e) {
            e.printStackTrace();
        }

    }

    public static Map<String, TableStats> getStatsMap() {
        return statsMap;
    }

    public static void computeStatistics() {
        Iterator<Integer> tableIt = Database.getCatalog().tableIdIterator();

        System.out.println("Computing table stats.");
        while (tableIt.hasNext()) {
            int tableid = tableIt.next();
            TableStats s = new TableStats(tableid, IOCOSTPERPAGE);
            setTableStats(Database.getCatalog().getTableName(tableid), s);
        }
        System.out.println("Done.");
    }

    /**
     * Number of bins for the histogram. Feel free to increase this value over
     * 100, though our tests assume that you have at least 100 bins in your
     * histograms.
     */
    static final int NUM_HIST_BINS = 100;
    private int tableid;
    private int ioCostPerPage;
    private TupleDesc td;
    private int tupleCount=0;
    /**
     * Create a new TableStats object, that keeps track of statistics on each
     * column of a table
     *
     * @param tableid       The table over which to compute statistics
     * @param ioCostPerPage The cost per page of IO. This doesn't differentiate between
     *                      sequential-scan IO and disk seeks.
     */
    public TableStats(int tableid, int ioCostPerPage) {
        // For this function, you'll have to get the
        // DbFile for the table in question,
        // then scan through its tuples and calculate
        // the values that you need.
        // You should try to do this reasonably efficiently, but you don't
        // necessarily have to (for example) do everything
        // in a single scan of the table.
        // TODO: some code goes here
        this.tableid = tableid;
        this.ioCostPerPage = ioCostPerPage;
        DbFile dbFile = (HeapFile) Database.getCatalog().getDatabaseFile(tableid);
        this.td= dbFile.getTupleDesc();
        int n = td.numFields();
        max=new int[n];
        min=new int[n];

        Arrays.fill(max,Integer.MIN_VALUE);
        Arrays.fill(min,Integer.MAX_VALUE);
        DbFileIterator dbFileIterator = dbFile.iterator(new TransactionId());
        each=new ArrayList<>();

        try {
            dbFileIterator.open();
            int i=0;
            while (dbFileIterator.hasNext()) {
                tupleCount++;
                Tuple next = dbFileIterator.next();
                for (int j = 0; j < n; j++) {
                    max[j]=Math.max(max[j],((IntField)next.getField(j)).getValue());
                    min[j]=Math.min(min[j],((IntField)next.getField(j)).getValue());
                }
                i++;
            }
            for (int j = 0; j < n; j++) {
                if (td.getFieldType(j)== Type.INT_TYPE){
                    each.add(new IntHistogram(10,min[j],max[j]));
                }else{
                    each.add(new StringHistogram(10));
                }
            }
            dbFileIterator.rewind();
            while (dbFileIterator.hasNext()) {
                Tuple next = dbFileIterator.next();
                for (int j = 0; j < n; j++) {
                    if (td.getFieldType(j)==Type.INT_TYPE){
                        ((IntHistogram)each.get(j)).addValue(((IntField)next.getField(j)).getValue());
                    }else {
                        ((StringHistogram)each.get(j)).addValue(((StringField)next.getField(j)).getValue());
                    }
                }
            }
            this.scanCost= ((HeapFile)dbFile).numPages()*ioCostPerPage;
        } catch (TransactionAbortedException e) {

        } catch (DbException e) {
            throw new RuntimeException(e);
        }finally {
            dbFileIterator.close();
        }
    }

    /**
     * Estimates the cost of sequentially scanning the file, given that the cost
     * to read a page is costPerPageIO. You can assume that there are no seeks
     * and that no pages are in the buffer pool.
     * <p>
     * Also, assume that your hard drive can only read entire pages at once, so
     * if the last page of the table only has one tuple on it, it's just as
     * expensive to read as a full page. (Most real hard drives can't
     * efficiently address regions smaller than a page at a time.)
     *
     * @return The estimated cost of scanning the table.
     */
    int[] max;
    int[] min;
    private ArrayList<? super Object> each;

    int scanCost;
    public double estimateScanCost() {
        return scanCost;
    }

    /**
     * This method returns the number of tuples in the relation, given that a
     * predicate with selectivity selectivityFactor is applied.
     *
     * @param selectivityFactor The selectivity of any predicates over the table
     * @return The estimated cardinality of the scan with the specified
     *         selectivityFactor
     */
    public int estimateTableCardinality(double selectivityFactor) {
        // TODO: some code goes here
        return (int)(tupleCount*selectivityFactor);
    }

    /**
     * The average selectivity of the field under op.
     *
     * @param field the index of the field
     * @param op    the operator in the predicate
     *              The semantic of the method is that, given the table, and then given a
     *              tuple, of which we do not know the value of the field, return the
     *              expected selectivity. You may estimate this value from the histograms.
     */
    public double avgSelectivity(int field, Predicate.Op op) {
        // TODO: some code goes here
        return 1.0;
    }

    /**
     * Estimate the selectivity of predicate <tt>field op constant</tt> on the
     * table.
     *
     * @param field    The field over which the predicate ranges
     * @param op       The logical operation in the predicate
     * @param constant The value against which the field is compared
     * @return The estimated selectivity (fraction of tuples that satisfy) the
     *         predicate
     */
    public double estimateSelectivity(int field, Predicate.Op op, Field constant) {
        // TODO: some code goes here
        if (constant.getType()==Type.INT_TYPE){
            return ((IntHistogram)each.get(field)).estimateSelectivity(op,((IntField)constant).getValue());
        }else{
            return ((StringHistogram)each.get(field)).estimateSelectivity(op,((StringField)constant).getValue());
        }

    }

    /**
     * return the total number of tuples in this table
     */
    public int totalTuples() {
        // TODO: some code goes here
        return tupleCount;
    }

}
