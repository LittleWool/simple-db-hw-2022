
#### 日志格式
raf.writeInt(COMMIT_RECORD);类型
raf.writeLong(tid.getId());事务id

raf.writeLong(currentOffset);偏移量
currentOffset = raf.getFilePointer();
force();
tidToFirstLogRecord.remove(tid.getId());



raf.writeInt(UPDATE_RECORD);
raf.writeLong(tid.getId());

writePageData(raf, before);
writePageData(raf, after);

raf.writeLong(currentOffset);
currentOffset = raf.getFilePointer();