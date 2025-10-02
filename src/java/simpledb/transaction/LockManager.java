package simpledb.transaction;

import simpledb.common.Permissions;
import simpledb.storage.PageId;


import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @ClassName: LockManager
 * @Description:
 * @Author: LittleWool
 * @Date: 2025/9/26 15:04
 * @Version: 1.0
 **/

public class LockManager {

    // 使用更合适的并发数据结构
    private final ConcurrentHashMap<PageId, PageLock> pageLocks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<TransactionId, Set<PageId>> transactionLocks = new ConcurrentHashMap<>();
    //等待图
    private final ConcurrentHashMap<TransactionId,Set<TransactionId>> waitsForGraph =new ConcurrentHashMap<>();
    public LockManager() {

    }


    public static class PageLock {
        private PageId pageId;
        private Permissions lockType;
        private Set<TransactionId> holders;


        public PageLock(PageId pageId,  Permissions lockType) {
            this.pageId = pageId;
            this.holders = ConcurrentHashMap.newKeySet();
            this.lockType = lockType;
        }

        // getter 和 setter 方法
        public PageId getPageId() {
            return pageId;
        }

        public Permissions getLockType() {
            return lockType;
        }

        public void setLockType(Permissions lockType) {
            this.lockType = lockType;
        }

        public Set<TransactionId> getHolders() {
            return holders;
        }


        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            PageLock pageLock = (PageLock) o;
            return Objects.equals(pageId, pageLock.pageId);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(pageId);
        }
    }

    public ConcurrentHashMap<PageId, PageLock> getPageLocks() {
        return pageLocks;
    }

    public ConcurrentHashMap<TransactionId, Set<PageId>> getTransactionLocks() {
        return transactionLocks;
    }

    public boolean tryAcquireLock(TransactionId tid, PageId pid, Permissions perm, long time) throws TransactionAbortedException {
        long start = System.currentTimeMillis();
        int count = 0;
        synchronized (pid) {
            while (true) {
                count++;
                if (System.currentTimeMillis() - start > time) {
                    removeWaitEdge(tid);
                    return false;
                } else {
                    if (acquireLock(tid, pid, perm)) {
                        Set<PageId> pageIds = transactionLocks.computeIfAbsent(tid, k -> new HashSet<>());
                        pageIds.add(pid);
                        removeWaitEdge(tid);
                        return true;
                    }else {
                        PageLock pageLock=pageLocks.get(pid);
                        if (pageLock!=null&&!pageLock.holders.isEmpty()){
                            for (TransactionId holder : pageLock.holders) {
                                if (!holder.equals(tid)) {
                                    addWaitEdge(tid, holder);
                                    if (detectCycle(tid)) {
                                        removeWaitEdge(tid);
                                        throw new TransactionAbortedException();
                                    }
                                }
                            }
                        }
                    }
                    try {
                        pid.wait(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        removeWaitEdge(tid);
                        return false;
                    }
                }
            }
        }
    }

    public boolean acquireLock(TransactionId tid, PageId pid, Permissions perm) {
        if (perm == Permissions.READ_ONLY) {
            return tryAcquireReadLock(tid, pid);
        } else {
            return tryAcquireReadWriteLock(tid, pid);
        }

    }
    public boolean releaseLock(TransactionId tid, PageId pid) {
        boolean res = false;
        synchronized (pid) {
            PageLock pageLock = pageLocks.get(pid);
            if (pageLock == null) {
                removeWaitEdge(tid);
                return true;
            }
            if (pageLock.holders.contains(tid)){
                pageLock.holders.remove(tid);
                if (pageLock.holders.isEmpty()){
                    pageLocks.remove(pid);
                }
                res=true;
            }
            if (res) {
                Set<PageId> pageIds = transactionLocks.get(tid);
                pageIds.remove(pid);
                if (pageIds.isEmpty()) {
                    transactionLocks.remove(tid);
                }
                removeWaitEdge(tid);
            }
        }
        return res;
    }

    private boolean tryAcquireReadWriteLock(TransactionId tid, PageId pid) {
        PageLock pageLock = pageLocks.computeIfAbsent(pid, k -> new PageLock(pid,Permissions.READ_WRITE));
        boolean res = false;
        if (pageLock.holders.isEmpty()||(pageLock.holders.contains(tid)&&pageLock.holders.size()==1)){
            pageLock.setLockType(Permissions.READ_WRITE);
            pageLock.holders.add(tid);
            res=true;
        }
        return res;
    }


    private boolean tryAcquireReadLock(TransactionId tid, PageId pid) {
        PageLock pageLock = pageLocks.computeIfAbsent(pid, k -> new PageLock(pid,Permissions.READ_ONLY));
        boolean res = false;
        if (pageLock.lockType==Permissions.READ_ONLY||pageLock.holders.isEmpty()){
            pageLock.setLockType(Permissions.READ_ONLY);
            pageLock.holders.add(tid);
            res=true;
        }else if (pageLock.holders.contains(tid)){
            res=true;
        }

        return res;

    }

    public boolean tryReleaseLock(TransactionId tid, PageId pid, long time) {
        long start = System.currentTimeMillis();

        synchronized (pid) {
            while (true) {
                if (System.currentTimeMillis() - start > time) {
                    return false;
                } else {
                    if (releaseLock(tid, pid)) {
                        return true;
                    }
                }
            }
        }
    }



    public boolean holdsLock(TransactionId tid, PageId pid) {
        PageLock pageLock=pageLocks.get(pid);
        boolean res = false;
        if (pageLock!=null&&pageLock.holders.contains(tid)) {
            res = true;
        }
        return res;
    }

    private void addWaitEdge(TransactionId waitingTx,TransactionId holdingTx){
        synchronized (waitsForGraph){
            waitsForGraph.computeIfAbsent(waitingTx,k->new HashSet<>()).add(holdingTx);
        }
    }
    private void removeWaitEdge(TransactionId tid){
        synchronized (waitsForGraph){
            waitsForGraph.remove(tid);
            for (Set<TransactionId> value : waitsForGraph.values()) {
                value.remove(tid);
            }
        }
    }
    private boolean detectCycle(TransactionId startTid){
        synchronized (waitsForGraph){
            HashSet<TransactionId> visited=new HashSet<>();
            HashSet<TransactionId> recursionStack=new HashSet<>();
            return isCycle(startTid,visited,recursionStack);
        }

    }

    private boolean isCycle(TransactionId startTid, HashSet<TransactionId> visited, HashSet<TransactionId> recursionStack) {
        if (!visited.contains(startTid)){
            visited.add(startTid);
            recursionStack.add(startTid);
            Set<TransactionId> neighbors = waitsForGraph.getOrDefault(startTid, new HashSet<>());
            if (!neighbors.isEmpty()){
                for (TransactionId neighbor : neighbors) {
                    if (!visited.contains(neighbor)&&isCycle(neighbor,visited,recursionStack)){
                        return true;
                    }else if (recursionStack.contains(neighbor)){
                        return true;
                    }
                }
            }
        }
        recursionStack.remove(startTid);
        return false;
    }


}
