package simpledb.storage;

import java.io.Serializable;
import java.util.Objects;

/**
 * A RecordId is a reference to a specific tuple on a specific page of a
 * specific table.
 * 在物理即页的层面定位一条记录
 * 主键是在一个确定的表内定位一条记录
 */
public class RecordId implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a new RecordId referring to the specified PageId and tuple
     * number.

     */
    PageId pageId;
    int tupleNo;
    public RecordId(PageId pid, int tupleno) {
        // TODO: some code goes here
        this.pageId=pid;
        this.tupleNo=tupleno;
    }

    /**
     * @return the tuple number this RecordId references.
     */
    public int getTupleNumber() {
        // TODO: some code goes here
        return tupleNo;
    }

    /**
     * @return the page id this RecordId references.
     */
    public PageId getPageId() {
        // TODO: some code goes here
        return pageId;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        RecordId recordId = (RecordId) o;
        return tupleNo == recordId.tupleNo && Objects.equals(pageId, recordId.pageId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pageId, tupleNo);
    }
}
