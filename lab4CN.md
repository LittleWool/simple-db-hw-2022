# 6.5830/6.5831 实验4：SimpleDB事务处理

**发布时间：** 2022年10月24日（周一）

**截止时间：** 2022年11月9日（周三）晚上11:59（东部时间）

在本实验中，你将在SimpleDB中实现一个基于锁的简单事务系统。你需要在代码中的适当位置添加锁和解锁调用，以及跟踪每个事务持有的锁并在需要时授予事务锁的代码。

本文档的其余部分描述了添加事务支持所涉及的内容，并提供了如何将此支持添加到数据库的基本大纲。

与之前的实验一样，我们建议你尽早开始。锁和事务可能很难调试！

## 1. 开始

你应该从Lab 3提交的代码开始（如果你没有提交Lab 3的代码，或者你的解决方案不能正常工作，请联系我们讨论选项）。此外，我们为这个实验提供了额外的测试用例，这些测试用例不在你收到的原始代码分发中。我们重申，我们提供的单元测试是为了帮助指导你的实现，但它们并不全面，也不能确定正确性。

你需要将这些新文件添加到你的发布版本中。最简单的方法是切换到你的项目目录（可能叫做simple-db-hw-2022）并从主GitHub仓库拉取：

```
$ cd simple-db-hw-2022
$ git pull upstream main
```


## 2. 事务、锁和并发控制

在开始之前，你应该确保理解事务是什么以及严格两阶段锁（你将使用它来确保事务的隔离性和原子性）是如何工作的。

在本节的其余部分，我们简要概述这些概念并讨论它们如何与SimpleDB相关。

### 2.1. 事务

事务是一组数据库操作（例如，插入、删除和读取），这些操作以*原子*方式执行；也就是说，要么所有操作都完成，要么都不完成，对于数据库的外部观察者来说，这些操作不是作为一个单一的、不可分割的操作来完成的。

### 2.2. ACID属性

为了帮助你理解SimpleDB中的事务管理如何工作，我们简要回顾一下它如何确保满足ACID属性：

* **原子性**：严格两阶段锁和仔细的缓冲区管理确保原子性。
* **一致性**：通过原子性确保数据库的事务一致性。其他一致性问题（例如，键约束）在SimpleDB中没有解决。
* **隔离性**：严格两阶段锁提供隔离性。
* **持久性**：FORCE缓冲区管理策略确保持久性（见下面第2.3节）。

### 2.3. 恢复和缓冲区管理

为了简化你的工作，我们建议你实现NO STEAL/FORCE缓冲区管理策略。

正如我们在课堂上讨论的，这意味着：

* 你不应该驱逐被未提交事务锁定的脏（已更新）页面（这是NO STEAL）。
* 在事务提交时，你应该强制将脏页面写入磁盘（例如，写出这些页面）（这是FORCE）。

为了进一步简化你的工作，你可以假设SimpleDB在处理[transactionComplete](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\transaction\Transaction.java#L52-L71)命令时不会崩溃。请注意，这三点意味着你不需要在本实验中实现基于日志的恢复，因为你永远不会需要撤销任何工作（你从不驱逐脏页面），你也永远不会需要重做任何工作（你在提交时强制更新，并且在提交处理过程中不会崩溃）。

### 2.4. 授予锁

你需要在SimpleDB中添加调用（例如，在[BufferPool](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\BufferPool.java#L25-L275)中），允许调用者代表特定事务请求或释放特定对象上的（共享或独占）锁。

我们建议在*页面*粒度上加锁；请不要为测试简单性而实现表级锁（尽管这是可能的）。本文档的其余部分和我们的单元测试都假设页面级锁。

你需要创建数据结构来跟踪每个事务持有的锁，并在请求锁时检查是否应该授予事务锁。

你需要实现共享锁和独占锁；回想一下，它们的工作方式如下：

* 在事务可以读取对象之前，它必须对该对象持有共享锁。
* 在事务可以写入对象之前，它必须对该对象持有独占锁。
* 多个事务可以对一个对象持有共享锁。
* 只有一个事务可以对一个对象持有独占锁。
* 如果事务*t*是唯一持有对象*o*共享锁的事务，*t*可以将其在*o*上的锁*升级*为独占锁。

如果事务请求的锁无法立即授予，你的代码应该*阻塞*，等待该锁变得可用（即被运行在不同线程中的另一个事务释放）。在你的锁实现中要注意竞态条件——思考并发调用你的锁可能如何影响行为。你可能希望阅读Java中的[线程同步](http://docs.oracle.com/javase/tutorial/essential/concurrency/sync.html)。

---

**练习1.**

在BufferPool中编写获取和释放锁的方法。假设你使用页面级锁，你需要完成以下内容：

* 修改[getPage()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\BufferPool.java#L86-L92)在返回页面之前阻塞并获取所需的锁。
* 实现[unsafeReleasePage()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\BufferPool.java#L113-L116)。这个方法主要用于测试，以及在事务结束时。
* 实现[holdsLock()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\BufferPool.java#L131-L135)，以便练习2中的逻辑可以确定事务是否已经锁定了页面。

你可能会发现定义一个`LockManager`类来负责维护事务和锁的状态是有帮助的，但设计决定权在你。

你可能需要在通过[LockingTest](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\test\simpledb\LockingTest.java#L15-L204)中的单元测试之前实现下一个练习。

---

### 2.5. 锁的生命周期

你需要实现严格两阶段锁。这意味着事务在访问任何对象之前应该获取适当类型的锁，并且在事务提交之前不应该释放任何锁。

幸运的是，SimpleDB的设计使得在[BufferPool.getPage()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\BufferPool.java#L86-L92)中获取页面锁成为可能，然后你才能读取或修改它们。因此，我们建议在[getPage()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\BufferPool.java#L86-L92)中获取锁，而不是在每个操作符中添加对锁例程的调用。根据你的实现，你可能不需要在其他地方获取锁。由你来验证这一点！

在读取任何页面（或元组）之前，你需要获取*共享*锁，而在写入任何页面（或元组）之前，你需要获取*独占*锁。你会注意到我们已经在BufferPool中传递[Permissions](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\common\Permissions.java#L7-L9)对象；这些对象指示调用者希望在被访问的对象上拥有什么类型的锁（我们已经给了你[Permissions](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\common\Permissions.java#L7-L9)类的代码。）

注意，你的[HeapFile.insertTuple()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\HeapFile.java#L145-L177)和[HeapFile.deleteTuple()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\HeapFile.java#L184-L197)实现，以及[HeapFile.iterator()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\HeapFile.java#L209-L214)返回的迭代器实现应该使用[BufferPool.getPage()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\BufferPool.java#L86-L92)访问页面。仔细检查这些不同用途的[getPage()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\BufferPool.java#L86-L92)传递了正确的权限对象（例如，[Permissions.READ_WRITE](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\common\Permissions.java#L8-L8)或[Permissions.READ_ONLY](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\common\Permissions.java#L8-L8)）。你可能还想仔细检查你的[BufferPool.insertTuple()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\BufferPool.java#L164-L175)和`BufferPool.deleteTupe()`实现是否在它们访问的任何页面上调用了[markDirty()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\Page.java#L35-L35)（你应该在Lab 2中实现此代码时就做了这一点，但我们没有测试这种情况。）

在获取锁之后，你还需要考虑何时释放它们。显然，你应该在事务提交或中止后释放与该事务相关的所有锁，以确保严格的2PL。然而，可能存在其他场景，在事务结束之前释放锁可能是有用的。例如，你可以在扫描页面找到空槽后立即释放页面上的共享锁。

---

**练习2.**

确保在整个SimpleDB中正确获取和释放锁。一些（但不一定是全部）你应该验证正常工作的操作：

* 在SeqScan期间从页面读取元组（如果你在[BufferPool.getPage()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\BufferPool.java#L86-L92)中实现了锁，只要你的[HeapFile.iterator()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\HeapFile.java#L209-L214)使用[BufferPool.getPage()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\BufferPool.java#L86-L92)，这应该能正常工作。）
* 通过BufferPool和HeapFile方法插入和删除元组（如果你在[BufferPool.getPage()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\BufferPool.java#L86-L92)中实现了锁，只要[HeapFile.insertTuple()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\HeapFile.java#L145-L177)和[HeapFile.deleteTuple()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\HeapFile.java#L184-L197)使用[BufferPool.getPage()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\BufferPool.java#L86-L92)，这应该能正常工作。）

你还需要特别仔细地考虑在以下情况中获取和释放锁：

* 向[HeapFile](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\HeapFile.java#L23-L315)添加新页面。你什么时候物理地将页面写入磁盘？是否存在与其他事务（在其他线程上）的竞态条件，需要在HeapFile级别特别注意，而不考虑页面级锁？
* 寻找可以插入元组的空槽。大多数实现在扫描页面寻找空槽时需要READ_ONLY锁。然而，令人惊讶的是，如果事务*t*在页面*p*上找不到空槽，*t*可以立即释放*p*上的锁。虽然这显然违反了两阶段锁的规则，但这是可以的，因为*t*没有使用页面上的任何数据，使得并发事务**t'** 更新*p*不可能影响*t*的答案或结果。

在这一点上，你的代码应该通过[LockingTest](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\test\simpledb\LockingTest.java#L15-L204)中的单元测试。

---

### 2.6. 实现NO STEAL

事务的修改只有在提交后才写入磁盘。这意味着我们可以通过丢弃脏页面并从磁盘重新读取它们来中止事务。因此，我们不能驱逐脏页面。这个策略叫做NO STEAL。

你需要修改[BufferPool](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\BufferPool.java#L25-L275)中的[evictPage()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\BufferPool.java#L268-L273)方法。特别是，它绝不能驱逐脏页面。如果你的驱逐策略优先驱逐脏页面，你将不得不找到一种方法来驱逐替代页面。在缓冲池中所有页面都是脏页面的情况下，你应该抛出一个[DbException](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\common\DbException.java#L5-L11)。如果你的驱逐策略驱逐了一个干净页面，要注意事务可能已经持有的被驱逐页面上的任何锁，并在你的实现中适当地处理它们。

---

**练习3.**

在[BufferPool](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\BufferPool.java#L25-L275)的[evictPage()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\BufferPool.java#L268-L273)方法中实现不驱逐脏页面的页面驱逐必要逻辑。

---

### 2.7. 事务

在SimpleDB中，每个查询开始时都会创建一个[TransactionId](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\transaction\TransactionId.java#L8-L42)对象。这个对象被传递给查询中涉及的每个操作符。当查询完成时，调用[BufferPool](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\BufferPool.java#L25-L275)方法[transactionComplete()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\transaction\Transaction.java#L52-L71)。

调用此方法要么*提交*要么*中止*事务，由参数标志[commit](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\transaction\Transaction.java#L38-L40)指定。在其执行过程中的任何时候，操作符都可能抛出[TransactionAbortedException](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\transaction\TransactionAbortedException.java#L5-L10)异常，这表明发生了内部错误或死锁。我们提供的测试用例为你创建了适当的[TransactionId](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\transaction\TransactionId.java#L8-L42)对象，以适当的方式将它们传递给你的操作符，并在查询完成时调用[transactionComplete()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\transaction\Transaction.java#L52-L71)。我们还实现了[TransactionId](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\transaction\TransactionId.java#L8-L42)。

---

**练习4.**

在[BufferPool](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\BufferPool.java#L25-L275)中实现[transactionComplete()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\transaction\Transaction.java#L52-L71)方法。注意有两个版本的transactionComplete，一个接受额外的布尔**commit**参数，一个不接受。没有额外参数的版本应该总是提交，因此可以通过调用[transactionComplete(tid, true)](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\transaction\Transaction.java#L52-L71)来简单实现。

当你提交时，你应该将与事务相关的脏页面刷新到磁盘。当你中止时，你应该通过将页面恢复到磁盘状态来撤销事务所做的任何更改。

无论事务提交还是中止，你还应该释放[BufferPool](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\BufferPool.java#L25-L275)为事务保持的任何状态，包括释放事务持有的任何锁。

在这一点上，你的代码应该通过[TransactionTest](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\test\simpledb\TransactionTest.java#L14-L129)单元测试和[AbortEvictionTest](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\test\simpledb\systemtest\AbortEvictionTest.java#L18-L96)系统测试。你可能会发现`TransactionTest{One, Two Five, Ten, AllDirty}`系统测试很有说明性，但在你完成下一个练习之前它们可能会失败。

### 2.8. 死锁和中止

SimpleDB中的事务可能会死锁（如果你不理解原因，我们建议阅读Ramakrishnan & Gehrke关于死锁的内容）。你需要检测这种情况并抛出[TransactionAbortedException](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\transaction\TransactionAbortedException.java#L5-L10)。

检测死锁有许多可能的方法。一个简单的例子是实现一个简单的超时策略，如果事务在给定时间段后没有完成就中止它。对于真正的解决方案，你可以在讲座中展示的依赖图数据结构中实现循环检测。在这种方案中，你会定期或在每次尝试授予新锁时检查依赖图中的循环，并在存在循环时中止某些事务。在检测到死锁后，你必须决定如何改善情况。假设你在事务*t*等待锁时检测到死锁。如果你有杀戮倾向，你可以中止*t*正在等待的所有事务；这可能导致大量工作被撤销，但你可以保证*t*会取得进展。或者，你可以决定中止*t*以给其他事务一个取得进展的机会。这意味着最终用户将不得不重试事务*t*。

另一种方法是使用事务的全局排序来避免构建等待图。出于性能原因，这有时是首选的，但在此方案下可能错误地中止本可以成功的事务。示例包括WAIT-DIE和WOUND-WAIT方案。

---

**练习5.**

在`src/simpledb/BufferPool.java`中实现死锁检测或预防。你的死锁处理系统有许多设计决策，但不需要做一些非常复杂的事情。我们期望你做得比每个事务的简单超时更好。一个很好的起点是在每次锁请求之前实现等待图中的循环检测，这样的实现将获得满分。请在实验报告中描述你的选择，并列出你的选择与替代方案相比的优缺点。

你应该确保你的代码在发生死锁时正确中止事务，通过抛出[TransactionAbortedException](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\transaction\TransactionAbortedException.java#L5-L10)异常。这个异常将被执行事务的代码（例如，[TransactionTestUtil.java](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\test\simpledb\systemtest\TransactionTestUtil.java)）捕获，它应该调用[transactionComplete()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\transaction\Transaction.java#L52-L71)来清理事务。你不应该自动重启因死锁而失败的事务——你可以假设更高级别的代码会处理这个问题。

我们在`test/simpledb/DeadlockTest.java`中提供了一些（不是那么单元的）测试。它们实际上有点复杂，所以它们可能需要超过几秒钟才能运行（取决于你的策略）。如果它们似乎无限期地挂起，那么你可能有一个未解决的死锁。这些测试构建了简单的死锁情况，你的代码应该能够逃脱。

注意在`DeadLockTest.java`顶部附近有两个时间参数；这些参数确定测试检查锁是否已被获取的频率以及中止事务重启前的等待时间。如果你使用基于超时的检测方法，通过调整这些参数你可能会观察到不同的性能特征。测试会将对应于已解决死锁的`TransactionAbortedExceptions`输出到控制台。

你的代码现在应该通过`TransactionTest{One, Two, Five, Ten, AllDirty}`系统测试（根据你的实现，这些测试也可能运行相当长的时间）。

在这一点上，你应该有一个可恢复的数据库，这意味着如果数据库系统崩溃（在[transactionComplete()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\transaction\Transaction.java#L52-L71)之外的点）或者用户显式中止事务，任何运行事务的效果在系统重启（或事务中止）后都不会可见。你可能希望通过运行一些事务并显式杀死数据库服务器来验证这一点。

---

### 2.9. 设计选择

在本实验过程中，我们已经确定了一些你需要做出的重要设计选择：

* 锁粒度：页面级与元组级
* 死锁处理：检测vs预防，中止自己vs他人

---

**奖励练习6.（20%额外学分）**

对于一个或多个这些选择，实现两种替代方案并实验性地比较它们的性能特征。在你的报告中包含你的基准测试代码和简要评估（可能带有图表）。

你现在已经完成了这个实验。干得好！

---

## 3. 后勤

你必须提交你的代码（见下文）以及描述你方法的简短（最多2页）报告。这份报告应该：

* 描述你在死锁处理中的任何设计决策，并列出你方法的优缺点。
* 讨论并证明你对API所做的任何更改。
* 描述你的代码中任何缺失或不完整的元素。
* 描述你在实验上花费的时间，以及是否有任何你发现特别困难或令人困惑的地方。
* 描述你已完成的任何额外学分实现。

### 3.1. 协作

这个实验对单个人来说应该是可管理的，但如果你更喜欢与合作伙伴一起工作，这也是可以的。不允许更大的小组。请在你的报告中清楚地说明你与谁合作（如果有的话）。

### 3.2. 提交你的作业

我们将使用Gradescope来自动评分所有编程作业。你们都应该已经被邀请到班级实例；如果没有，请告诉我们，我们可以帮助你设置。你可以在截止日期前多次提交你的代码；我们将使用Gradescope确定的最新版本。将报告放在一个名为`lab4-writeup.txt`的文件中与你的提交一起。你还需要显式添加你创建的任何其他文件，如新的`*.java`文件。

如果你与合作伙伴一起工作，只有一个人需要提交到Gradescope。但是，请确保将另一个人添加到你的小组。还要注意，每个成员都必须有自己的报告。请在文件名和报告本身中添加你的Kerberos用户名（例如，`lab4-writeup-username1.txt`和`lab4-writeup-username2.txt`）。

提交到Gradescope的最简单方法是使用包含你的代码的`.zip`文件。在Linux/macOS上，你可以通过运行以下命令来实现：

```bash
$ zip -r submission.zip src/ lab4-writeup.txt

# 如果你与合作伙伴一起工作：
$ zip -r submission.zip src/ lab4-writeup-username1.txt lab4-writeup-username2.txt
```


### 3.3. 提交错误

SimpleDB是一个相对复杂的代码。你很可能会发现错误、不一致以及糟糕、过时或不正确的文档等。

因此，我们要求你以冒险的心态来做这个实验。如果某些事情不清楚，甚至错误，不要生气；相反，试着自己弄清楚或者给我们发一封友好的邮件。

请将（友好的！）错误报告提交到 <a href="mailto:6.5830-staff@mit.edu">6.5830-staff@mit.edu</a>。当你这样做时，请尽量包括：

* 错误的描述。
* 一个我们可以放在`test/simpledb`目录中的[.java](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\common\Permissions.java)文件，可以编译和运行。
* 一个包含重现错误数据的[.txt](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\bin\depcache\dependencies.txt)文件。我们应该能够使用[HeapFileEncoder](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\HeapFileEncoder.java#L17-L227)将其转换为`.dat`文件。

如果你觉得遇到了错误，你也可以在Piazza的班级页面上发帖。

### 3.4 评分

你的成绩50%将基于你的代码是否通过我们将运行的系统测试套件。这些测试将是我们提供测试的超集。在提交之前，你应该确保它在`ant test`和`ant systemtest`中都不产生错误（通过所有测试）。

**新增：**

* 鉴于这个实验需要你大量修改早期代码，我们强烈建议你确保你的实现通过早期实验的测试（特别是实验1和实验2）。如果你在取得进展方面遇到困难，因为你的实现无法通过之前实验的测试，请联系我们讨论选项。

* 鉴于这个实验涉及并发，我们将在截止日期后重新运行自动评分器，以阻止尝试幸运的错误代码。你有责任确保你的代码**可靠地**通过测试。

* 这个实验有50%的手动评分比例，比之前的实验高。具体来说，如果你的并发处理是虚假的（例如，插入`Thread.sleep(1000)`直到竞态消失），我们会很不高兴。

**重要：** 在测试之前，Gradescope将用我们的版本替换你的[build.xml](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\build.xml)、[HeapFileEncoder.java](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\HeapFileEncoder.java)、[Parser.java](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\Parser.java)和整个`test`目录的内容。这意味着你不能改变`.dat`文件的格式，也不能依赖[Parser.java](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\Parser.java)中的任何自定义功能。你也应该小心改变我们的API。你应该测试你的代码编译未修改的测试。

提交后，你应该从Gradescope获得失败测试的即时反馈和错误输出（如果有的话）。另外50%的成绩将基于你的报告质量和我们对你代码的主观评估。这部分也将在我们完成评分后在Gradescope上公布。

我们在设计这个作业时很有趣，我们希望你喜欢破解它！