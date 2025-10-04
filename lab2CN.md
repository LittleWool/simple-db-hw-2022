# 6.5830/6.5831 实验2: SimpleDB操作符

**发布日期:** 2022年9月26日星期一

**截止日期:** 2022年10月5日星期三晚上11:59 ET

在这个实验作业中，您将为SimpleDB编写一组操作符来实现表修改（例如，插入和删除记录）、选择、连接和聚合。这些将建立在您在实验1中编写的基础上，为您提供一个可以对多个表执行简单查询的数据库系统。

此外，我们在实验1中忽略了缓冲池管理的问题：我们没有处理当我们在数据库生命周期中引用的页面超过内存中能容纳的页面时出现的问题。在实验2中，您将设计一个驱逐策略来从缓冲池中刷新陈旧页面。

您不需要在此实验中实现事务或锁定。

本文档的其余部分提供了一些关于如何开始编码的建议，描述了一组练习来帮助您完成实验，并讨论了如何提交您的代码。这个实验需要您编写相当多的代码，所以我们鼓励您**尽早开始**！

---

**更新 (2022年10月05日):** 如果您在10月5日或之后开始这个实验，您从这个仓库获得的代码将包括后续实验的起始代码和测试。要获得仅用于实验2的代码和测试子集，请切换到`lab2`分支（`git checkout lab2`）。

<a name="starting"></a>

## 1. 入门指南

您应该从您为实验1提交的代码开始（如果您没有为实验1提交代码，或者您的解决方案不能正常工作，请联系我们讨论选项）。此外，我们正在为这个实验提供额外的源文件和测试文件，这些文件不在您收到的原始代码分发中。

### 1.1. 获取实验2

您需要将这些新文件添加到您的版本中。最简单的方法是导航到您的项目目录（可能叫做`simple-db-hw-2022`）并从主GitHub仓库拉取：

```
$ cd simple-db-hw-2022
$ git pull upstream main
```


**IDE用户**需要更新他们的项目依赖以包含新的库jar文件。

对于VSCode，请检查窗口左下角"Java Projects"面板中的"Referenced Libraries"部分。查看[jline-0.9.94.jar](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\lib\jline-0.9.94.jar)和[zql.jar](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\lib\zql.jar)是否出现。如果没有，点击"+"图标并添加jar文件（它们应该在`lib`目录中）。

对于IntelliJ或Eclipse的简单解决方案，运行

```
ant eclipse
```


然后用IntelliJ或Eclipse重新打开项目。

如果您对项目设置做了其他更改并且不想丢失它们，也可以手动添加依赖。

对于IntelliJ，进入**File**下的**Project Structure**，在**Modules**下选择`simpledb`项目，导航到**Dependencies**选项卡。在窗格底部，点击"+"图标将jar文件添加为编译时依赖。

对于Eclipse，在包资源管理器中右键单击项目名称（可能是`simple-db-hw-2022`），选择**Properties**。在左侧选择**Java Build Path**，在右侧点击**Libraries**选项卡。点击**Add JARs...**按钮，选择**zql.jar**和**jline-0.9.94.jar**，然后点击**OK**，再点击**OK**。您的代码现在应该可以编译了。

### 1.2. 实现提示

和以前一样，我们**强烈建议**您通读整个文档以了解SimpleDB的高级设计，然后再编写代码。

我们建议按照本文档中的练习来指导您的实现，但您可能会发现不同的顺序对您更有意义。和以前一样，我们将通过查看您的代码并验证您是否通过了`test`和`systemtest`的ant目标测试来评分您的作业。请注意，代码只需要通过我们在本实验中指出的测试，而不是所有的单元和系统测试。有关评分的完整讨论和您需要通过的测试列表，请参见第3.4节。

以下是您可能进行SimpleDB实现的一个粗略大纲；关于此大纲中步骤的更多详细信息，包括练习，将在下面的第2节中给出。

* 实现[Filter](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\execution\Filter.java#L12-L98)和[Join](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\execution\Join.java#L14-L157)操作符并验证它们对应的测试是否正常工作。这些操作符的Javadoc注释包含了它们应该如何工作的详细信息。我们已经为您提供了[Project](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\execution\Project.java#L14-L96)和[OrderBy](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\execution\OrderBy.java#L13-L99)的实现，这可能有助于您理解其他操作符的工作原理。

* 实现[IntegerAggregator](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\execution\IntegerAggregator.java#L13-L174)和[StringAggregator](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\execution\StringAggregator.java#L15-L142)。在这里，您将编写实际计算跨多个组的输入元组序列中特定字段聚合的逻辑。使用整数除法计算平均值，因为SimpleDB只支持整数。StringAggegator只需要支持COUNT聚合，因为其他操作对字符串没有意义。

* 实现[Aggregate](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\execution\Aggregate.java#L17-L185)操作符。和其他操作符一样，聚合实现[OpIterator](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\execution\OpIterator.java#L19-L67)接口，这样它们就可以放置在SimpleDB查询计划中。请注意，[Aggregate](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\execution\Aggregate.java#L17-L185)操作符的输出是每次调用[next()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\execution\Query.java#L86-L92)时整个组的聚合值，聚合构造函数接受聚合和分组字段。

* 在[BufferPool](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\BufferPool.java#L27-L346)中实现与元组插入、删除和页面驱逐相关的方法。此时您不需要担心事务。

* 实现[Insert](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\execution\Insert.java#L18-L120)和[Delete](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\execution\Delete.java#L21-L114)操作符。和所有操作符一样，[Insert](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\execution\Insert.java#L18-L120)和[Delete](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\execution\Delete.java#L21-L114)实现[OpIterator](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\execution\OpIterator.java#L19-L67)，接受要插入或删除的元组流，并输出一个包含整数字段的单个元组，该字段指示插入或删除的元组数。这些操作符需要调用[BufferPool](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\BufferPool.java#L27-L346)中实际修改磁盘上页面的适当方法。检查插入和删除元组的测试是否正常工作。

请注意，SimpleDB不实现任何一致性或完整性检查，因此可以将重复记录插入文件，而且没有办法强制执行主键或外键约束。

此时您应该能够通过ant `systemtest`目标中的测试，这是本实验的目标。

您还将能够使用提供的SQL解析器对您的数据库运行SQL查询！有关简要教程，请参见[第2.7节](#parser)。

最后，您可能会注意到本实验中的迭代器扩展了[Operator](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\execution\Operator.java#L15-L109)类而不是实现OpIterator接口。由于[next](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\execution\Query.java#L86-L92)/[hasNext](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\execution\Query.java#L73-L75)的实现通常是重复的、烦人的和容易出错的，[Operator](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\execution\Operator.java#L15-L109)通用地实现了这个逻辑，只需要您实现更简单的[readNext](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\test\simpledb\TestUtil.java#L279-L287)。您可以随意使用这种实现风格，或者如果您愿意，也可以直接实现[OpIterator](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\execution\OpIterator.java#L19-L67)接口。要实现OpIterator接口，请从迭代器类中移除`extends Operator`，并替换为`implements OpIterator`。

## 2. SimpleDB架构和实现指南

### 2.1. 过滤器和连接

回想一下，SimpleDB OpIterator类实现了关系代数的操作。现在您将实现两个操作符，使您能够执行比表扫描稍微更有趣的查询。

* *Filter*：此操作符只返回满足其构造函数中指定的[Predicate](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\execution\Predicate.java#L11-L120)的元组。因此，它过滤掉不匹配谓词的任何元组。

* *Join*：此操作符根据其构造函数中传入的[JoinPredicate](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\execution\JoinPredicate.java#L12-L63)连接其两个子节点的元组。我们只需要一个简单的嵌套循环连接，但您可以探索更有趣的连接实现。在您的实验报告中描述您的实现。

**练习1.**

实现以下骨架方法：

---

* src/java/simpledb/execution/Predicate.java
* src/java/simpledb/execution/JoinPredicate.java
* src/java/simpledb/execution/Filter.java
* src/java/simpledb/execution/Join.java

---

此时，您的代码应该通过[PredicateTest](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\test\simpledb\PredicateTest.java#L11-L65)、[JoinPredicateTest](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\test\simpledb\JoinPredicateTest.java#L12-L67)、[FilterTest](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\test\simpledb\FilterTest.java#L18-L134)和[JoinTest](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\test\simpledb\JoinTest.java#L19-L120)中的单元测试。此外，您应该能够通过[FilterTest](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\test\simpledb\FilterTest.java#L18-L134)和[JoinTest](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\test\simpledb\JoinTest.java#L19-L120)系统测试。

### 2.2. 聚合

另一个SimpleDB操作符实现了带有`GROUP BY`子句的基本SQL聚合。您应该实现五个SQL聚合（[COUNT](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\execution\Aggregator.java#L20-L20)、[SUM](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\execution\Aggregator.java#L20-L20)、[AVG](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\execution\Aggregator.java#L20-L20)、[MIN](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\execution\Aggregator.java#L20-L20)、[MAX](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\execution\Aggregator.java#L20-L20)）并支持分组。您只需要支持单个字段上的聚合和单个字段的分组。

为了计算聚合，我们使用[Aggregator](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\execution\Aggregator.java#L11-L87)接口，它将新元组合并到现有的聚合计算中。在构造期间，[Aggregator](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\execution\Aggregator.java#L11-L87)被告知应该使用什么操作进行聚合。随后，客户端代码应该为子迭代器中的每个元组调用[Aggregator.mergeTupleIntoGroup()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\execution\Aggregator.java#L78-L78)。在所有元组合并后，客户端可以检索聚合结果的OpIterator。结果中的每个元组都是[(groupValue, aggregateValue)](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\test\simpledb\AggregateTest.java#L18-L199)形式的对，除非分组字段的值是[Aggregator.NO_GROUPING](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\execution\Aggregator.java#L12-L12)，在这种情况下结果是[(aggregateValue)](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\test\simpledb\AggregateTest.java#L196-L198)形式的单个元组。

请注意，这种实现需要的空间与不同组的数量成线性关系。就本实验而言，您不需要担心组数量超过可用内存的情况。

**练习2.**

实现以下骨架方法：

---

* src/java/simpledb/execution/IntegerAggregator.java
* src/java/simpledb/execution/StringAggregator.java
* src/java/simpledb/execution/Aggregate.java

---

此时，您的代码应该通过[IntegerAggregatorTest](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\test\simpledb\IntegerAggregatorTest.java#L17-L190)、[StringAggregatorTest](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\test\simpledb\StringAggregatorTest.java#L15-L117)和[AggregateTest](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\test\simpledb\AggregateTest.java#L18-L199)单元测试。此外，您应该能够通过[AggregateTest](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\test\simpledb\AggregateTest.java#L18-L199)系统测试。

### 2.3. HeapFile可变性

现在，我们将开始实现支持修改表的方法。我们从单个页面和文件级别开始。有两种主要操作集：添加元组和删除元组。

**删除元组：** 要删除元组，您需要实现[deleteTuple](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\test\simpledb\BTreeLeafPageTest.java#L320-L347)。元组包含`RecordIDs`，允许您找到它们所在的页面，所以这应该很简单，只需定位元组所属的页面并适当修改页面的头部。

**添加元组：** [HeapFile.java](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\HeapFile.java)中的[insertTuple](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\test\simpledb\BufferPoolWriteTest.java#L72-L88)方法负责向堆文件添加元组。要向HeapFile添加新元组，您必须找到有空槽的页面。如果HeapFile中不存在这样的页面，您需要创建新页面并将其追加到磁盘上的物理文件中。您需要确保元组中的RecordID正确更新。

**练习3.**

实现以下剩余骨架方法：

---

* src/java/simpledb/storage/HeapPage.java
* src/java/simpledb/storage/HeapFile.java<br>
  （请注意，此时您不一定需要实现[writePage()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreeFile.java#L139-L152)）。

---

要实现HeapPage，您需要修改头部位图以用于[insertTuple()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\test\simpledb\BufferPoolWriteTest.java#L72-L88)和[deleteTuple()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\test\simpledb\BTreeLeafPageTest.java#L320-L347)等方法。您可能会发现我们在实验1中要求您实现的[getNumUnusedSlots()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\HeapPage.java#L351-L365)和[isSlotUsed()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreePage.java#L144-L144)方法作为有用的抽象。请注意，提供了一个[markSlotUsed()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\HeapPage.java#L384-L395)方法作为抽象来修改页面头部中元组的填充或清除状态。

请注意，[HeapFile.insertTuple()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\HeapFile.java#L145-L177)和[HeapFile.deleteTuple()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\HeapFile.java#L184-L197)方法必须使用[BufferPool.getPage()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\BufferPool.java#L89-L101)方法访问页面；否则，您在下一个实验中实现事务将无法正常工作。

在`src/simpledb/BufferPool.java`中实现以下骨架方法：

---

* [insertTuple()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\test\simpledb\BufferPoolWriteTest.java#L72-L88)
* [deleteTuple()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\test\simpledb\BTreeLeafPageTest.java#L320-L347)

---

这些方法应该调用属于被修改表的HeapFile中的适当方法（这种额外的间接层是必要的，以支持其他类型的文件——比如索引——在未来）。

此时，您的代码应该通过[HeapPageWriteTest](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\test\simpledb\HeapPageWriteTest.java#L23-L139)、[HeapFileWriteTest](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\test\simpledb\HeapFileWriteTest.java#L16-L99)和[BufferPoolWriteTest](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\test\simpledb\BufferPoolWriteTest.java#L23-L148)中的单元测试。

### 2.4. 插入和删除

现在您已经编写了所有用于添加和删除元组的HeapFile机制，您将实现[Insert](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\execution\Insert.java#L18-L120)和[Delete](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\execution\Delete.java#L21-L114)操作符。

对于实现`insert`和`delete`查询的计划，最顶层的操作符是特殊的[Insert](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\execution\Insert.java#L18-L120)或[Delete](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\execution\Delete.java#L21-L114)操作符，它们修改页面。这些操作符返回受影响的元组数。这是通过返回包含计数的单个整数字段元组来实现的。

* *Insert*：此操作符将从其子操作符读取的元组添加到其构造函数中指定的[tableid](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreeFile.java#L32-L32)。它应该使用[BufferPool.insertTuple()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\BufferPool.java#L215-L226)方法来执行此操作。

* *Delete*：此操作符将从其子操作符读取的元组从其构造函数中指定的[tableid](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreeFile.java#L32-L32)中删除。它应该使用[BufferPool.deleteTuple()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\BufferPool.java#L241-L253)方法来执行此操作。

**练习4.**

实现以下骨架方法：

---

* src/java/simpledb/execution/Insert.java
* src/java/simpledb/execution/Delete.java

---

此时，您的代码应该通过[InsertTest](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\test\simpledb\InsertTest.java#L18-L69)中的单元测试。我们没有为[Delete](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\execution\Delete.java#L21-L114)提供单元测试。此外，您应该能够通过[InsertTest](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\test\simpledb\InsertTest.java#L18-L69)和[DeleteTest](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\test\simpledb\systemtest\DeleteTest.java#L19-L68)系统测试。

### 2.5. 页面驱逐

在实验1中，我们没有正确观察由构造函数参数[numPages](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreeFile.java#L157-L160)定义的缓冲池中最大页面数的限制。现在，您将选择页面驱逐策略并修改任何读取或创建页面的先前代码来实现您的策略。

当缓冲池中的页面超过[numPages](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreeFile.java#L157-L160)时，在加载下一个页面之前应该从池中驱逐一个页面。驱逐策略的选择由您决定；没有必要做复杂的事情。在实验报告中描述您的策略。

请注意，[BufferPool](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\BufferPool.java#L27-L346)要求您实现一个[flushAllPages()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\BufferPool.java#L260-L269)方法。这在实际的缓冲池实现中是不需要的。但是，我们需要这个方法用于测试目的。您应该永远不会在任何实际代码中调用此方法。

由于我们实现`ScanTest.cacheTest`的方式，您需要确保您的[flushPage()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\BufferPool.java#L298-L308)和[flushAllPages()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\BufferPool.java#L260-L269)方法在适当通过此测试时不驱逐缓冲池中的页面。

[flushAllPages()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\BufferPool.java#L260-L269)应该在[BufferPool](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\BufferPool.java#L27-L346)中的所有页面上调用[flushPage()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\BufferPool.java#L298-L308)，而[flushPage()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\BufferPool.java#L298-L308)应该将任何脏页面写入磁盘并标记为不脏，同时将其留在[BufferPool](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\BufferPool.java#L27-L346)中。

唯一应该从缓冲池中移除页面的方法是[evictPage()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\BufferPool.java#L328-L343)，它应该在驱逐任何脏页面时调用[flushPage()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\BufferPool.java#L298-L308)。

**练习5.**

填写[flushPage()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\BufferPool.java#L298-L308)方法和额外的辅助方法来实现页面驱逐：

---

* src/java/simpledb/storage/BufferPool.java

---

如果您在上面的[HeapFile.java](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\HeapFile.java)中没有实现[writePage()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreeFile.java#L139-L152)，您也需要在这里实现。最后，您还应该实现[removePage()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\BufferPool.java#L280-L291)来从缓冲池中移除页面*而不*将其刷新到磁盘。我们不会在本实验中测试[removePage()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\BufferPool.java#L280-L291)，但它对未来的实验是必要的。

此时，您的代码应该通过[EvictionTest](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\test\simpledb\systemtest\EvictionTest.java#L26-L91)系统测试。

由于我们不会检查任何特定的驱逐策略，此测试通过创建一个具有16个页面的[BufferPool](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\BufferPool.java#L27-L346)来工作（注意：虽然[DEFAULT_PAGES](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\BufferPool.java#L42-L42)是50，但我们初始化BufferPool时使用了更少的页面！），扫描一个具有远超16个页面的文件，并查看JVM的内存使用量是否增加了超过5 MB。如果您没有正确实现驱逐策略，您将不会驱逐足够的页面，并会超出大小限制，从而导致测试失败。

您现在已经完成了这个实验。干得好！

<a name="query_walkthrough"></a>

### 2.6. 查询演练

以下代码实现了两个表之间的简单连接查询，每个表由三个整数列组成。（文件`some_data_file1.dat`和`some_data_file2.dat`是来自此文件的页面的二进制表示）。此代码等同于SQL语句：

```sql
SELECT *
FROM some_data_file1,
     some_data_file2
WHERE some_data_file1.field1 = some_data_file2.field1
  AND some_data_file1.id > 1
```


要查看更多查询操作的示例，您可能会发现浏览连接、过滤器和聚合的单元测试很有帮助。

```java
package simpledb;

import java.io.*;

public class jointest {

    public static void main(String[] argv) {
        // 构造一个3列的表模式
        Type types[] = new Type[]{Type.INT_TYPE, Type.INT_TYPE, Type.INT_TYPE};
        String names[] = new String[]{"field0", "field1", "field2"};

        TupleDesc tupleDesc = new TupleDesc(types, names);

        // 创建表，将它们与数据文件关联
        // 并告诉目录表的模式。
        HeapFile table1 = new HeapFile(new File("some_data_file1.dat"), tupleDesc);
        Database.getCatalog().addTable(table1, "t1");

        HeapFile table2 = new HeapFile(new File("some_data_file2.dat"), tupleDesc);
        Database.getCatalog().addTable(table2, "t2");

        // 构造查询：我们使用两个SeqScans，它们通过迭代器将元组送入连接
        TransactionId tid = new TransactionId();

        SeqScan ss1 = new SeqScan(tid, table1.getId(), "t1");
        SeqScan ss2 = new SeqScan(tid, table2.getId(), "t2");

        // 为where条件创建过滤器
        Filter sf1 = new Filter(
                new Predicate(0,
                        Predicate.Op.GREATER_THAN, new IntField(1)), ss1);

        JoinPredicate p = new JoinPredicate(1, Predicate.Op.EQUALS, 1);
        Join j = new Join(p, sf1, ss2);

        // 并运行它
        try {
            j.open();
            while (j.hasNext()) {
                Tuple tup = j.next();
                System.out.println(tup);
            }
            j.close();
            Database.getBufferPool().transactionComplete(tid);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
```


两个表都有三个整数字段。为了表达这一点，我们创建一个[TupleDesc](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\TupleDesc.java#L11-L253)对象并传递给它一个[Type](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\common\Type.java#L16-L70)对象数组指示字段类型和`String`对象指示字段名称。一旦我们创建了这个[TupleDesc](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\TupleDesc.java#L11-L253)，我们初始化两个代表表的[HeapFile](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\HeapFile.java#L23-L315)对象。一旦我们创建了表，我们将它们添加到[Catalog](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\common\Catalog.java#L23-L207)中。（如果这是一个已经在运行的数据库服务器，我们将加载这些目录信息；我们只需要为这个测试的目的加载它）。

一旦我们完成了数据库系统的初始化，我们就创建一个查询计划。我们的计划由两个[SeqScan](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\execution\SeqScan.java#L17-L160)操作符组成，它们扫描磁盘上每个文件的元组，连接到第一个[HeapFile](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\HeapFile.java#L23-L315)上的[Filter](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\execution\Filter.java#L12-L98)操作符，连接到根据[JoinPredicate](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\execution\JoinPredicate.java#L12-L63)连接表中元组的[Join](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\execution\Join.java#L14-L157)操作符。一般来说，这些操作符使用对适当表（在[SeqScan](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\execution\SeqScan.java#L17-L160)的情况下）或子操作符（在例如[Join](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\execution\Join.java#L14-L157)的情况下）的引用来实例化。测试程序然后在[Join](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\execution\Join.java#L14-L157)操作符上重复调用[next](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\execution\Query.java#L86-L92)，这反过来从其子节点拉取元组。当元组从[Join](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\execution\Join.java#L14-L157)输出时，它们会在命令行上打印出来。

<a name="parser"></a>

### 2.7. 查询解析器

我们为您提供了一个SimpleDB的查询解析器，一旦您完成了本实验中的练习，您可以使用它来编写和运行针对您的数据库的SQL查询。

第一步是创建一些数据表和目录。假设您有一个文件`data.txt`，内容如下：

```
1,10
2,20
3,30
4,40
5,50
5,50
```


您可以使用[convert](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\HeapFileEncoder.java#L58-L63)命令将其转换为SimpleDB表（确保先输入`ant`！）：

```bash
java -jar dist/simpledb.jar convert data.txt 2 "int,int"
```


这创建了一个文件`data.dat`。除了表的原始数据外，两个附加参数指定每条记录有两个字段，它们的类型是`int`和`int`。

接下来，创建一个目录文件`catalog.txt`，内容如下：

```
data (f1 int, f2 int)
```


这告诉SimpleDB有一个表`data`（存储在`data.dat`中），有两个名为[f1](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\test\simpledb\JoinOptimizerTest.java#L54-L54)和[f2](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\test\simpledb\JoinOptimizerTest.java#L60-L60)的整数字段。

最后，调用解析器。您必须从命令行运行[java](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\common\Database.java)（ant不能正确处理交互式目标）。从`simpledb/`目录中，输入：

```bash
java -jar dist/simpledb.jar parser catalog.txt
```


您应该看到如下输出：

```
Added table : data with schema INT(f1), INT(f2), 
SimpleDB> 
```


最后，您可以运行查询：

```
SimpleDB> select d.f1, d.f2 from data d;
Started a new transaction tid = 1221852405823
 ADDING TABLE d(data) TO tableMap
     TABLE HAS  tupleDesc INT(d.f1), INT(d.f2), 
1       10
2       20
3       30
4       40
5       50
5       50

 6 rows.
----------------
0.16 seconds

SimpleDB> 
```


解析器功能相对完整（包括对[SELECT](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\optimizer\QueryPlanVisualizer.java#L14-L14)、`INSERT`、`DELETE`和事务的支持），但确实存在一些问题，不一定报告完全翔实的错误消息。以下是一些需要注意的限制：

* 您必须在每个字段名前加上表名，即使字段名是唯一的（您可以使用表名别名，如上面的示例所示，但不能使用`AS`关键字。）

* 嵌套查询在`WHERE`子句中受支持，但在`FROM`子句中不受支持。

* 不支持算术表达式（例如，您不能取两个字段的和）。

* 最多允许一个`GROUP BY`和一个聚合列。

* 不允许使用面向集合的操作符如`IN`、`UNION`和`EXCEPT`。

* `WHERE`子句中只允许`AND`表达式。

* 不支持`UPDATE`表达式。

* 字符串操作符[LIKE](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\execution\Predicate.java#L19-L19)是允许的，但必须完全写出（也就是说，不允许使用Postgres波浪号[~]简写。）

## 3. 后勤

您必须提交您的代码（见下文）以及描述您的方法的简短（最多2页）报告。这个报告应该：

* 描述您做出的任何设计决策，包括您选择的页面驱逐策略。如果您使用了嵌套循环连接以外的算法，请描述您选择的算法的权衡。

* 讨论并证明您对API所做的任何更改。

* 描述您的代码中任何缺失或不完整的元素。

* 描述您在实验上花费的时间，以及是否有任何您觉得特别困难或令人困惑的地方。

### 3.1. 协作

这个实验一个人应该可以管理，但如果您更喜欢与合作伙伴一起工作，这也是可以的。不允许更大的小组。请在您的个人报告中清楚地说明您与谁合作（如果有的话）。

### 3.2. 提交您的作业

我们将使用gradescope对所有编程作业进行自动评分。您应该都已受邀加入班级实例；如果没有，请查看piazza获取邀请码。如果您仍有问题，请告诉我们，我们可以帮助您设置。您可以在截止日期前多次提交代码；我们将使用gradescope确定的最新版本。将报告放在名为`lab2-writeup.txt`的文件中与您的提交一起。您还需要显式添加您创建的任何其他文件，如新的`*.java`文件。

如果您与合作伙伴合作，只有一个人需要提交到Gradescope。但是，请确保将其他人添加到您的小组中。还要注意，每个成员都必须有自己的报告。请在文件名和报告本身中添加您的Kerberos用户名（例如，`lab2-writeup-username1.txt`和`lab2-writeup-username2.txt`）。

提交到gradescope的最简单方法是使用包含您的代码的`.zip`文件。在Linux/MacOS上，您可以通过运行以下命令来实现：

```bash
$ zip -r submission.zip src/ lab2-writeup.txt

# 如果您与合作伙伴合作：
$ zip -r submission.zip src/ lab2-writeup-username1.txt lab2-writeup-username2.txt
```


<a name="bugs"></a>

### 3.3. 提交错误

SimpleDB是一个相对复杂的代码。您很可能会发现错误、不一致以及过时或不正确的文档等。

因此，我们要求您以冒险的心态来做这个实验。如果有什么不清楚或甚至是错误的，不要生气；相反，试着自己弄清楚或给我们发送一封友好的电子邮件。

请将（友好的！）错误报告提交到[6.5830-staff@mit.edu](mailto:6.5830-staff@mit.edu)。当您这样做时，请尝试包括：

* 错误的描述。

* 我们可以放在`test/simpledb`目录中、编译和运行的[.java](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\common\Database.java)文件。

* 重现错误的数据的[.txt](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\bin\depcache\dependencies.txt)文件。我们应该能够使用[HeapFileEncoder](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\HeapFileEncoder.java#L17-L227)将其转换为`.dat`文件。

如果您觉得遇到了错误，也可以在Piazza的班级页面上发帖。

<a name="grading"></a>

### 3.4 评分

您的成绩75%将基于您的代码是否通过我们将对其运行的系统测试套件。这些测试将是我们提供的测试的超集。在提交代码之前，您应该确保从`ant test`和`ant systemtest`中不会产生错误（通过所有测试）。

**重要：** 在测试之前，Gradescope将用我们的版本替换您的[build.xml](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\build.xml)、[HeapFileEncoder.java](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\HeapFileEncoder.java)和`test`目录的全部内容。这意味着您不能更改`.dat`文件的格式！您也应该小心更改我们的API。您应该测试您的代码是否编译未修改的测试。

提交后，您应该立即从gradescope获得反馈和失败测试的错误输出（如果有的话）。给出的分数将是您作业自动评分部分的成绩。另外25%的成绩将基于您的报告质量和我们对您代码的主观评估。这部分也将在我们完成评分您的作业后在gradescope上发布。

我们在设计这个作业时有很多乐趣，我们希望您享受破解它的过程！