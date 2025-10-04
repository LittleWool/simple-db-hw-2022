package simpledb.storage;

import simpledb.common.Database;
import simpledb.common.DbException;
import simpledb.common.Permissions;
import simpledb.transaction.TransactionAbortedException;
import simpledb.transaction.TransactionId;

import java.io.*;
import java.util.*;

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
     * file.
     */
    //表对应的物理文件
    File file;
    //表的模式
    TupleDesc tupleDesc;

    HashSet<PageId> freeSet;

    public HeapFile(File f, TupleDesc td) {
        // TODO: some code goes here
        this.file = f;
        this.tupleDesc = td;

        freeSet = new HashSet<>();
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
     *
     * @param pid
     * @return
     */
    public Page readPage(PageId pid) {
        // TODO: some code goes here
        if (pid.getPageNumber() >= numPages()) {
            return null;
        }
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            int offset = pid.getPageNumber() * BufferPool.getPageSize();
            if (offset >= raf.length()) {
                throw new IllegalArgumentException("Page offset exceeds file length");
            }
            raf.seek(offset);
            byte[] bytes = new byte[BufferPool.getPageSize()];
            raf.read(bytes, 0, BufferPool.getPageSize());
            HeapPage heapPage = new HeapPage(new HeapPageId(pid.getTableId(), pid.getPageNumber()), bytes);
            if (!heapPage.freeSlots.isEmpty()) {
                freeSet.add(heapPage.pid);
            }
            return heapPage;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    // see DbFile.java for javadocs
    public void writePage(Page page) throws IOException {

        PageId pid = page.getId();
        int pageNumber = pid.getPageNumber();
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {

            int requiredSize = (pageNumber + 1) * BufferPool.getPageSize();
            if (requiredSize > raf.length()) {
                raf.setLength(requiredSize);
            }
            long offset = (long) pageNumber * BufferPool.getPageSize();
            raf.seek(offset);
            raf.write(page.getPageData());

        } catch (IOException e) {
            throw new IOException("Failed to write page " + pageNumber + " to file " + file.getName(), e);
        }
    }

    /**
     * Returns the number of pages in this HeapFile.
     */
    public int numPages() {
        // TODO: some code goes here
        if (file.length() == 0) return 0;
        return ((int) file.length() - 1) / BufferPool.getPageSize() + 1;
    }

    // see DbFile.java for javadocs
    public List<Page> insertTuple(TransactionId tid, Tuple t)
            throws DbException, IOException, TransactionAbortedException {
        // TODO: some code goes here

        if (freeSet.isEmpty()) {
            HeapPage page = null;
            //寻找空闲页面
            for (int i = 0; i < numPages(); i++) {
                HeapPageId heapPageId = new HeapPageId(getId(), i);
                HeapPage tmp = (HeapPage) getPageFromBufferPool(tid, heapPageId, Permissions.READ_WRITE);
                if (!tmp.freeSlots.isEmpty()){
                    page = tmp;
                    break;
                }

            }
            //无空闲页就创建新页面,但先不刷盘
            if (page == null) {
                HeapPageId heapPageId = new HeapPageId(getId(), numPages());
                HeapPage heapPage = new HeapPage(heapPageId, HeapPage.createEmptyPageData());
                writePage(heapPage);
                freeSet.add(heapPageId);
            }
        }
        HeapPage page = (HeapPage) getPageFromBufferPool(tid, freeSet.iterator().next(), Permissions.READ_WRITE);


        page.insertTuple(t);
        if (page.freeSlots.isEmpty()) {
            freeSet.remove(page.getId());
        }
        return List.of(page);
    }

    private Page getPageFromBufferPool(TransactionId tid, PageId pid, Permissions perm) throws TransactionAbortedException, DbException {
        return Database.getBufferPool().getPage(tid, pid, perm);
    }

    // see DbFile.java for javadocs
    public List<Page> deleteTuple(TransactionId tid, Tuple t) throws DbException,
            TransactionAbortedException {
        // TODO: some code goes here
        RecordId recordId = t.recordId;
        HeapPage page = (HeapPage) Database.getBufferPool().getPage(tid, recordId.pageId, Permissions.READ_WRITE);
        page.deleteTuple(t);
        if (!page.freeSlots.isEmpty()){
            freeSet.add(page.getId());
        }
        List<Page> modifiedPage = new ArrayList<>();
        modifiedPage.add(page);
        return modifiedPage;
        // not necessary for lab1
    }


    /**
     * Returns an iterator over all the tuples stored in this DbFile. The
     * iterator must use {@link BufferPool#getPage}, rather than
     * {@link #readPage} to iterate through the pages.
     * 获取所有元组的迭代器，使用bf的getPage
     *
     * @return an iterator over all the tuples stored in this DbFile.
     */
    // see DbFile.java for javadocs
    public DbFileIterator iterator(TransactionId tid) {
        // TODO: some code goes here
        //暂时lab1用不着tid貌似

        return new HeapPageItr(tid);
    }

    private class HeapPageItr implements DbFileIterator {
        //当前页码
        int pageNo = 0;// index of next element to return
        //表id
        int taleId = getId();
        //迭代器状态
        boolean isOpen = false;

        Iterator<Tuple> iterator = null;

        TransactionId tid;

        // prevent creating a synthetic constructor
        HeapPageItr(TransactionId tid) {
            this.tid = tid;
            this.taleId = getId();
        }

        //读取新的页面，更换新的页面的迭代器
        private void loadPage() throws DbException, TransactionAbortedException {
            if (pageNo < numPages()) {
                Page page = Database.getBufferPool().getPage(tid, new HeapPageId(taleId, pageNo++), Permissions.READ_ONLY);
                if (page instanceof HeapPage) {
                    //this.heapPage = (HeapPage) page;
                    this.iterator = ((HeapPage) page).iterator();
                } else {
                    throw new DbException("Expected HeapPage but got " + page.getClass().getSimpleName());
                }
            } else {
                iterator = null;
            }


        }

        @Override
        public void open() throws DbException, TransactionAbortedException {
            isOpen = true;
            if (iterator == null) {
                loadPage();
            }
        }


        @Override
        public boolean hasNext() throws DbException, TransactionAbortedException {
            if (!isOpen || iterator == null) {
                return false;
            }
            if (iterator.hasNext()) {
                //当前页面仍旧可用
                return true;
            } else {
                //更换新的页面
                while (pageNo < numPages()) {
                    loadPage();
                    if (iterator != null && iterator.hasNext()) {
                        break;
                    }
                }
                if (iterator == null) {
                    return false;
                } else {
                    return iterator.hasNext();
                }

            }
        }

        @Override
        public Tuple next() throws DbException, TransactionAbortedException, NoSuchElementException {
            if (!isOpen) {
                throw new NoSuchElementException();
            }
            Tuple res = null;
            if (hasNext()) {
                res = iterator.next();
            }
            return res;
        }

        //指针重置为开始状态
        @Override
        public void rewind() throws DbException, TransactionAbortedException {
            if (!isOpen) {
                throw new NoSuchElementException();
            }
            pageNo = 0;
            loadPage();
        }

        @Override
        public void close() {
            isOpen = false;
            // iterator=null;
        }
    }


}

