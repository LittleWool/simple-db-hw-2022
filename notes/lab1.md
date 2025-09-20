# Lab 1 实验总结

## Exercise 1: 实体类补全
补全两个实体类并没有太大问题

## Exercise 2: Catalog 类实现
- 因为是表信息的目录类，所以创建了一个存储表信息的 [TableInfo](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\common\TableInfo.java#L13-L64)
- [TableInfo](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\common\TableInfo.java#L13-L64) 相比于 [TupleDesc](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\TupleDesc.java#L11-L266) 多了主键（暂时没用，也先存储起来了）、`id` 以及存储的表的文件、表名
- 最初并未找到 `id` 在哪获取，后在测试以及前人笔记的基础上，找到在 [DbFile](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\DbFile.java#L21-L99) 中
- [DbFile](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\DbFile.java#L21-L99) 还有 [TupleDesc](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\TupleDesc.java#L11-L266)，百宝箱了属于是
- 使用了两张 [map](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\BufferPool.java#L33-L33) 加快查找速度

## Exercise 3: BufferPool 实现
- [BufferPool](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\BufferPool.java#L25-L228) 的 [getPage](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\BufferPool.java#L81-L89) 完成即可，初步使用一个 [map](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\BufferPool.java#L33-L33) 搞定
- [BufferPool](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\BufferPool.java#L25-L228) 就是磁盘文件和内存之间的区域，无论是从 disk 读入数据还是将数据写回磁盘都需要经过 [BufferPool](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\BufferPool.java#L25-L228)

## Exercise 4: HeapPageId、RecordId、HeapPage 实现
- 两个 `ID` 都是一些构造方法或者 `get`、`set` 的补全，主要是 [HeapPage](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\HeapPage.java#L20-L390)
- [RecordId](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\RecordId.java#L11-L55) 记录的是在页中确认记录位置的 `Id`
- [HeapPage](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\HeapPage.java#L20-L390) 中包含 [HeapPageId](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\HeapPageId.java#L9-L70)，由 [tableId](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\common\TableInfo.java#L15-L15) 和 `pageNum` 组成，即可以确认是某一个表的某一页，所以是一张表其中的一页
- 一部分已经给出了部分信息的计算公式，直接填上去即可

### 技术要点：
- 很多数据库内通用的信息都可以使用 [Database](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\common\Database.java#L19-L84) 来获取，比如页面大小和目录，使用目录来进一步获取表的相关信息
- 在这个练习中，也学习了一些关于位运算的信息：给出一个 bit 的位置，可以先使用整数除法和取余，计算是哪个字节的哪一位
- 该练习是以字节为单位存储的，就可以定位到字节位置，然后将 1 左移 x 位置，进行 & 运算
- 因为其他位置为 0，若是结果不为 1，则说明该字节的这位不为 0，即是 1
- 在本次练习中，也练习了如何自定义实现迭代器，当然也可以直接转化为集合或使用 `Arrays` 类直接获取 [iterator](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\HeapPage.java#L357-L359)

## Exercise 5: HeapFile 实现
- [HeapFile](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\HeapFile.java#L31-L255) 用来表示一张表的实体类
- 存储有该表的物理文件，有记录的增删功能（还未实现），全表的记录遍历（通过迭代器实现，就是把每个页的迭代加上了用完页之后更换新的页面）

### 遇到的问题：
- 在 `public int numPages()` 中计算公式应该是用总字节数/每页大小，二者都是字节，所以不用处理
- 但我错误地在分母乘上了 8，但在 `readTest` 是体现不出来的，结果在最后一个练习迟迟不通过
- 最后使用 AI 找出来具体错误

## Exercise 6: SeqScan 实现
- 对 [HeapFile](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\HeapFile.java#L31-L255) 的迭代器进行封装就是 [scan](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\test\simpledb\FilterTest.java#L21-L21)，真是封了一层又一层
- [SeqScan](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\execution\SeqScan.java#L17-L152) 的表模式描述是需要加上前缀的，所以需要在之前表模式描述的字段上创建一个新的加上前缀的

## 总结：
即使是lab1也不像大佬们说的那么简单，还有就是感谢人们的笔记，最后AI真好用，idea上md写着好难受，使用typora又有点割裂，在idea上写完之后用让AI美化格式👍