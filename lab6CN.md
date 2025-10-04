# 6.5831 实验六：回滚和恢复

**发布时间：** 2022年11月28日星期一

**截止日期：** 2022年12月14日星期三晚上11:59（东部时间）

**注意：** 本实验是针对选修6.5831（本科版本课程）且**不**做期末项目的同学。选修6.5830（研究生版本课程）的同学不需要完成本实验。

**注意：** 本实验不能使用延期提交。

## 0. 简介

在本实验中，您将实现基于日志的事务中止回滚和基于日志的崩溃恢复。我们为您提供定义日志格式的代码，并在事务执行的适当时机将记录追加到日志文件中。您将使用日志文件的内容来实现回滚和恢复功能。

我们提供的日志代码生成的记录适用于物理整页撤销(UNDO)和重做(REDO)操作。当页面首次被读入时，我们的代码会记住页面的原始内容作为"前镜像(before-image)"。当事务更新页面时，相应的日志记录包含记住的前镜像以及修改后页面的内容作为"后镜像(after-image)"。您将使用前镜像在中止时进行回滚和在恢复过程中撤销失败事务的操作，使用后镜像在恢复过程中重做成功事务的操作。

我们能够执行整页物理UNDO（而ARIES必须执行逻辑UNDO），因为我们使用页级锁定，并且我们没有索引，这些索引在UNDO时可能具有与最初写入日志时不同的结构。页级锁定简化了操作的原因是，如果一个事务修改了页面，它必须拥有该页面的独占锁，这意味着没有其他事务同时修改它，所以我们可以通过覆盖整个页面来UNDO更改。

您的 [BufferPool](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\BufferPool.java#L27-L345) 已经通过删除脏页实现了中止功能，并通过仅在提交时强制将脏页写入磁盘来模拟原子提交。日志记录允许更灵活的缓冲区管理（STEAL和NO-FORCE），我们的测试代码在某些点调用 [BufferPool.flushAllPages()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\BufferPool.java#L260-L269) 来测试这种灵活性。

## 1. 开始

您应该从Lab 5提交的代码开始（如果您没有提交Lab 5的代码，或者您的解决方案不能正常工作，请联系我们讨论选项）。

您需要修改一些现有的源代码并添加几个新文件。以下是具体操作：

* 首先切换到您的项目目录（可能叫 `simple-db-hw-2022`）并从上游GitHub仓库拉取：

  ```bash
  $ cd simple-db-hw-2022
  $ git pull upstream main
  ```


* 现在，对现有代码进行以下修改：
    1. 在 [BufferPool.flushPage()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\BufferPool.java#L298-L308) 中调用 [writePage(p)](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\DbFile.java#L35-L35) 之前插入以下行，其中 [p](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\execution\Join.java#L26-L26) 是对正在写入页面的引用：
    ```java
        // 向日志追加更新记录，包含
        // 前镜像和后镜像。
        TransactionId dirtier = p.isDirty();
        if (dirtier != null){
          Database.getLogFile().logWrite(dirtier, p.getBeforeImage(), p);
          Database.getLogFile().force();
        }
    ```

  这会使日志系统向日志写入更新记录。我们强制刷新日志以确保在页面写入磁盘之前日志记录已经在磁盘上。


2. 您的 `BufferPool.transactionComplete()` 会对每个已提交事务修改的页面调用 `flushPage()`。对于每个这样的页面，在刷新页面后添加调用 `p.setBeforeImage()`：
   ```java
   // 使用当前页面内容作为下一个修改此页面的事务的前镜像
   // 
   p.setBeforeImage();
   ```

   更新提交后，页面的前镜像需要更新，以便后续中止的事务可以回滚到这个已提交版本的页面。（注意：我们不能只在 [flushPage()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\BufferPool.java#L298-L308) 中调用 [setBeforeImage()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\Page.java#L60-L60)，因为即使事务没有提交，也可能调用 [flushPage()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\BufferPool.java#L298-L308)。我们的测试用例实际上就是这样做的！如果您通过调用 [flushPages()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\BufferPool.java#L313-L322) 来实现 [transactionComplete()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\transaction\Transaction.java#L52-L71)，您可能需要向 [flushPages()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\BufferPool.java#L313-L322) 传递额外的参数来告诉它刷新是否是为提交的事务进行的。但是，在这种情况下我们强烈建议您简单地重写 [transactionComplete()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\transaction\Transaction.java#L52-L71) 来使用 [flushPage()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\BufferPool.java#L298-L308)。）

* 完成这些更改后，进行一次干净的构建（从命令行运行 `ant clean; ant`，或在Eclipse的"Project"菜单中选择"Clean"）。

* 此时您的代码应该能通过 [LogTest](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\test\simpledb\systemtest\LogTest.java#L22-L441) 系统测试的前三个子测试，并且其余测试会失败：
  ```bash
  $ ant runsystest -Dtest=LogTest
    ...
    [junit] Running simpledb.systemtest.LogTest
    [junit] Testsuite: simpledb.systemtest.LogTest
    [junit] Tests run: 10, Failures: 0, Errors: 7, Time elapsed: 0.42 sec
    [junit] Tests run: 10, Failures: 0, Errors: 7, Time elapsed: 0.42 sec
    [junit] 
    [junit] Testcase: PatchTest took 0.057 sec
    [junit] Testcase: TestFlushAll took 0.022 sec
    [junit] Testcase: TestCommitCrash took 0.018 sec
    [junit] Testcase: TestAbort took 0.03 sec
    [junit]     Caused an ERROR
    [junit] LogTest: tuple present but shouldn't be
    ...
  ```


* 如果您在运行 `ant runsystest -Dtest=LogTest` 时没有看到上述输出，说明拉取新文件时出了问题，或者您所做的更改与现有代码不兼容。您应该在继续之前找出并修复问题；如有必要请向我们寻求帮助。

## 2. 回滚

阅读 [LogFile.java](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\LogFile.java) 中的注释以了解日志文件格式的描述。您应该在 [LogFile.java](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\LogFile.java) 中看到一组函数，如 [logCommit()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\LogFile.java#L184-L195)，这些函数生成各种类型的日志记录并将其追加到日志中。

您的第一个任务是在 [LogFile.java](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\LogFile.java) 中实现 [rollback()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\LogFile.java#L470-L478) 函数。当事务中止时，在事务释放锁之前会调用此函数。其任务是撤销事务可能对数据库所做的任何更改。

您的 [rollback()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\LogFile.java#L470-L478) 应该读取日志文件，找到与中止事务关联的所有更新记录，从每条记录中提取前镜像，并将前镜像写入表文件。使用 `raf.seek()` 在日志文件中移动，使用 `raf.readInt()` 等方法检查它。使用 [readPageData()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\LogFile.java#L258-L296) 读取每个前镜像和后镜像。您可以使用 [tidToFirstLogRecord](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\LogFile.java#L98-L98) 映射（将事务ID映射到堆文件中的偏移量）来确定从何处开始读取特定事务的日志文件。您需要确保从缓冲池中丢弃任何您将其前镜像写回到表文件的页面。

在开发代码时，您可能会发现 `Logfile.print()` 方法对于显示日志的当前内容很有用。

----------

**练习1：LogFile.rollback()**

实现 [LogFile.rollback()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\LogFile.java#L470-L478)。

完成此练习后，您应该能够通过 [LogTest](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\test\simpledb\systemtest\LogTest.java#L22-L441) 系统测试中的 [TestAbort](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\test\simpledb\systemtest\LogTest.java#L200-L218) 和 [TestAbortCommitInterleaved](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\test\simpledb\systemtest\LogTest.java#L220-L251) 子测试。

----------

## 3. 恢复

如果数据库崩溃然后重启，将在任何新事务开始之前调用 [LogFile.recover()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\LogFile.java#L500-L507)。您的实现应该：

1. 读取最后一个检查点（如果有的话）。

2. 从检查点（如果没有检查点则从日志文件开始）向前扫描以构建失败事务集合。在此过程中重做更新操作。您可以安全地从检查点开始重做，因为 [LogFile.logCheckpoint()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\LogFile.java#L323-L361) 会将所有脏缓冲区刷新到磁盘。

3. 撤销失败事务的更新操作。

----------

**练习2：LogFile.recover()**

实现 [LogFile.recover()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\LogFile.java#L500-L507)。

完成此练习后，您应该能够通过所有的 [LogTest](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\test\simpledb\systemtest\LogTest.java#L22-L441) 系统测试。

----------

## 4. 提交事项

您必须提交代码（见下文）以及一份简短（最多1页）的说明文档，描述您的方法。说明文档应包括：

* 描述您做出的任何设计决策，包括任何困难或意外的情况。

* 讨论并证明您在 [LogFile.java](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\LogFile.java) 之外所做的任何更改的合理性。

### 4.1. 合作

这个实验单人应该可以完成，但如果您更愿意与合作伙伴一起工作，这也是可以的。不允许更大的小组。如果有人合作，请在说明文档中清楚地表明。

### 4.2. 提交作业

我们将使用Gradescope自动评分所有编程作业。您应该都已受邀加入班级实例；如果没有，请告知我们，我们可以帮助您设置。在截止日期前您可以多次提交代码；我们将使用Gradescope确定的最新版本。将说明文档放在名为 lab6-writeup.txt 的文件中与您的提交一起提交。您还需要显式添加您创建的任何其他文件，如新的 `*.java` 文件。

如果您与合作伙伴一起工作，只需要一个人提交到Gradescope。但是，请确保将其他人添加到您的小组。另请注意，每个成员都必须有自己的说明文档。请在文件名和说明文档本身中添加您的Kerberos用户名（例如，`lab6-writeup-username1.txt` 和 `lab6-writeup-username2.txt`）。

提交到Gradescope的最简单方法是使用包含您代码的 `.zip` 文件。在Linux/macOS上，您可以通过运行以下命令来实现：

```bash
$ zip -r submission.zip src/ lab6-writeup.txt

# 如果您与合作伙伴一起工作：
$ zip -r submission.zip src/ lab6-writeup-username1.txt lab6-writeup-username2.txt
```


<a name="bugs"></a>
### 4.3. 提交错误

SimpleDB是一段相对复杂的代码。您很可能会发现错误、不一致之处、过时或不正确的文档等。

因此，我们要求您以探索的心态完成这个实验。如果某些内容不清楚，甚至是错误的，不要生气；而是尝试自己解决或给我们发送友好的邮件。请向 <a href="mailto:6.5830-staff@mit.edu">6.5830-staff@mit.edu</a> 提交（友好的！）错误报告。当您提交时，请尽量包括：

* 错误的描述。

* 一个我们可以放入 `src/simpledb/test` 目录的 [.java](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\Parser.java) 文件，可以编译和运行。

* 一个包含重现错误数据的 [.txt](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\bin\depcache\dependencies.txt) 文件。我们应该能够使用 `PageEncoder` 将其转换为 `.dat` 文件。

如果您认为遇到了错误，也可以在Piazza的班级页面上发帖。

<a name="grading"></a>
### 4.4 评分

您的成绩75%将基于您的代码是否通过我们将运行的系统测试套件。这些测试将是我们提供的测试的超集。在提交代码之前，您应该确保它在 `ant test` 和 `ant systemtest` 中都不产生错误（通过所有测试）。

**重要：** 在测试之前，Gradescope将用我们的版本替换您的 [build.xml](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\build.xml)、[HeapFileEncoder.java](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\HeapFileEncoder.java)、[Parser.java](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\Parser.java) 和 `test` 目录的全部内容。这意味着您不能更改 `.dat` 文件的格式，也不能依赖 [Parser.java](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\Parser.java) 中的任何自定义功能！您也应该小心更改我们的API。您应该测试您的代码是否能编译未修改的测试。

提交后，您应该能从Gradescope获得即时反馈和失败测试的错误输出（如果有的话）。给出的分数将是您作业自动评分部分的成绩。另外25%的成绩将基于您的说明文档质量和我们对您代码的主观评价。这部分也将在我们完成评分后在Gradescope上公布。

我们设计这个作业时很有趣，希望您在编程时也能享受其中！