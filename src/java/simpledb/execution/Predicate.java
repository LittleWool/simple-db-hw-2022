package simpledb.execution;

import simpledb.storage.Field;
import simpledb.storage.Tuple;

import java.io.Serializable;

/**
 * Predicate compares tuples to a specified Field value.
 * 使用谓词对某个字段的值进行比较
 */
public class Predicate implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Constants used for return codes in Field.compare
     */
    public enum Op implements Serializable {
        EQUALS, GREATER_THAN, LESS_THAN, LESS_THAN_OR_EQ, GREATER_THAN_OR_EQ, LIKE, NOT_EQUALS;

        /**
         * Interface to access operations by integer value for command-line
         * convenience.
         *
         * @param i a valid integer Op index
         */
        public static Op getOp(int i) {
            return values()[i];
        }

        public String toString() {
            if (this == EQUALS)
                return "=";
            if (this == GREATER_THAN)
                return ">";
            if (this == LESS_THAN)
                return "<";
            if (this == LESS_THAN_OR_EQ)
                return "<=";
            if (this == GREATER_THAN_OR_EQ)
                return ">=";
            if (this == LIKE)
                return "LIKE";
            if (this == NOT_EQUALS)
                return "<>";
            throw new IllegalStateException("impossible to reach here");
        }

    }

    //比较的字段索引
    int fieldIndex;
    //用于比较的操作符
    Op op;
    //操作数
    Field operand;
    /**
     * Constructor.
     *
     * @param field   field number of passed in tuples to compare against.
     * @param op      operation to use for comparison
     * @param operand field value to compare passed in tuples to
     */
    public Predicate(int field, Op op, Field operand) {
        // TODO: some code goes here
        this.fieldIndex =field;
        this.op=op;
        this.operand =operand;
    }

    /**
     * @return the field number
     */
    public int getField() {
        // TODO: some code goes here
        return fieldIndex;
    }

    /**
     * @return the operator
     */
    public Op getOp() {
        // TODO: some code goes here
        return op;
    }

    /**
     * @return the operand
     */
    public Field getOperand() {
        // TODO: some code goes here
        return operand;
    }

    /**
     * Compares the field number of t specified in the constructor to the
     * operand field specified in the constructor using the operator specific in
     * the constructor. The comparison can be made through Field's compare
     * method.
     *
     * @param t The tuple to compare against
     * @return true if the comparison is true, false otherwise.
     */
    public boolean filter(Tuple t) {
        // TODO: some code goes here
        if (t.getField(this.fieldIndex).getType()!=this.operand.getType()){
            return false;
        }
        return t.getField(this.fieldIndex).compare(this.op,this.operand);
    }

    /**
     * Returns something useful, like "f = field_id op = op_string operand =
     * operand_string"
     */
    public String toString() {
        // TODO: some code goes here
        return "f = "+this.fieldIndex+"\top = "+op.toString()+"\toperand = "+operand;
    }
}
