package simpledb.storage;

import com.sun.source.tree.ReturnTree;
import simpledb.common.Database;
import simpledb.common.DbException;
import simpledb.common.DeadlockException;
import simpledb.common.Permissions;
import simpledb.transaction.LockManager;
import simpledb.transaction.TransactionAbortedException;
import simpledb.transaction.TransactionId;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * BufferPool manages the reading and writing of pages into memory from
 * disk. Access methods call into it to retrieve pages, and it fetches
 * pages from the appropriate location.
 * <p>
 * The BufferPool is also responsible for locking;  when a transaction fetches
 * a page, BufferPool checks that the transaction has the appropriate
 * locks to read/write the page.
 *
 * @Threadsafe, all fields are final
 */
public class BufferPool {
    /**
     * Bytes per page, including header.
     */
    private static final int DEFAULT_PAGE_SIZE = 4096;

    private static int pageSize = DEFAULT_PAGE_SIZE;

    private HashMap<PageId, Page> map;
    private LinkedList<Page> pages;
    /**
     * Default number of pages passed to the constructor. This is used by
     * other classes. BufferPool should use the numPages argument to the
     * constructor instead.
     */
    public static final int DEFAULT_PAGES = 50;

    private int numPages = 0;
    LockManager lockManager = new LockManager();

    /**
     * Creates a BufferPool that caches up to numPages pages.
     *
     * @param numPages maximum number of pages in this buffer pool.
     */
    public BufferPool(int numPages) {
        // TODO: some code goes here
        map = new HashMap<>();
        pages = new LinkedList<>();
        this.numPages = numPages == 0 ? DEFAULT_PAGES : numPages;
    }

    public static int getPageSize() {
        return pageSize;
    }


    // THIS FUNCTION SHOULD ONLY BE USED FOR TESTING!!
    public static void setPageSize(int pageSize) {
        BufferPool.pageSize = pageSize;
    }

    // THIS FUNCTION SHOULD ONLY BE USED FOR TESTING!!
    public static void resetPageSize() {
        BufferPool.pageSize = DEFAULT_PAGE_SIZE;
    }

    /**
     * Retrieve the specified page with the associated permissions.
     * Will acquire a lock and may block if that lock is held by another
     * transaction.
     * <p>
     * The retrieved page should be looked up in the buffer pool.  If it
     * is present, it should be returned.  If it is not present, it should
     * be added to the buffer pool and returned.  If there is insufficient
     * space in the buffer pool, a page should be evicted and the new page
     * should be added in its place.
     *
     * @param tid  the ID of the transaction requesting the page
     * @param pid  the ID of the requested page
     * @param perm the requested permissions on the page
     */
    public Page getPage(TransactionId tid, PageId pid, Permissions perm)
            throws TransactionAbortedException, DbException {
        final int timeout = new Random().nextInt(2000) + 1000;
        boolean lockSuccess = lockManager.tryAcquireLock(tid, pid, perm, timeout);
        if (lockSuccess) {
            Page pg = map.get(pid);
            if (pg != null) return pg;
            pg = readPageInBf(pid);
            return pg;
        } else {
            throw new TransactionAbortedException();
        }
    }

    private Page readPageInBf(PageId pid) throws DbException {
        DbFile db = Database.getCatalog().getDatabaseFile(pid.getTableId());
        Page pg = db.readPage(pid);
        if (pages.size() == numPages) {
            evictPage();
        }
        pages.add(pg);
        map.put(pid, pg);
        return pg;
    }

    /**
     * Releases the lock on a page.
     * Calling this is very risky, and may result in wrong behavior. Think hard
     * about who needs to call this and why, and why they can run the risk of
     * calling it.
     *
     * @param tid the ID of the transaction requesting the unlock
     * @param pid the ID of the page to unlock
     */
    public void unsafeReleasePage(TransactionId tid, PageId pid) {
        // TODO: some code goes here
        // not necessary for lab1|lab2
        lockManager.tryReleaseLock(tid, pid, 5000);
    }


    /**
     * Return true if the specified transaction has a lock on the specified page
     */
    public boolean holdsLock(TransactionId tid, PageId p) {
        // TODO: some code goes here
        // not necessary for lab1|lab2
        return lockManager.holdsLock(tid, p);
    }

    /**
     * Release all locks associated with a given transaction.
     *
     * @param tid the ID of the transaction requesting the unlock
     */
    public void transactionComplete(TransactionId tid) {
        // TODO: some code goes here
        // not necessary for lab1|lab2
        transactionComplete(tid, true);
    }

    /**
     * Commit or abort a given transaction; release all locks associated to
     * the transaction.
     *
     * @param tid    the ID of the transaction requesting the unlock
     * @param commit a flag indicating whether we should commit or abort
     */
    public void transactionComplete(TransactionId tid, boolean commit) {
        // TODO: some code goes here
        // not necessary for lab1|lab2

        if (commit) {
            try {
                flushPages(tid);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            try {
                rollBackPages(tid);
            } catch (DbException e) {
                throw new RuntimeException(e);
            }
        }
        Set<PageId> pageIds = lockManager.releaseLocks(tid);
//        if (pageIds != null) {
//            // 创建副本避免并发修改异常
//            Set<PageId> pageIdsCopy = new HashSet<>(pageIds);
//            for (PageId pageId : pageIdsCopy) {
//                unsafeReleasePage(tid, pageId);
//            }
//        }

    }

    private void rollBackPages(TransactionId tid) throws DbException {
        Set<PageId> pageIds = lockManager.getTransactionLocks().get(tid);
        if (pageIds != null) {
            // 创建副本避免并发修改异常
            Set<PageId> pageIdsCopy = new HashSet<>(pageIds);
            for (PageId pageId : pageIdsCopy) {
                removePage(pageId);
            }

            for (PageId pageId : pageIdsCopy) {
                readPageInBf(pageId);
            }
        }
    }


    /**
     * Add a tuple to the specified table on behalf of transaction tid.  Will
     * acquire a write lock on the page the tuple is added to and any other
     * pages that are updated (Lock acquisition is not needed for lab2).
     * May block if the lock(s) cannot be acquired.
     * <p>
     * Marks any pages that were dirtied by the operation as dirty by calling
     * their markDirty bit, and adds versions of any pages that have
     * been dirtied to the cache (replacing any existing versions of those pages) so
     * that future requests see up-to-date pages.
     *
     * @param tid     the transaction adding the tuple
     * @param tableId the table to add the tuple to
     * @param t       the tuple to add
     */
    public void insertTuple(TransactionId tid, int tableId, Tuple t)
            throws DbException, IOException, TransactionAbortedException {
        // TODO: some code goes here
        DbFile databaseFile = Database.getCatalog().getDatabaseFile(tableId);
        List<Page> pages = databaseFile.insertTuple(tid, t);
        for (Page page : pages) {
            page.markDirty(true, tid);
            //本来把计入缓存放在HeapFile里了,结果测试过不去
            map.put(page.getId(), page);
        }
        // not necessary for lab1
    }

    /**
     * Remove the specified tuple from the buffer pool.
     * Will acquire a write lock on the page the tuple is removed from and any
     * other pages that are updated. May block if the lock(s) cannot be acquired.
     * <p>
     * Marks any pages that were dirtied by the operation as dirty by calling
     * their markDirty bit, and adds versions of any pages that have
     * been dirtied to the cache (replacing any existing versions of those pages) so
     * that future requests see up-to-date pages.
     *
     * @param tid the transaction deleting the tuple.
     * @param t   the tuple to delete
     */
    public void deleteTuple(TransactionId tid, Tuple t)
            throws DbException, IOException, TransactionAbortedException {
        // TODO: some code goes here
        int tableId = t.recordId.getPageId().getTableId();
        DbFile databaseFile = Database.getCatalog().getDatabaseFile(tableId);
        List<Page> pages = databaseFile.deleteTuple(tid, t);
        for (Page page : pages) {
            page.markDirty(true, tid);
            map.put(page.getId(), page);
        }

        // not necessary for lab1
    }

    /**
     * Flush all dirty pages to disk.
     * NB: Be careful using this routine -- it writes dirty data to disk so will
     * break simpledb if running in NO STEAL mode.
     */
    public synchronized void flushAllPages() throws IOException {
        // TODO: some code goes here
        // not necessary for lab1
        for (Map.Entry<PageId, Page> pageEntry : map.entrySet()) {
            if (pageEntry.getValue().isDirty() != null) {
                flushPage(pageEntry.getKey());
            }
        }

    }

    /**
     * Remove the specific page id from the buffer pool.
     * Needed by the recovery manager to ensure that the
     * buffer pool doesn't keep a rolled back page in its
     * cache.
     * <p>
     * Also used by B+ tree files to ensure that deleted pages
     * are removed from the cache so they can be reused safely
     */
    public synchronized void removePage(PageId pid) {
        // TODO: some code goes here
        // not necessary for lab1
        Page page = map.get(pid);
        if (page == null) {
            return;
        }
        // 从map中移除
        map.remove(page.getId());
        // 从pages列表中移除
        pages.remove(page);
    }

    /**
     * Flushes a certain page to disk
     *
     * @param pid an ID indicating the page to flush
     */
    private synchronized void flushPage(PageId pid) throws IOException {
        // TODO: some code goes here
        // not necessary for lab1
        Page page = map.get(pid);
        if (page != null) {
            Database.getCatalog().getDatabaseFile(pid.getTableId()).writePage(page);
            page.markDirty(false, null);
        }


    }

    /**
     * Write all pages of the specified transaction to disk.
     */
    public synchronized void flushPages(TransactionId tid) throws IOException {
        // TODO: some code goes here
        // not necessary for lab1|lab2
        Set<PageId> pageIds = lockManager.getTransactionLocks().get(tid);
        if (pageIds != null && !pageIds.isEmpty()) {
            for (PageId pageId : pageIds) {
                flushPage(pageId);
            }
        }
    }

    /**
     * Discards a page from the buffer pool.
     * Flushes the page to disk to ensure dirty pages are updated on disk.
     */
    private synchronized void evictPage() throws DbException {
        // 实现 NO STEAL 策略：不驱逐脏页面
        // 遍历页面列表，找到第一个非脏页面进行驱逐
        Iterator<Page> iterator = pages.iterator();
        while (iterator.hasNext()) {
            Page page = iterator.next();
            if (page.isDirty() == null) {  // 非脏页面
                iterator.remove();
                map.remove(page.getId());
                return;
            }
        }

        // 如果所有页面都是脏页面，则抛出异常
        throw new DbException("All pages are dirty, cannot evict any page under NO STEAL policy");
    }

}
