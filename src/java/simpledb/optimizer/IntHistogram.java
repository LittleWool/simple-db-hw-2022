package simpledb.optimizer;

import simpledb.execution.Predicate;

/**
 * A class to represent a fixed-width histogram over a single integer-based field.
 */
public class IntHistogram {

    /**
     * Create a new IntHistogram.
     * <p>
     * This IntHistogram should maintain a histogram of integer values that it receives.
     * It should split the histogram into "buckets" buckets.
     * <p>
     * The values that are being histogrammed will be provided one-at-a-time through the "addValue()" function.
     * <p>
     * Your implementation should use space and have execution time that are both
     * constant with respect to the number of values being histogrammed.  For example, you shouldn't
     * simply store every value that you see in a sorted list.
     *
     * @param buckets The number of buckets to split the input value into.
     * @param min     The minimum integer value that will ever be passed to this class for histogramming
     * @param max     The maximum integer value that will ever be passed to this class for histogramming
     */
    //横坐标个数
    int buckets;
    int min;
    int max;
    //横坐标之间的跨度
    int span;
    int[] count;
    int total=0;

    int[] preSum;
    int[] suffixSum;
    boolean isDirty;
    public IntHistogram(int buckets, int min, int max) {
        // TODO: some code goes here
        isDirty=true;
        this.buckets = buckets;
        this.min = min;
        this.max = max;
        //最小间隔数为1，之前搞了个0没过去
        span = Math.max(1,(int)Math.ceil((double)(max - min + 1) / buckets));
        if(this.span == 1) this.buckets = max - min + 1;
        else this.buckets = buckets;
        count = new int[buckets];
    }

    private int bucketIndex(int v){
        // 添加边界检查，防止数组越界
        if (v < min) return 0;
        if (v > max) return buckets - 1;
        int index = (v - min) / span;
        return Math.max(0, Math.min(index, buckets - 1));
    }
    /**
     * Add a value to the set of values that you are keeping a histogram of.
     *
     * @param v Value to add to the histogram
     */
    public void addValue(int v) {
        // TODO: some code goes here
        count[bucketIndex(v)]++;
        total++;
        isDirty=true;
    }


    /**
     * Estimate the selectivity of a particular predicate and operand on this table.
     * <p>
     * For example, if "op" is "GREATER_THAN" and "v" is 5,
     * return your estimate of the fraction of elements that are greater than 5.
     *
     * @param op Operator
     * @param v  Value
     * @return Predicted selectivity of this particular operator and value
     */
    public double estimateSelectivity(Predicate.Op op, int v) {
        // TODO: some code goes here
        if (total==0){
            return 0.0;
        }
        updateCache();
        //处理边界情况
        if (v < min) {
            switch (op) {
                case GREATER_THAN :;
                case NOT_EQUALS:;
                case GREATER_THAN_OR_EQ:return 1.0;
                case EQUALS:;
                case LESS_THAN:;
                case LESS_THAN_OR_EQ:return 0.0;
            }
        }
        if (v > max) {
            switch (op) {
                case GREATER_THAN :;
                case EQUALS:;
                case GREATER_THAN_OR_EQ:return 0.0;
                case NOT_EQUALS:;
                case LESS_THAN:;
                case LESS_THAN_OR_EQ:return 1.0;
            }
        }

        //桶索引
        int bIndex = bucketIndex(v);
        //桶内偏移量
        int offset=(v - min) %span;
        double res=-1.0;

        switch (op){
            case EQUALS:{
                res=(1.0/span)*count[bIndex]/total;
                break;
            }
            case NOT_EQUALS:{
                res=1.0-estimateSelectivity(Predicate.Op.EQUALS,v);
                break;
            }
            case GREATER_THAN :{
                //这个值大于v,就是大于等于v+1
                res=(suffixSum[bIndex]+(1.0*(span-(offset+1))/span)*count[bIndex]);
                res=res/total;
                break;
            }
            case GREATER_THAN_OR_EQ:{
                res=(suffixSum[bIndex]+(1.0*(span-(offset))/span)*count[bIndex]);
                res=res/total;
                break;
            }
            case LESS_THAN:{
                res=preSum[bIndex]+(1.0*(Math.max(0,offset-1))/span)*count[bIndex];
                res=res/total;
                break;
            }
            case LESS_THAN_OR_EQ:{
                res=preSum[bIndex]+(1.0*(offset)/span)*count[bIndex];
                res=res/total;
                break;
            }
        }

        return res;
    }

    private void updateCache() {
        if (!isDirty||total==0){
            return;
        }
        //preSum[i]前i-1个桶的元组综合
        preSum=new int[buckets];
        suffixSum =new int[buckets];
        for (int i = 1; i < buckets; i++) {
            preSum[i]=preSum[i-1]+count[i-1];
        }
        for (int i = buckets - 2; i >= 0; i--) {
            suffixSum[i]= suffixSum[i+1]+count[i+1];
        }
        isDirty=false;
    }

    /**
     * @return the average selectivity of this histogram.
     * <p>
     * This is not an indispensable method to implement the basic
     * join optimization. It may be needed if you want to
     * implement a more efficient optimization
     */
    public double avgSelectivity() {
        // 如果没有数据，返回默认值
//        if (total == 0) {
//            return 0.1; // 或者其他合理的默认值
//        }
//
//        // 方法1: 基于不同值的估计
//        // 假设每个桶内的值是均匀分布的，计算平均选择性
//        double sum = 0.0;
//        int nonEmptyBuckets = 0;
//
//        for (int i = 0; i < buckets; i++) {
//            if (count[i] > 0) {
//                nonEmptyBuckets++;
//                // 对于每个非空桶，等值查询的平均选择性约为 1/桶内不同值的数量
//                // 简化估计：假设桶内不同值的数量与桶的宽度成比例
//                double bucketWidth = span;
//                if (i == buckets - 1) {
//                    // 最后一个桶的宽度可能不同
//                    bucketWidth = max - (i * span + min) + 1;
//                }
//
//                if (bucketWidth > 0) {
//                    // 每个值的平均选择性
//                    double avgSelPerValue = 1.0 / bucketWidth;
//                    // 桶的总选择性
//                    double bucketSelectivity = avgSelPerValue * count[i];
//                    sum += bucketSelectivity;
//                }
//            }
//        }
//
//        // 如果所有桶都为空，返回一个很小的默认值
//        if (nonEmptyBuckets == 0) {
//            return 0.1;
//        }
//
//        // 返回平均选择性
//        return sum / total;
        // skip 大家都不xie....
        return 1.0;
    }

    /**
     * @return A string describing this histogram, for debugging purposes
     */
    public String toString() {
        // TODO: some code goes here
        StringBuilder tmp=new StringBuilder();
        for (int i = 0; i < buckets; i++) {
            tmp.append(i * span + min + "-" + (i + 1) * span + ":" + count[i]+"\n");
        }
        return tmp.toString();
    }
}
