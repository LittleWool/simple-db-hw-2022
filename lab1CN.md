# 6.5830/6.5831 实验1: SimpleDB

**发布日期:** 2022年9月14日星期三

**截止日期:** 2022年9月28日星期三晚上11:59 ET

在6.5830/6.5831的实验作业中，您将编写一个名为SimpleDB的基本数据库管理系统。在本次实验中，您将专注于实现访问磁盘上存储数据所需的核心模块；在未来的实验中，您将添加对各种查询处理操作符、事务、锁定和并发查询的支持。

SimpleDB是用Java编写的。我们为您提供了一组大部分未实现的类和接口。您需要为这些类编写代码。我们将通过运行一组使用[JUnit](http://junit.sourceforge.net/)编写的系统测试来评分您的代码。我们还提供了一些单元测试，虽然我们不会用于评分，但您可能会发现它们在验证代码是否正常工作时很有用。我们也鼓励您开发自己的测试套件来补充我们的测试。

本文档的其余部分描述了SimpleDB的基本架构，提供了一些关于如何开始编码的建议，并讨论了如何提交您的实验。

我们**强烈建议**您尽早开始这个实验。它需要您编写相当多的代码！

## 0. 环境设置

**首先通过以下说明从课程GitHub仓库下载实验1的代码。**

---

**更新 (2022年9月26日):** 如果您在9月26日或之后开始这个实验，您从这个仓库获得的代码将包括后续实验的起始代码和测试。要获得仅用于实验1的代码和测试子集，请切换到`lab1`分支(`git checkout lab1`)。

---

这些说明是为Athena或任何其他基于Unix的平台（例如，Linux、macOS等）编写的。由于代码是用Java编写的，在Windows下也应该可以工作，尽管本文档中的说明可能不适用。

除非您在Athena上运行，否则您还需要确保安装了Java开发工具包（JDK）。SimpleDB至少需要Java 8（注意"Java 8"和"Java 1.8"指的是同一版本的Java——前者用于品牌目的）。该项目已配置为默认使用Java 11。如果您运行的是较旧版本的Java，您需要更新[build.xml](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\build.xml)中的以下行：

```xml
<!-- 更新下面的值以匹配您的版本（例如，"1.8"）。 -->
<property name="sourceversion" value="11"/>
```


**我们的自动评分器使用Java 11，所以请不要使用Java 11之后的新特性（您不需要它们来完成实验）。**

您可以按以下方式安装JDK：
- 在macOS上: `brew install openjdk@11`
- 在Ubuntu（或WSL）上: `sudo apt install openjdk-11-jdk`
- 在Windows上: 参见这个[指南](https://docs.microsoft.com/en-us/java/openjdk/install)。

我们还包含了[第1.2节](#ides)关于在IntelliJ、VSCode或Eclipse中使用项目的说明。

## 1. 入门指南

SimpleDB使用[Ant构建工具](http://ant.apache.org/)来编译代码和运行测试。Ant类似于[make](http://www.gnu.org/software/make/manual/)，但构建文件是用XML编写的，更适合Java代码。大多数现代Linux发行版都包含Ant。在Athena上，它包含在`sipb`储物柜中，您可以通过在Athena提示符下输入`add sipb`来访问。请注意，在某些版本的Athena上，您还必须运行`add -f java`来正确设置Java程序的环境。有关更多详细信息，请参见[使用Java的Athena文档](http://web.mit.edu/java/www/)。

为了在开发过程中帮助您，我们提供了一组单元测试，除了我们用于评分的端到端测试。这些绝不是全面的，您不应该仅仅依赖它们来验证项目的正确性（发挥您6.1040（前6.170）的技能！）。

要运行单元测试，请使用`test`构建目标：

```
$ cd [项目目录]
$ # 运行所有单元测试
$ ant test
$ # 运行特定的单元测试
$ ant runtest -Dtest=TupleTest
```


您应该看到类似的输出：

```
 构建输出...

test:
    [junit] 运行 simpledb.CatalogTest
    [junit] 测试套件: simpledb.CatalogTest
    [junit] 运行测试: 2, 失败: 0, 错误: 2, 耗时: 0.037 秒
    [junit] 运行测试: 2, 失败: 0, 错误: 2, 耗时: 0.037 秒

 ... 堆栈跟踪和错误报告 ...
```


上面的输出表明在编译过程中发生了两个错误；这是因为我们给您的代码还不能正常工作。当您完成实验的各个部分时，您将逐步通过额外的单元测试。

如果您希望在编码时编写新的单元测试，它们应该添加到`test/simpledb`目录中。

有关如何使用Ant的更多详细信息，请参见[手册](http://ant.apache.org/manual/)。[运行Ant](http://ant.apache.org/manual/running.html)部分提供了使用`ant`命令的详细信息。但是，下面的快速参考表应该足以用于实验。

命令 | 描述
--- | ---
ant|构建默认目标（对于simpledb，这是dist）。
ant -projecthelp|列出[build.xml](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\build.xml)中所有带有描述的目标。
ant dist|编译src中的代码并打包到`dist/simpledb.jar`中。
ant test|编译并运行所有单元测试。
ant runtest -Dtest=testname|运行名为`testname`的单元测试。
ant systemtest|编译并运行所有系统测试。
ant runsystest -Dtest=testname|编译并运行名为`testname`的系统测试。

如果您使用Windows系统并且不想从命令行运行ant测试，也可以从eclipse运行它们。右键单击build.xml，在目标选项卡中，您可以看到"runtest" "runsystest"等。例如，选择runtest相当于从命令行运行"ant runtest"。参数如"-Dtest=testname"可以在"Main"选项卡的"Arguments"文本框中指定。请注意，您也可以通过从build.xml复制、修改目标和参数并重命名为runtest_build.xml来创建运行测试的快捷方式。

### 1.1. 运行端到端测试

我们还提供了一组最终将用于评分的端到端测试。这些测试结构为JUnit测试，位于`test/simpledb/systemtest`目录中。要运行所有系统测试，请使用`systemtest`构建目标：

```
$ ant systemtest

 ... 构建输出 ...

    [junit] 测试用例: testSmall 耗时 0.017 秒
    [junit]     导致错误
    [junit] 预期找到以下元组:
    [junit]     19128
    [junit] 
    [junit] java.lang.AssertionError: 预期找到以下元组:
    [junit]     19128
    [junit] 
    [junit]     at simpledb.systemtest.SystemTestUtil.matchTuples(SystemTestUtil.java:122)
    [junit]     at simpledb.systemtest.SystemTestUtil.matchTuples(SystemTestUtil.java:83)
    [junit]     at simpledb.systemtest.SystemTestUtil.matchTuples(SystemTestUtil.java:75)
    [junit]     at simpledb.systemtest.ScanTest.validateScan(ScanTest.java:30)
    [junit]     at simpledb.systemtest.ScanTest.testSmall(ScanTest.java:40)

 ... 更多错误信息 ...
```


这表明该测试失败了，显示了检测到错误的堆栈跟踪。要调试，请首先阅读发生错误的源代码。当测试通过时，您将看到类似以下内容：

```
$ ant systemtest

 ... 构建输出 ...

    [junit] 测试套件: simpledb.systemtest.ScanTest
    [junit] 运行测试: 3, 失败: 0, 错误: 0, 耗时: 7.278 秒
    [junit] 运行测试: 3, 失败: 0, 错误: 0, 耗时: 7.278 秒
    [junit] 
    [junit] 测试用例: testSmall 耗时 0.937 秒
    [junit] 测试用例: testLarge 耗时 5.276 秒
    [junit] 测试用例: testRandom 耗时 1.049 秒

构建成功
总耗时: 52 秒
```


#### 1.1.1 创建虚拟表

您可能希望创建自己的测试和自己的数据表来测试您自己的SimpleDB实现。您可以创建任何`.txt`文件，并使用以下命令将其转换为SimpleDB的`HeapFile`格式的`.dat`文件：

```
$ java -jar dist/simpledb.jar convert file.txt N
```


其中`file.txt`是文件名，`N`是文件中的列数。请注意，`file.txt`必须采用以下格式：

```
int1,int2,...,intN
int1,int2,...,intN
int1,int2,...,intN
int1,int2,...,intN
```


...其中每个`intN`都是非负整数。

要查看表的内容，请使用`print`命令：

```
$ java -jar dist/simpledb.jar print file.dat N
```


其中`file.dat`是使用[convert](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\HeapFileEncoder.java#L58-L63)命令创建的表的名称，`N`是文件中的列数。

<a name="ides"></a>

### 1.2. 使用IDE

IDE（集成开发环境）是图形化软件开发环境，可能有助于您管理大型项目。我们推荐使用[IntelliJ](https://www.jetbrains.com/idea/)或[VSCode](https://code.visualstudio.com/)。如果您喜欢冒险，也可以尝试[Eclipse](https://www.eclipse.org/ide/)。

我们提供了设置[IntelliJ](https://www.jetbrains.com/idea/)、[VSCode](https://code.visualstudio.com)和[Eclipse](http://www.eclipse.org)的说明。对于IntelliJ，我们使用的是终极版，您可以通过您的mit.edu账户[这里](https://www.jetbrains.com/community/education/#students)获得教育许可证。我们为Eclipse提供的说明是使用Java开发者版（不是企业版）和Java 1.7生成的。我们强烈建议您为这个项目设置并学习其中一个IDE。

#### 1.2.1. 设置IntelliJ

IntelliJ是一个现代Java IDE，因其更直观而受到一些用户的欢迎。要使用IntelliJ，首先安装它并打开应用程序。在Projects下，选择Open并导航到您的项目根目录。双击`.project`文件（您可能需要配置操作系统以显示隐藏文件才能看到它），然后点击"open as project"。IntelliJ具有Ant的工具窗口支持，您可以根据[这里](https://www.jetbrains.com/help/idea/ant.html)的说明进行设置，但这对开发来说不是必需的。您可以在[这里](https://www.jetbrains.com/help/idea/discover-intellij-idea.html)找到IntelliJ功能的详细演练。

#### 1.2.2. 设置VSCode

VSCode是一个流行的免费可扩展代码编辑器，支持多种语言，包括Java。您可以在[这里](https://code.visualstudio.com/docs/setup/setup-overview)找到安装说明。

安装VSCode后，您还需要安装[Java Extension Pack](https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-java-pack)以获得Java语言功能（例如，调试器、自动完成等）。为此，首先点击左侧边栏的"blocks"图标（Windows/Linux上按Ctrl-Shift-X或macOS上按⌘-Shift-X）。然后，搜索"java extension pack"并在看到扩展包时点击安装（它应该是第一个结果）。

最后，要使用SimpleDB，点击File > Open Folder并导航到您克隆此仓库的文件夹。项目应该加载，您应该能够看到代码。首次打开项目时，VSCode可能会询问您是否[信任项目](https://code.visualstudio.com/docs/editor/workspace-trust)。您需要点击是才能获得项目的所有VSCode功能。

#### 1.2.3. 设置Eclipse

**准备代码库**

运行以下命令为IDE生成项目文件：

```
ant eclipse
```


**在Eclipse中设置实验**

* 安装Eclipse后，启动它，注意第一个屏幕会要求您为工作区选择一个位置（我们将把这个目录称为$W）。选择包含您的simple-db-hw仓库的目录。
* 在Eclipse中，选择File->New->Project->Java->Java Project，然后点击Next。
* 输入"simple-db-hw"作为项目名称。
* 在输入项目名称的同一屏幕上，选择"Create project from existing source"，然后浏览到$W/simple-db-hw。
* 点击finish，您应该能够在屏幕左侧的Project Explorer选项卡中看到"simple-db-hw"作为一个新项目。打开这个项目会显示上面讨论的目录结构——实现代码可以在"src"中找到，单元测试和系统测试在"test"中找到。

**注意:** 本课程假设您使用的是官方Oracle发布的Java。这是macOS上的默认设置，大多数Windows Eclipse安装也是如此；但许多Linux发行版默认使用替代的Java运行时（如OpenJDK）。请从[Oracle网站](http://www.oracle.com/technetwork/java/javase/downloads/index.html)下载最新的Java 8更新，使用该Java版本。如果您不切换，您可能会在后续实验的一些性能测试中看到虚假的测试失败。

**运行单个单元和系统测试**

要运行单元测试或系统测试（两者都是JUnit测试，可以以相同方式初始化），请转到屏幕左侧的Package Explorer选项卡。在"simple-db-hw"项目下，打开"test"目录。单元测试在"simpledb"包中找到，系统测试在"simpledb.systemtests"包中找到。要运行这些测试之一，请选择测试（它们都叫*Test.java* - 不要选择TestUtil.java或SystemTestUtil.java），右键单击它，选择"Run As"，然后选择"JUnit Test"。这将打开一个JUnit选项卡，它会告诉您JUnit测试套件中各个测试的状态，并会显示帮助您调试问题的异常和其他错误。

**运行Ant构建目标**

如果您想运行诸如"ant test"或"ant systemtest"之类的命令，请在Package Explorer中右键单击build.xml。选择"Run As"，然后选择"Ant Build..."（注意：选择带省略号(...)的选项，否则您不会看到要运行的构建目标集）。然后，在下一个屏幕的"Targets"选项卡中，勾选您想要运行的目标（可能是"dist"和"test"或"systemtest"中的一个）。这应该运行构建目标并在Eclipse的控制台窗口中显示结果。

### 1.3. 实现提示

在开始编写代码之前，我们**强烈建议**您通读整个文档以了解SimpleDB的高级设计。

您需要填写任何未实现的代码部分。很明显我们认为您应该在哪里编写代码。您可能需要添加私有方法和/或辅助类。您可以更改API，但请确保我们的[评分](#grading)测试仍然运行，并确保在您的writeup中提及、解释和辩护您的决定。

除了您需要为此实验填写的方法外，类接口还包含许多您在后续实验之前不需要实现的方法。这些将按类指示：

```java
// 实验1不需要。
public class Insert implements DbIterator {
}
```


或按方法指示：

```Java
public boolean deleteTuple(Tuple t)throws DbException{
    // TODO: 一些代码在这里
    // 实验1不需要
    return false;
}
```


您提交的代码应该在不修改这些方法的情况下编译。

我们建议您按照本文档中的练习来指导您的实现，但您可能会发现不同的顺序对您更有意义。

**以下是您可能进行SimpleDB实现的一个粗略大纲：**

---

* 实现管理元组的类，即[Tuple](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\Tuple.java#L15-L148)、[TupleDesc](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\TupleDesc.java#L11-L253)。我们已经为您实现了[Field](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\Field.java#L12-L48)、[IntField](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\IntField.java#L11-L85)、[StringField](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\StringField.java#L11-L114)和[Type](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\common\Type.java#L16-L70)。由于您只需要支持整数和（固定长度）字符串字段以及固定长度元组，这些都很简单。
* 实现[Catalog](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\common\Catalog.java#L23-L207)（这应该非常简单）。
* 实现[BufferPool](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\BufferPool.java#L27-L346)构造函数和[getPage()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\BufferPool.java#L89-L101)方法。
* 实现访问方法，[HeapPage](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\HeapPage.java#L17-L446)和[HeapFile](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\HeapFile.java#L23-L315)以及相关的ID类。这些文件的很大一部分已经为您编写好了。
* 实现操作符[SeqScan](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\execution\SeqScan.java#L17-L160)。
* 此时，您应该能够通过[ScanTest](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\test\simpledb\systemtest\ScanTest.java#L27-L145)系统测试，这是本次实验的目标。

---

第2节下面更详细地引导您完成这些实现步骤和相应的单元测试。

### 1.4. 事务、锁定和恢复

当您查看我们提供的接口时，您会看到许多关于锁定、事务和恢复的引用。您在此实验中不需要支持这些功能，但您应该在代码接口中保留这些参数，因为您将在未来的实验中实现事务和锁定。我们提供的测试代码会生成一个假的事务ID，该ID被传递到它运行的查询的操作符中；您应该将此事务ID传递给其他操作符和缓冲池。

## 2. SimpleDB架构和实现指南

SimpleDB包括：

* 表示字段、元组和元组模式的类；
* 将谓词和条件应用于元组的类；
* 一种或多种访问方法（例如，堆文件），它们将关系存储在磁盘上并提供遍历这些关系元组的方法；
* 一组操作符类（例如，选择、连接、插入、删除等）来处理元组；
* 缓冲池，在内存中缓存活动元组和页面并处理并发控制和事务（您在此实验中不需要担心这些）；以及，
* 存储有关可用表及其模式信息的目录。

SimpleDB不包括许多您可能认为是"数据库"一部分的东西。特别是，SimpleDB没有：

* （在此实验中）SQL前端或解析器，允许您直接在SimpleDB中输入查询。相反，查询是通过将一组操作符链接在一起形成手工构建的查询计划来构建的（参见[第2.7节](#query_walkthrough)）。我们将在后续实验中提供一个简单的解析器。
* 视图。
* 除整数和固定长度字符串以外的数据类型。
* （在此实验中）查询优化器。
* （在此实验中）索引。

在本节的其余部分中，我们描述了您在此实验中需要实现的SimpleDB的主要组件。您应该使用本讨论中的练习来指导您的实现。本文档绝不是SimpleDB的完整规范；您需要对系统的各个部分的设计和实现做出决定。请注意，对于实验1，您不需要实现任何操作符（例如，选择、连接、投影），除了顺序扫描。您将在未来的实验中添加对其他操作符的支持。

### 2.1. 数据库类

[Database](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\common\Database.java#L19-L84)类提供了访问作为数据库全局状态的静态对象集合。特别是，这包括访问目录（数据库中所有表的列表）、缓冲池（当前驻留在内存中的数据库文件页面集合）和日志文件的方法。您在此实验中不需要担心日志文件。我们已经为您实现了[Database](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\common\Database.java#L19-L84)类。您应该看一下这个文件，因为您将需要访问这些对象。

### 2.2. 字段和元组

SimpleDB中的元组非常基础。它们由[Field](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\Field.java#L12-L48)对象集合组成，每个字段对应一个[Tuple](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\Tuple.java#L15-L148)中的字段。[Field](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\Field.java#L12-L48)是一个接口，不同的数据类型（例如，整数、字符串）实现它。[Tuple](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\Tuple.java#L15-L148)对象由底层访问方法（例如，堆文件或B树）创建，如下一节所述。元组还有一个类型（或模式），称为_tuple descriptor_，由[TupleDesc](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\TupleDesc.java#L11-L253)对象表示。该对象由[Type](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\common\Type.java#L16-L70)对象集合组成，每个字段对应一个，每个都描述相应字段的类型。

### 练习1

**实现以下骨架方法：**

---
* src/java/simpledb/storage/TupleDesc.java
* src/java/simpledb/storage/Tuple.java
---

此时，您的代码应该通过[TupleTest](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\test\simpledb\TupleTest.java#L11-L67)和[TupleDescTest](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\test\simpledb\TupleDescTest.java#L15-L181)单元测试。此时，[modifyRecordId()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\test\simpledb\TupleTest.java#L45-L59)应该失败，因为您还没有实现它。

### 2.3. 目录

目录（SimpleDB中的[Catalog](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\common\Catalog.java#L23-L207)类）由当前在数据库中的表及其模式的列表组成。您需要支持添加新表以及获取特定表信息的能力。与每个表关联的是一个[TupleDesc](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\TupleDesc.java#L11-L253)对象，它允许操作符确定表中的字段类型和数量。

全局目录是为整个SimpleDB进程分配的单个[Catalog](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\common\Catalog.java#L23-L207)实例。可以通过[Database.getCatalog()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\common\Database.java#L58-L60)方法检索全局目录，缓冲池也是如此（使用[Database.getBufferPool()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\common\Database.java#L51-L53)）。

### 练习2

**实现以下骨架方法：**

---
* src/java/simpledb/common/Catalog.java
--- 

此时，您的代码应该通过[CatalogTest](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\test\simpledb\CatalogTest.java#L21-L105)中的单元测试。

### 2.4. 缓冲池

缓冲池（SimpleDB中的[BufferPool](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\BufferPool.java#L27-L346)类）负责缓存最近从磁盘读取的页面到内存中。所有操作符都通过缓冲池从磁盘上的各种文件读取和写入页面。它由固定数量的页面组成，由传递给[BufferPool](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\BufferPool.java#L27-L346)构造函数的[numPages](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreeFile.java#L157-L160)参数定义。在后续实验中，您将实现驱逐策略。对于此实验，您只需要实现构造函数和[BufferPool.getPage()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\BufferPool.java#L89-L101)方法，该方法由SeqScan操作符使用。缓冲池应该存储最多[numPages](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreeFile.java#L157-L160)页面。对于此实验，如果有超过[numPages](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreeFile.java#L157-L160)个不同页面的请求，则不是实现驱逐策略，而是可以抛出DbException。在未来的实验中，您将需要实现驱逐策略。

[Database](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\common\Database.java#L19-L84)类提供了一个静态方法[Database.getBufferPool()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\common\Database.java#L51-L53)，它返回对整个SimpleDB进程的单个BufferPool实例的引用。

### 练习3

**实现以下方法：**

---
* src/java/simpledb/storage/BufferPool.java
---

我们没有为[BufferPool](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\BufferPool.java#L27-L346)提供单元测试。您实现的功能将在下面的[HeapFile](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\HeapFile.java#L23-L315)实现中进行测试。您应该使用[DbFile.readPage](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\DbFile.java#L27-L27)方法来访问[DbFile](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\DbFile.java#L21-L99)的页面。

### 2.5. [HeapFile](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\HeapFile.java#L23-L315)访问方法

访问方法提供了一种以特定方式从磁盘读取或写入数据的方法。常见的访问方法包括堆文件（未排序的元组文件）和B树；对于此作业，您只需要实现堆文件访问方法，我们已经为您编写了一些代码。

[HeapFile](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\HeapFile.java#L23-L315)对象被组织成一组页面，每个页面由固定数量的字节组成，用于存储元组（由常量[BufferPool.DEFAULT_PAGE_SIZE](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\BufferPool.java#L31-L31)定义），包括一个头部。在SimpleDB中，每个数据库中的表都有一个[HeapFile](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\HeapFile.java#L23-L315)对象。[HeapFile](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\HeapFile.java#L23-L315)中的每个页面被组织成一组槽，每个槽可以容纳一个元组（SimpleDB中的给定表的元组都是相同大小的）。除了这些槽之外，每个页面都有一个头部，由每个元组槽对应一位的位图组成。如果对应特定元组的位是1，表示该元组有效；如果是0，表示该元组无效（例如，已被删除或从未初始化）。[HeapFile](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\HeapFile.java#L23-L315)对象的页面是[HeapPage](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\HeapPage.java#L17-L446)类型，它实现了[Page](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\Page.java#L14-L61)接口。页面存储在缓冲池中，但由[HeapFile](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\HeapFile.java#L23-L315)类读取和写入。

SimpleDB在磁盘上存储堆文件的格式与在内存中存储的格式基本相同。每个文件由连续排列在磁盘上的页面数据组成。每个页面由一个或多个字节的头部表示，后跟页面大小字节的实际页面内容。每个元组需要_tuple size_ * 8位来存储其内容和1位用于头部。因此，单个页面中可以容纳的元组数为：

```
每页元组数 = floor((页面大小 * 8) / (元组大小 * 8 + 1))
```


其中_tuple size_是以字节为单位的页面中元组的大小。这里的想法是每个元组在头部需要一个额外的存储位。我们计算页面中的位数（通过将页面大小乘以8），然后除以元组中的位数（包括这个额外的头部位）来得到每页的元组数。floor操作向下舍入到最接近的整数元组数（我们不想在页面上存储部分元组！）

一旦我们知道每页的元组数，存储头部所需的字节数就是：

```
头部字节数 = ceiling(每页元组数 / 8)
```


ceiling操作向上舍入到最接近的整数字节数（我们从不存储少于一个完整字节的头部信息。）

每个字节的低位（最低有效位）表示文件中较早槽的状态。因此，第一个字节的最低位表示页面中的第一个槽是否在使用中。第一个字节的第二低位表示页面中的第二个槽是否在使用中，以此类推。还要注意，最后一个字节的高位可能不对应于文件中实际存在的槽，因为槽的数量可能不是8的倍数。还要注意，所有Java虚拟机都是[大端](http://en.wikipedia.org/wiki/Endianness)的。

### 练习4

**实现以下骨架方法：**

---
* src/java/simpledb/storage/HeapPageId.java
* src/java/simpledb/storage/RecordId.java
* src/java/simpledb/storage/HeapPage.java
---

虽然您在实验1中不会直接使用它们，但我们要求您在[HeapPage](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\HeapPage.java#L17-L446)中实现`getNumUnusedSlots()`和`isSlotUsed()`。这些需要在页面头部中移动位。您可能会发现查看[HeapPage](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\HeapPage.java#L17-L446)中已经提供的其他方法或`src/simpledb/HeapFileEncoder.java`中的方法有助于理解页面布局。

您还需要实现页面中元组的`Iterator`，这可能涉及辅助类或数据结构。

此时，您的代码应该通过`HeapPageIdTest`、`RecordIDTest`和`HeapPageReadTest`中的单元测试。

在您实现[HeapPage](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\HeapPage.java#L17-L446)之后，您将在此实验中编写[HeapFile](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\HeapFile.java#L23-L315)的方法来计算文件中的页面数并从文件中读取页面。然后您将能够从存储在磁盘上的文件中获取元组。

### 练习5

**实现以下骨架方法：**

---
* src/java/simpledb/storage/HeapFile.java
--- 

要从磁盘读取页面，您首先需要计算文件中的正确偏移量。提示：您需要随机访问文件才能在任意偏移量处读取和写入页面。从磁盘读取页面时，您不应该调用[BufferPool](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\BufferPool.java#L27-L346)实例方法。

您还需要实现`HeapFile.iterator()`方法，该方法应该遍历HeapFile中每个页面的元组。迭代器必须使用[BufferPool.getPage()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\BufferPool.java#L89-L101)方法来访问[HeapFile](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\HeapFile.java#L23-L315)中的页面。此方法将页面加载到缓冲池中，最终将用于（在后续实验中）实现基于锁定的并发控制和恢复。不要在`open()`调用时将整个表加载到内存中——这将导致非常大表的内存不足错误。

此时，您的代码应该通过`HeapFileReadTest`中的单元测试。

### 2.6. 操作符

操作符负责查询计划的实际执行。它们实现了关系代数的操作。在SimpleDB中，操作符是基于迭代器的；每个操作符都实现了`DbIterator`接口。

操作符通过将较低级别的操作符传递到较高级别操作符的构造函数中连接成一个计划，即通过"链式连接"。计划叶节点上的特殊访问方法操作符负责从磁盘读取数据（因此它们下面没有任何操作符）。

在计划的顶部，与SimpleDB交互的程序只需在根操作符上调用`getNext()`；然后该操作符在其子节点上调用`getNext()`，依此类推，直到调用这些叶操作符。它们从磁盘获取元组并通过树向上传递（作为`getNext()`的返回参数）；元组以这种方式在计划中向上传播，直到在根部输出或被计划中的另一个操作符组合或拒绝。

对于实现`INSERT`和`DELETE`查询的计划，最顶层的操作符是特殊的`Insert`或`Delete`操作符，它们修改磁盘上的页面。这些操作符向用户级程序返回一个包含受影响元组数的元组。

对于此实验，您只需要实现一个SimpleDB操作符。

### 练习6

**实现以下骨架方法：**

---
* src/java/simpledb/execution/SeqScan.java
---

此操作符按顺序扫描由构造函数中的`tableid`指定的表页面中的所有元组。此操作符应该通过`DbFile.iterator()`方法访问元组。

此时，您应该能够完成ScanTest系统测试。干得好！

您将在后续实验中填充其他操作符。

<a name="query_walkthrough"></a>

### 2.7. 一个简单查询

本节的目的是说明这些各种组件是如何连接在一起处理简单查询的。

假设您有一个数据文件"some_data_file.txt"，内容如下：

```
1,1,1
2,2,2 
3,4,4
```


您可以按如下方式将其转换为SimpleDB可以查询的二进制文件：

```bash
java -jar dist/simpledb.jar convert some_data_file.txt 3
```


这里，参数"3"告诉转换器输入有3列。

以下代码实现了一个简单的选择查询。此代码等同于SQL语句`SELECT * FROM some_data_file`。

```java
package simpledb;
import java.io.*;

public class test {

    public static void main(String[] argv) {

        // 构造一个3列的表模式
        Type types[] = new Type[]{ Type.INT_TYPE, Type.INT_TYPE, Type.INT_TYPE };
        String names[] = new String[]{ "field0", "field1", "field2" };
        TupleDesc descriptor = new TupleDesc(types, names);

        // 创建表，将其与some_data_file.dat关联
        // 并告诉目录该表的模式。
        HeapFile table1 = new HeapFile(new File("some_data_file.dat"), descriptor);
        Database.getCatalog().addTable(table1, "test");

        // 构造查询：我们使用简单的SeqScan，它通过其迭代器提供元组。
        TransactionId tid = new TransactionId();
        SeqScan f = new SeqScan(tid, table1.getId());

        try {
            // 并运行它
            f.open();
            while (f.hasNext()) {
                Tuple tup = f.next();
                System.out.println(tup);
            }
            f.close();
            Database.getBufferPool().transactionComplete(tid);
        } catch (Exception e) {
            System.out.println ("异常 : " + e);
        }
    }

}
```


我们创建的表有三个整数字段。为了表达这一点，我们创建一个[TupleDesc](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\TupleDesc.java#L11-L253)对象并传递给它一个[Type](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\common\Type.java#L16-L70)对象数组，以及可选的`String`字段名数组。一旦我们创建了这个[TupleDesc](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\TupleDesc.java#L11-L253)，我们初始化一个代表存储在`some_data_file.dat`中的表的[HeapFile](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\HeapFile.java#L23-L315)对象。一旦我们创建了表，我们将其添加到目录中。如果这是一个已经在运行的数据库服务器，我们将加载这些目录信息。我们需要显式加载它以使此代码自包含。

一旦我们完成了数据库系统的初始化，我们就创建一个查询计划。我们的计划只包含[SeqScan](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\execution\SeqScan.java#L17-L160)操作符，它从磁盘扫描元组。一般来说，这些操作符使用对适当表（在[SeqScan](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\execution\SeqScan.java#L17-L160)的情况下）或子操作符（在例如Filter的情况下）的引用来实例化。测试程序然后在[SeqScan](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\execution\SeqScan.java#L17-L160)操作符上重复调用`hasNext`和`next`。当元组从[SeqScan](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\execution\SeqScan.java#L17-L160)输出时，它们会在命令行上打印出来。

我们**强烈建议**您尝试一下，作为一个有趣的端到端测试，这将帮助您获得编写自己的SimpleDB测试程序的经验。您应该在`src/java/simpledb`目录中创建名为"test.java"的文件，内容如上，并且您应该在代码上方添加一些"import"语句，并将`some_data_file.dat`文件放在顶层目录中。然后运行：

```bash
ant
java -classpath dist/simpledb.jar simpledb.test
```


注意`ant`会编译`test.java`并生成一个包含它的新jar文件。

## 3. 后勤

您必须提交您的代码（见下文）以及描述您的方法的简短（最多2页）的writeup。这个writeup应该：

* 描述您做出的任何设计决策。对于实验1，这些可能很少。
* 讨论并证明您对API所做的任何更改。
* 描述您的代码中任何缺失或不完整的元素。
* 描述您在实验上花费的时间，以及是否有任何您觉得特别困难或令人困惑的地方。

### 3.1. 协作

这个实验一个人应该可以管理，但如果您更喜欢与合作伙伴一起工作，这也是可以的。不允许更大的小组。请在您的个人writeup中清楚地说明您与谁合作（如果有的话）。

### 3.2. 提交您的作业

我们将使用Gradescope对所有编程作业进行自动评分。您应该都已受邀加入班级实例；如果没有，请查看Piazza获取邀请码。如果您仍有问题，请告诉我们，我们可以帮助您设置。您可以在截止日期前多次提交代码；我们将使用Gradescope确定的最新版本。将write-up放在名为`lab1-writeup.txt`的文件中与您的提交一起。

如果您与合作伙伴合作，只有一个人需要提交到Gradescope。但是，请确保将其他人添加到您的小组中。还要注意，每个成员都必须有自己的writeup。请在文件名和writeup本身中添加您的Kerberos用户名（例如，`lab1-writeup-username1.txt`和`lab1-writeup-username2.txt`）。

提交到Gradescope的最简单方法是使用包含您的代码的`.zip`文件。在Linux/macOS上，您可以通过运行以下命令来实现：

```bash
$ zip -r submission.zip src/ lab1-writeup.txt

# 如果您与合作伙伴合作：
$ zip -r submission.zip src/ lab1-writeup-username1.txt lab1-writeup-username2.txt
```


### 3.3. 提交错误

请将（友好的！）错误报告提交到[6.5830-staff@mit.edu](mailto:6.5830-staff@mit.edu)。当您这样做时，请尝试包括：

* 错误的描述。
* 我们可以放在`test/simpledb`目录中、编译和运行的`.java`文件。
* 重现错误的数据`.txt`文件。我们应该能够使用HeapFileEncoder将其转换为`.dat`文件。

如果您是第一个报告代码中特定错误的人，我们将给您一根糖果棒！

### 3.4 评分

您的成绩75%将基于您的代码是否通过我们将对其运行的系统测试套件。这些测试将是我们提供的测试的超集。在提交代码之前，您应该确保从`ant test`和`ant systemtest`中不会产生错误（通过所有测试）。

**重要：** 在测试之前，Gradescope将用我们的版本替换您的`build.xml`和`test`目录的全部内容。这意味着您不能更改`.dat`文件的格式！您也应该小心更改我们的API。您应该测试您的代码是否编译未修改的测试。

提交后，您应该立即从gradescope获得反馈和失败测试的错误输出（如果有的话）。给出的分数将是您作业自动评分部分的成绩。另外25%的成绩将基于您的writeup质量和我们对您代码的主观评估。这部分也将在我们完成评分您的作业后在gradescope上发布。

我们在设计这个作业时有很多乐趣，我们希望您享受破解它的过程！