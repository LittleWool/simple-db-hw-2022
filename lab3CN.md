# 6.5830/6.5831 实验3: 查询优化

**发布日期:** 2022年10月5日星期三

**截止日期:** 2022年10月26日星期三晚上11:59 ET

在这个实验中，您将在SimpleDB之上实现一个查询优化器。主要任务包括实现选择性估计框架和基于成本的优化器。您在具体实现上有自由度，但我们建议使用类似于课堂上讨论的Selinger基于成本的优化器（第9讲）。

本文档的其余部分描述了添加优化器支持所涉及的内容，并提供了基本的实现大纲。

与之前的实验一样，我们建议您尽早开始。

## 1. 入门指南

---

**更新 (2022年10月24日):** 如果您在10月24日或之后开始这个实验，您从这个仓库获得的代码将包括后续实验的起始代码和测试。要获得仅用于实验3的代码和测试子集，请切换到`lab3`分支（`git checkout lab3`）。

---

您应该从您为实验2提交的代码开始。（如果您没有为实验2提交代码，或者您的解决方案不能正常工作，请联系我们讨论选项。）

我们为您提供了额外的测试用例以及本实验的源代码文件，这些文件不在您收到的原始代码分发中。我们再次鼓励您开发自己的测试套件来补充我们提供的测试。

您需要将这些新文件添加到您的版本中。最简单的方法是切换到您的项目目录（可能叫做`simple-db-hw-2022`）并从主GitHub仓库拉取：

```
$ cd simple-db-hw-2022
$ git pull upstream main
```


### 1.1. 实现提示
我们建议按照本文档中的练习来指导您的实现，但您可能会发现不同的顺序对您更有意义。和以前一样，我们将通过查看您的代码并验证您是否通过了`test`和`systemtest`的ant目标测试来评分您的作业。有关评分和您需要通过的测试的完整讨论，请参见第3.4节。

以下是您可能进行此实验的一种粗略大纲。这些步骤的更多详细信息将在下面的第2节中给出。

* 实现[TableStats](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\optimizer\TableStats.java#L23-L223)类中的方法，使其能够使用直方图（为[IntHistogram](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\optimizer\IntHistogram.java#L7-L231)类提供了骨架）或您设计的其他形式的统计数据来估计过滤器的选择性和扫描成本。
* 实现[JoinOptimizer](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\optimizer\JoinOptimizer.java#L18-L595)类中的方法，使其能够估计连接的成本和选择性。
* 编写[JoinOptimizer](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\optimizer\JoinOptimizer.java#L18-L595)中的[orderJoins](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\optimizer\JoinOptimizer.java#L243-L281)方法。该方法必须为一系列连接生成最优顺序（可能使用Selinger算法），给定在前两个步骤中计算的统计数据。

## 2. 优化器大纲

回想一下，基于成本的优化器的主要思想是：

* 使用表的统计数据来估计不同查询计划的"成本"。通常，计划的成本与中间连接和选择的基数（产生的元组数）以及过滤器和连接谓词的选择性相关。
* 使用这些统计数据以最优方式对连接和选择进行排序，并从几种替代方案中选择最佳的连接算法实现。

在这个实验中，您将实现代码来执行这两个功能。

优化器将从`simpledb/Parser.java`调用。您可能希望在开始这个实验之前回顾[实验2解析器练习](https://github.com/MIT-DB-Class/simple-db-hw-2022/blob/master/lab2.md#27-query-parser)。简而言之，如果您有一个描述您的表的目录文件`catalog.txt`，您可以通过以下方式运行解析器：
```bash
java -jar dist/simpledb.jar parser catalog.txt
```


当调用`Parser`时，它将使用您提供的统计数据代码计算所有表的统计数据。当发出查询时，解析器将把查询转换为逻辑计划表示，然后调用您的查询优化器来生成最优计划。

### 2.1 整体优化器结构
在开始实现之前，您需要理解SimpleDB优化器的整体结构。解析器和优化器的SimpleDB模块的总体控制流如图1所示。

<p align="center">
<img width=400 src="lab3-controlflow.png"><br>
<i>图1: 展示解析器中使用的类、方法和对象的图表</i>
</p>

底部的图例解释了符号；您将实现具有双边框的组件。类和方法将在后面的文本中更详细地解释（您可能希望参考回这个图表），但基本操作如下：

1. [Parser.java](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\Parser.java)在初始化时构造一组表统计数据（存储在[statsMap](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\optimizer\TableStats.java#L26-L26)容器中）。然后它等待输入查询，并在该查询上调用`parseQuery`方法。
2. `parseQuery`首先构造一个表示解析查询的[LogicalPlan](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\optimizer\LogicalPlan.java#L29-L570)。`parseQuery`然后在其构造的[LogicalPlan](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\optimizer\LogicalPlan.java#L29-L570)实例上调用[physicalPlan](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\optimizer\LogicalPlan.java#L316-L506)方法。[physicalPlan](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\optimizer\LogicalPlan.java#L316-L506)方法返回一个`DBIterator`对象，可用于实际运行查询。

在接下来的练习中，您将实现帮助[physicalPlan](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\optimizer\LogicalPlan.java#L316-L506)设计最优计划的方法。

### 2.2. 统计估计
准确估计计划成本相当棘手。在这个实验中，我们将只关注连接序列和基本表访问的成本。我们不会担心访问方法选择（因为我们只有一种访问方法，即表扫描）或额外操作符的成本（如聚合）。

您只需要考虑这个实验的左深计划。有关您可能实现的其他"奖励"优化器功能的描述，包括处理丛生计划的方法，请参见第2.3节。

#### 2.2.1 整体计划成本

我们将编写形式为`p=t1 join t2 join ... tn`的连接计划，这表示t1是最左边的连接（树中最深的）的左深连接。给定像[p](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\execution\Join.java#L26-L26)这样的计划，其成本可以表示为：

```
scancost(t1) + scancost(t2) + joincost(t1 join t2) +
scancost(t3) + joincost((t1 join t2) join t3) +
...
```


这里，`scancost(t1)`是扫描表t1的I/O成本，`joincost(t1,t2)`是将t1连接到t2的CPU成本。为了使I/O和CPU成本可比较，通常使用常数缩放因子，例如：

```
cost(predicate application) = 1
cost(pageScan) = SCALING_FACTOR x cost(predicate application)
```


对于这个实验，您可以忽略缓存的影响（例如，假设每次对表的访问都会产生完整的扫描成本）——再次说明，这可能是您在第2.3节中作为可选奖励扩展添加到实验中的内容。因此，`scancost(t1)`简单地等于表t1中的页数 x `SCALING_FACTOR`。

#### 2.2.2 连接成本

当使用嵌套循环连接时，回想一下两个表t1和t2（其中t1是外部表）之间连接的成本简单地是：

```
joincost(t1 join t2) = scancost(t1) + ntups(t1) x scancost(t2) //IO成本
                       + ntups(t1) x ntups(t2)  //CPU成本
```


这里，`ntups(t1)`是表t1中的元组数。

#### 2.2.3 过滤器选择性

可以通过扫描表直接计算基本表的`ntups`。估计具有一个或多个选择谓词的表的`ntups`可能更棘手——这是*过滤器选择性估计*问题。以下是一种您可以使用的方法，基于计算表中值的直方图：

* 计算表中每个属性的最小值和最大值（通过扫描一次）。
* 为表中的每个属性构造直方图。一种简单的方法是使用固定数量的桶*NumB*，每个桶代表直方图属性域中固定范围内的记录数。例如，如果字段*f*的范围从1到100，并且有10个桶，那么桶1可能包含1到10之间记录数的计数，桶2包含11到20之间记录数的计数，以此类推。
* 再次扫描表，选择出所有元组的所有字段并使用它们来填充每个直方图中桶的计数。
* 为了估计等式表达式*f = const*的选择性，计算包含值*const*的桶。假设桶的宽度（值范围）是*w*，高度（元组数）是*h*，表中的元组数是*ntups*。那么，假设值在桶中均匀分布，表达式的选择性大致是*(h / w) / ntups*，因为*(h/w)*代表具有值*const*的bin中的预期元组数。
* 为了估计范围表达式*f > const*的选择性，计算*const*所在的桶*b*，其宽度为*w_b*，高度为*h_b*。然后，*b*包含总元组的分数<nobr>*b_f = h_b / ntups* </nobr>。假设元组在*b*中均匀分布，*b*中*> const*的部分*b_part*是<nobr>*(b_right - const) / w_b*</nobr>，其中*b_right*是*b*桶的右端点。因此，桶*b*对谓词的贡献选择性是*(b_f x b_part)*。此外，桶*b+1...NumB-1*贡献它们所有的选择性（可以使用类似于上面*b_f*的公式计算）。将所有桶的选择性贡献相加将得出表达式的总体选择性。图2展示了这个过程。
* 涉及*小于*的表达式的选择性可以类似于大于情况来执行，查看桶直到0。

<p align="center">
<img width=400 src="lab3-hist.png"><br>
<i>图2: 展示您将在实验5中实现的直方图的图表</i>
</p>

在接下来的两个练习中，您将编写代码来执行连接和过滤器的选择性估计。

----------

**练习1: IntHistogram.java**

您需要实现某种方式来记录表统计数据以进行选择性估计。我们提供了一个骨架类[IntHistogram](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\optimizer\IntHistogram.java#L7-L231)来完成这个任务。我们的意图是您使用上面描述的基于桶的方法计算直方图，但您可以自由使用其他方法，只要它提供合理的选择性估计。

我们提供了一个类[StringHistogram](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\optimizer\StringHistogram.java#L8-L96)，它使用[IntHistogram](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\optimizer\IntHistogram.java#L7-L231)来计算字符串谓词的选择性。如果您想实现更好的估计器，可以修改[StringHistogram](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\optimizer\StringHistogram.java#L8-L96)，尽管为了完成这个实验，您不需要这样做。

完成这个练习后，您应该能够通过[IntHistogramTest](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\test\simpledb\IntHistogramTest.java#L8-L178)单元测试（如果您选择不实现基于直方图的选择性估计，则不需要通过此测试）。**如果您以其他方式实现选择性估计，请在报告中说明并描述您的方法。**

----------

**练习2: TableStats.java**

[TableStats](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\optimizer\TableStats.java#L23-L223)类包含计算表中元组数和页数的方法，以及估计该表字段上谓词选择性的方法。我们创建的查询解析器为每个表创建一个[TableStats](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\optimizer\TableStats.java#L23-L223)实例，并将这些结构传递给您的查询优化器（您将在后续练习中需要这些结构）。

您应该在[TableStats](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\optimizer\TableStats.java#L23-L223)中填写以下方法和类：

* 实现[TableStats](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\optimizer\TableStats.java#L23-L223)构造函数：一旦您实现了跟踪统计数据的方法（如直方图），您应该实现[TableStats](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\optimizer\TableStats.java#L23-L223)构造函数，添加代码来扫描表（可能多次）以构建您需要的统计数据。
* 实现`estimateSelectivity(int field, Predicate.Op op, Field constant)`：使用您的统计数据（例如，根据字段类型使用[IntHistogram](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\optimizer\IntHistogram.java#L7-L231)或[StringHistogram](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\optimizer\StringHistogram.java#L8-L96)），估计表上谓词`field op constant`的选择性。
* 实现[estimateScanCost()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\optimizer\TableStats.java#L164-L166)：此方法估计顺序扫描文件的成本，给定读取页面的成本是`costPerPageIO`。您可以假设没有寻道，也没有页面在缓冲池中。此方法可能使用在构造函数中计算的成本或大小。
* 实现[estimateTableCardinality(double selectivityFactor)](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\optimizer\TableStats.java#L176-L179)：此方法返回应用具有选择性selectivityFactor的谓词后关系中的元组数。此方法可能使用在构造函数中计算的成本或大小。

您可能希望修改[TableStats.java](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\optimizer\TableStats.java)的构造函数，例如，计算上面描述的用于选择性估计的字段直方图。

完成这些任务后，您应该能够通过[TableStatsTest](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\test\simpledb\TableStatsTest.java#L21-L162)中的单元测试。

----------

#### 2.2.4 连接基数

最后，观察上面的连接计划[p](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\execution\Join.java#L26-L26)的成本包括形式为`joincost((t1 join t2) join t3)`的表达式。要评估这个表达式，您需要某种方式来估计`t1 join t2`的大小（`ntups`）。这个*连接基数估计*问题比过滤器选择性估计问题更难。在这个实验中，您不需要为此做任何花哨的事情，尽管第2.4节中的一个可选练习包括基于直方图的连接选择性估计方法。

在实现您的简单解决方案时，您应该记住以下几点：

* 对于等值连接，当其中一个属性是主键时，连接产生的元组数不能大于非主键属性的基数。
* 对于没有主键的等值连接，很难说输出的大小是多少——它可能是表基数乘积的大小（如果两个表的所有元组都有相同的值）——或者可能是0。可以编造一个简单的启发式方法（比如说，两个表中较大的那个的大小）。
* 对于范围扫描，同样很难准确地说出大小。输出的大小应该与输入的大小成比例。可以假设范围扫描发出的交叉积的固定比例（比如说，30%）。一般来说，范围连接的成本应该大于相同大小的两个表的非主键等值连接的成本。

----------

**练习3: 连接成本估计**

[JoinOptimizer.java](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\optimizer\JoinOptimizer.java)类包括所有用于排序和计算连接成本的方法。在这个练习中，您将编写估计连接选择性和成本的方法，具体来说：

* 实现`estimateJoinCost(LogicalJoinNode j, int card1, int card2, double cost1, double cost2)`：此方法估计连接`j`的成本，给定左输入的基数为`card1`，右输入的基数为`card2`，扫描左输入的成本为`cost1`，访问右输入的成本为`card2`。您可以假设连接是NL连接，并应用前面提到的公式。
* 实现`estimateJoinCardinality(LogicalJoinNode j, int card1, int card2, boolean t1pkey, boolean t2pkey)`：此方法估计连接`j`输出的元组数，给定左输入大小为`card1`，右输入大小为`card2`，以及标志`t1pkey`和`t2pkey`，它们指示左和右（分别）字段是否唯一（主键）。

实现这些方法后，您应该能够通过[JoinOptimizerTest.java](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\test\simpledb\JoinOptimizerTest.java)中的单元测试[estimateJoinCostTest](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\test\simpledb\JoinOptimizerTest.java#L116-L154)和[estimateJoinCardinality](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\test\simpledb\JoinOptimizerTest.java#L220-L274)。

----------

### 2.3 连接排序

现在您已经实现了估计成本的方法，您将实现Selinger优化器。对于这些方法，连接被表示为连接节点列表（例如，两个表上的谓词），而不是课堂上描述的连接关系列表。

将讲座中给出的算法转换为上述连接节点列表形式，伪代码大纲将是：
```
1. j = 连接节点集合
2. for (i in 1...|j|):
3.     for s in {j的所有长度为i的子集}
4.       bestPlan = {}
5.       for s' in {s的所有长度d-1的子集}
6.            subplan = optjoin(s')
7.            plan = 将(s-s')连接到subplan的最佳方式
8.            if (cost(plan) < cost(bestPlan))
9.               bestPlan = plan
10.      optjoin(s) = bestPlan
11. return optjoin(j)
```


为了帮助您实现这个算法，我们提供了几个类和方法来协助您。首先，[JoinOptimizer.java](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\optimizer\JoinOptimizer.java)中的方法`enumerateSubsets(List<T> v, int size)`将返回`v`的所有大小为`size`的子集。对于大型集合，此方法非常低效；您可以通过实现更高效的枚举器来获得额外学分（提示：考虑使用就地生成算法和惰性迭代器（或流）接口来避免物化整个幂集）。

其次，我们提供了方法：
```java
private CostCard computeCostAndCardOfSubplan(Map<String, TableStats> stats,
                                             Map<String, Double> filterSelectivities,
                                             LogicalJoinNode joinToRemove,
                                             Set<LogicalJoinNode> joinSet,
                                             double bestCostSoFar,
                                             PlanCache pc)
```


给定连接的子集（`joinSet`）和要从此集合中移除的连接（`joinToRemove`），此方法计算将`joinToRemove`连接到`joinSet - {joinToRemove}`的最佳方式。它在[CostCard](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\optimizer\CostCard.java#L8-L21)对象中返回这个最佳方法，其中包括成本、基数和最佳连接顺序（作为列表）。如果找不到计划（例如，因为没有可能的左深连接），或者所有计划的成本都大于`bestCostSoFar`参数，[computeCostAndCardOfSubplan](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\optimizer\JoinOptimizer.java#L308-L424)可能返回`null`。该方法使用名为`pc`（上面伪代码中的`optjoin`）的先前连接缓存来快速查找连接`joinSet - {joinToRemove}`的最快方式。其他参数（`stats`和`filterSelectivities`）在您必须实现的[orderJoins](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\optimizer\JoinOptimizer.java#L243-L281)方法中传递，并在下面解释。此方法本质上执行前面描述的伪代码的第6-8行。

第三，我们提供了方法：
```java
private void printJoins(List<LogicalJoinNode> js,
                        PlanCache pc,
                        Map<String, TableStats> stats,
                        Map<String, Double> selectivities)
```


当通过优化器的"-explain"选项设置"explain"标志时，此方法可用于显示连接计划的图形表示。

第四，我们提供了一个类[PlanCache](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\execution\PlanCache.java#L13-L63)，可用于缓存到目前为止在您的Selinger实现中考虑的连接子集的最佳连接方式（使用[computeCostAndCardOfSubplan](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\optimizer\JoinOptimizer.java#L308-L424)需要此类的实例）。

----------

**练习4: 连接排序**

在[JoinOptimizer.java](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\optimizer\JoinOptimizer.java)中，实现方法：
```java
List<LogicalJoinNode> orderJoins(Map<String, TableStats> stats,
                                 Map<String, Double> filterSelectivities,
                                 boolean explain)
```


此方法应该在[joins](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\optimizer\LogicalPlan.java#L30-L30)类成员上操作，返回一个新`List`，指定应该执行连接的顺序。此列表的第0项表示左深计划中最左边、最底层的连接。返回列表中的相邻连接应该共享至少一个字段，以确保计划是左深的。这里的`stats`是一个对象，允许您找到出现在查询`FROM`列表中的给定表名的[TableStats](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\optimizer\TableStats.java#L23-L223)。`filterSelectivities`允许您找到表上任何谓词的选择性；它保证在`FROM`列表中的每个表名都有一个条目。最后，[explain](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\Parser.java#L25-L25)指定您应该输出连接顺序的表示以供信息目的。

您可能希望使用上面描述的辅助方法和类来协助您的实现。大致上，您的实现应该遵循上面的伪代码，循环遍历子集大小、子集和子集的子计划，调用[computeCostAndCardOfSubplan](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\optimizer\JoinOptimizer.java#L308-L424)并构建一个[PlanCache](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\execution\PlanCache.java#L13-L63)对象，该对象存储执行每个子集连接的最小成本方式。

实现此方法后，您应该能够通过[JoinOptimizerTest](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\test\simpledb\JoinOptimizerTest.java#L25-L631)中的所有单元测试。您还应该通过系统测试[QueryTest](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\test\simpledb\systemtest\QueryTest.java#L20-L166)。

### 2.4 额外学分

在本节中，我们描述了几个您可以实现的可选练习以获得额外学分。这些练习没有前面的练习定义得那么明确，但给您一个展示您掌握查询优化的机会！请在报告中清楚地标明您选择完成的练习，并简要解释您的实现并展示您的结果（基准数字、经验报告等）。

----------

**奖励练习。** 每个奖励最多可获得5%的额外学分：

* *添加代码以执行更高级的连接基数估计*。与其使用简单启发式来估计连接基数，不如设计更复杂的算法。
    - 一种选择是使用每对表*t1*和*t2*中每对属性*a*和*b*之间的联合直方图。想法是创建*a*的桶，并为*a*的每个桶*A*，创建与*A*中的*a*值共同出现的*b*值的直方图。
    - 估计连接基数的另一种方法是假设较小表中的每个值在较大表中都有匹配值。然后连接选择性的公式将是：1/(*Max*(*num-distinct*(t1, column1), *num-distinct*(t2, column2)))。这里，column1和column2是连接属性。连接的基数然后是*t1*和*t2*的基数乘以选择性。
* *改进的子集迭代器*。我们对[enumerateSubsets()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\optimizer\JoinOptimizer.java#L205-L225)的实现相当低效，因为它在每次调用时创建大量Java对象。在这个奖励练习中，您将提高[enumerateSubsets()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\optimizer\JoinOptimizer.java#L205-L225)的性能，使您的系统能够对具有20个或更多连接的计划执行查询优化（目前这样的计划需要几分钟或几小时来计算）。
* *考虑缓存的成本模型*。估计扫描和连接成本的方法没有考虑缓冲池中的缓存。您应该扩展成本模型以考虑缓存效应。这很棘手，因为由于迭代器模型，多个连接同时运行，因此可能很难使用我们在以前实验中实现的简单缓冲池预测每个连接将有多少内存访问。
* *改进的连接算法和算法选择*。我们当前的成本估计和连接操作符选择算法（参见[JoinOptimizer.java](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\optimizer\JoinOptimizer.java)中的[instantiateJoin()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\optimizer\JoinOptimizer.java#L45-L75)）只考虑嵌套循环连接。扩展这些方法以使用一种或多种额外的连接算法（例如，使用`HashMap`的某种内存哈希形式）。
* *丛生计划*。改进提供的[orderJoins()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\optimizer\JoinOptimizer.java#L243-L281)和其他辅助方法以生成丛生连接。我们的查询计划生成和可视化算法完全能够处理丛生计划；例如，如果[orderJoins()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\optimizer\JoinOptimizer.java#L243-L281)返回列表(t1 join t2 ; t3 join t4 ; t2 join t3)，这将对应于顶部有(t2 join t3)节点的丛生计划。

----------

您现在已经完成了这个实验。干得好！

## 3. 后勤
您必须提交您的代码（见下文）以及描述您的方法的简短（最多2页）报告。这个报告应该：

* 描述您做出的任何设计决策，包括选择性估计、连接排序的方法，以及您选择实现的任何奖励练习以及您如何实现它们（对于每个奖励练习，您可以提交最多1页的额外内容）。
* 讨论并证明您对API所做的任何更改。
* 描述您的代码中任何缺失或不完整的元素。
* 描述您在实验上花费的时间，以及是否有任何您觉得特别困难或令人困惑的地方。
* 描述您完成的任何额外学分实现。

### 3.1. 协作
这个实验一个人应该可以管理，但如果您更喜欢与合作伙伴一起工作，这也是可以的。不允许更大的小组。请在您的报告中清楚地说明您与谁合作（如果有的话）。

### 3.2. 提交您的作业
我们将使用Gradescope对所有编程作业进行自动评分。您应该都已受邀加入班级实例；如果没有，请告诉我们，我们可以帮助您设置。您可以在截止日期前多次提交代码；我们将使用Gradescope确定的最新版本。将报告放在名为lab3-writeup.txt的文件中与您的提交一起。您还需要显式添加您创建的任何其他文件，如新的*.java文件。

如果您与合作伙伴合作，只有一个人需要提交到Gradescope。但是，请确保将其他人添加到您的小组中。还要注意，每个成员都必须有自己的报告。请在文件名和报告本身中添加您的Kerberos用户名（例如，`lab3-writeup-username1.txt`和`lab3-writeup-username2.txt`）。

提交到gradescope的最简单方法是使用包含您的代码的`.zip`文件。在Linux/macOS上，您可以通过运行以下命令来实现：

```bash
$ zip -r submission.zip src/ lab3-writeup.txt

# 如果您与合作伙伴合作：
$ zip -r submission.zip src/ lab3-writeup-username1.txt lab3-writeup-username2.txt
```


<a name="bugs"></a>
### 3.3. 提交错误

SimpleDB是一个相对复杂的代码。您很可能会发现错误、不一致以及过时或不正确的文档等。

因此，我们要求您以冒险的心态来做这个实验。如果有什么不清楚或甚至是错误的，不要生气；相反，试着自己弄清楚或给我们发送一封友好的电子邮件。

请将（友好的！）错误报告提交到<a href="mailto:6.5830-staff@mit.edu">6.5830-staff@mit.edu</a>。当您这样做时，请尝试包括：

* 错误的描述。
* 我们可以放在`test/simpledb`目录中、编译和运行的[.java](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\common\Database.java)文件。
* 重现错误的数据的[.txt](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\bin\depcache\dependencies.txt)文件。我们应该能够使用[HeapFileEncoder](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\HeapFileEncoder.java#L17-L227)将其转换为`.dat`文件。

如果您觉得遇到了错误，也可以在Piazza的班级页面上发帖。

### 3.4 评分
您的成绩75%将基于您的代码是否通过我们将对其运行的系统测试套件。这些测试将是我们提供的测试的超集。在提交代码之前，您应该确保从`ant test`和`ant systemtest`中不会产生错误（通过所有测试）。

**重要：** 在测试之前，Gradescope将用我们的版本替换您的[build.xml](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\build.xml)、[HeapFileEncoder.java](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\HeapFileEncoder.java)、[Parser.java](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\Parser.java)和`test`目录的全部内容。这意味着您不能更改`.dat`文件的格式，也不能依赖[Parser.java](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\Parser.java)中的任何自定义功能。您也应该小心更改我们的API。您应该测试您的代码是否编译未修改的测试。

提交后，您应该立即从Gradescope获得反馈和失败测试的错误输出（如果有的话）。给出的分数将是您作业自动评分部分的成绩。另外25%的成绩将基于您的报告质量和我们对您代码的主观评估。这部分也将在我们完成评分您的作业后在Gradescope上发布。

我们在设计这个作业时有很多乐趣，我们希望您享受破解它的过程！