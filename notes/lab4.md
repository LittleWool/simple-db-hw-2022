## Lab4 实现要点总结

### 锁管理机制设计
- 推荐使用 [LockManager](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\transaction\LockManager.java#L17-L252) 类来统一管理锁机制
- 锁类型区分：
    - **共享锁**：允许多个事务同时持有
    - **排他锁**：仅允许单个事务持有
- 锁粒度采用页面级别，需要维护以下映射关系：
    - 事务ID与页面的映射关系
    - 页面与锁信息的映射关系
    - 页面等待队列的映射关系
- 需要定义专门的锁信息类，并实现获取锁和释放锁的基本功能

### 关键实现细节
- 获取锁时必须对 [PageId](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\PageId.java#L5-L39) 对象进行同步加锁，确保锁信息的准确性
- 死锁检测采用等待图法来解决

### 遇到的问题及解决方案

1. **页面获取超时问题**：
    - 问题原因：测试代码中会执行刷盘和 `resetDataBase` 操作，但部分静态变量未正确刷新
    - 解决方案：调整静态变量的作用域，确保与实例正确绑定

2. **并发修改异常**：
    - 问题场景：在 [unsafeReleasePage](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\BufferPool.java#L123-L127) 方法中使用增强for循环释放页面时出现
    - 解决方案：创建副本集合后再进行释放操作

3. **页面驱逐限制**：
    - 注意事项：驱逐测试中不能驱逐脏页
    - 解决方案：遇到脏页时直接跳过，不进行驱逐操作

4. **权限控制补充**：
    - 在多处 [getPage](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\BufferPool.java#L89-L101) 调用中补充了合适的权限参数
    - 在 [DbFile](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\DbFile.java#L21-L99) 读取页面时也需要考虑是否需要加锁

5. **性能优化空间**：
    - 删除 `List` 中页面时使用的遍历方式有待优化