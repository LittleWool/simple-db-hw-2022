package simpledb.storage;

import simpledb.SimpleDb;
import simpledb.common.Database;
import simpledb.common.DbException;
import simpledb.common.Debug;
import simpledb.common.Permissions;
import simpledb.transaction.TransactionAbortedException;
import simpledb.transaction.TransactionId;

import java.beans.beancontext.BeanContextMembershipEvent;
import java.io.*;
import java.nio.channels.MulticastChannel;
import java.rmi.UnexpectedException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * HeapFile is an implementation of a DbFile that stores a collection of tuples
 * in no particular order. Tuples are stored on pages, each of which is a fixed
 * size, and the file is simply a collection of those pages. HeapFile works
 * closely with HeapPage. The format of HeapPages is described in the HeapPage
 * constructor.
 *
 * @author Sam Madden
 * @see HeapPage#HeapPage
 */


public class HeapFile implements DbFile {

    /**
     * Constructs a heap file backed by the specified file.
     *
     * @param f the file that stores the on-disk backing store for this heap
     *          file.
     */
    //表对应的物理文件
    File file;
    //表的模式
    TupleDesc tupleDesc;


    public HeapFile(File f, TupleDesc td) {
        // TODO: some code goes here
        this.file=f;
        this.tupleDesc=td;
    }

    /**
     * Returns the File backing this HeapFile on disk.
     *
     * @return the File backing this HeapFile on disk.
     */
    public File getFile() {
        // TODO: some code goes here
        return file;
    }

    /**
     * Returns an ID uniquely identifying this HeapFile. Implementation note:
     * you will need to generate this tableid somewhere to ensure that each
     * HeapFile has a "unique id," and that you always return the same value for
     * a particular HeapFile. We suggest hashing the absolute file name of the
     * file underlying the heapfile, i.e. f.getAbsoluteFile().hashCode().
     *
     * @return an ID uniquely identifying this HeapFile.
     */
    //获取一个表唯一id,因为一个heap file就对应一张表,这里用文件名hash值生成唯一id
    public int getId() {
        // TODO: some code goes here
        //throw new UnsupportedOperationException("implement this");
        return file.getName().hashCode();
    }

    /**
     * Returns the TupleDesc of the table stored in this DbFile.
     *
     * @return TupleDesc of this DbFile.
     */
    public TupleDesc getTupleDesc() {
        // TODO: some code goes here
        //throw new UnsupportedOperationException("implement this");
        return tupleDesc;
    }

    // see DbFile.java for javadocs
    /**
     * 读某一个页面进入bf
     * @param pid
     * @return
     */
    public Page readPage(PageId pid) {
        // TODO: some code goes here
        if (pid.getPageNumber()>numPages()){
            return null;
        }
        try(RandomAccessFile raf=new RandomAccessFile(file,"r")){
            int offset=pid.getPageNumber()*BufferPool.getPageSize();
            if (offset>=raf.length()){
                throw new IllegalArgumentException("Page offset exceeds file length");
            }
            raf.seek(offset);
            byte[]  bytes=new byte[BufferPool.getPageSize()];
            raf.read(bytes,0,BufferPool.getPageSize());
            return new HeapPage(new HeapPageId(pid.getTableId(),pid.getPageNumber()),bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    // see DbFile.java for javadocs
    public void writePage(Page page) throws IOException {
        // TODO: some code goes here
        // not necessary for lab1
    }

    /**
     * Returns the number of pages in this HeapFile.
     */
    public int numPages() {
        // TODO: some code goes here

        return (int) Math.ceil(1.0*file.length()/(BufferPool.getPageSize()));
    }

    // see DbFile.java for javadocs
    public List<Page> insertTuple(TransactionId tid, Tuple t)
            throws DbException, IOException, TransactionAbortedException {
        // TODO: some code goes here
        return null;
        // not necessary for lab1
    }

    // see DbFile.java for javadocs
    public List<Page> deleteTuple(TransactionId tid, Tuple t) throws DbException,
            TransactionAbortedException {
        // TODO: some code goes here
        return null;
        // not necessary for lab1
    }


    /**
     * Returns an iterator over all the tuples stored in this DbFile. The
     * iterator must use {@link BufferPool#getPage}, rather than
     * {@link #readPage} to iterate through the pages.
     *获取所有元组的迭代器，使用bf的getPage
     *
     * @return an iterator over all the tuples stored in this DbFile.
     */
    // see DbFile.java for javadocs
    public DbFileIterator iterator(TransactionId tid) {
        // TODO: some code goes here
        //暂时lab1用不着tid貌似

        return new HeapPageItr(tid);
    }

    private class HeapPageItr  implements DbFileIterator{
        //当前页码
        int pageNo=0;// index of next element to return
        //表id
        int taleId=getId();
        //迭代器状态
        boolean isOpen=false;

        Iterator<Tuple> iterator=null;

        TransactionId tid;

        // prevent creating a synthetic constructor
        HeapPageItr(TransactionId tid) {
             this.tid=tid;
            this.taleId=getId();
        }

        //读取新的页面，更换新的页面的迭代器
        private void loadPage()throws DbException, TransactionAbortedException{
            if (pageNo<numPages()){
                Page page = Database.getBufferPool().getPage(tid, new HeapPageId(taleId, pageNo++), null);
                if (page instanceof HeapPage) {
                    //this.heapPage = (HeapPage) page;
                    this.iterator=((HeapPage)page).iterator();
                } else {
                    throw new DbException("Expected HeapPage but got " + page.getClass().getSimpleName());
                }
            }else{
                iterator=null;
            }


        }
        @Override
        public void open() throws DbException, TransactionAbortedException {
            isOpen=true;
            if (iterator==null){
                loadPage();
            }
        }


        @Override
        public boolean hasNext() throws DbException, TransactionAbortedException {
            if (!isOpen||iterator==null){
                return false;
            }
            if (iterator.hasNext()){
                //当前页面仍旧可用
                return true;
            }else{
                //更换新的页面
                loadPage();
                if (iterator==null){
                    return false;
                }else{
                    return iterator.hasNext();
                }

            }
        }

        @Override
        public Tuple next() throws DbException, TransactionAbortedException, NoSuchElementException {
            if (!isOpen){
                throw new NoSuchElementException();
            }
            Tuple res=null;
            if (hasNext()){
                res=iterator.next();
            }
            return res;
        }

        //指针重置为开始状态
        @Override
        public void rewind() throws DbException, TransactionAbortedException {
            if (!isOpen){
                throw new NoSuchElementException();
            }
            pageNo=0;
            loadPage();
        }

        @Override
        public void close() {
            isOpen=false;
           // iterator=null;
        }
    }


}

