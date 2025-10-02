# 6.5831 实验5：B+树索引

**发布时间：** 2022年11月9日星期三

**截止日期：** 2022年11月21日星期一晚上11:59（东部时间）

**注意：** 本实验面向注册6.5831课程（本科版本）且**不**做期末项目的同学。注册6.5830课程（研究生版本）的同学无需完成此实验。

## 0. 简介

在本实验中，您将实现一个B+树索引来支持高效的查找和范围扫描。我们为您提供实现树结构所需的所有底层代码。您需要实现搜索、页面分割、在页面间重新分配元组以及合并页面的功能。

您可能会发现复习Alex Petrov所著的《Database Internals》第2章很有帮助，该书详细介绍了B+树的结构。

如课堂讨论的那样，B+树中的内部节点包含多个条目，每个条目由一个键值和左右子指针组成。相邻的键共享一个子指针，因此包含*m*个键的内部节点有*m*+1个子指针。叶节点可以包含数据条目或者指向其他数据库文件中数据条目的指针。为了简化起见，我们将实现一个叶页面实际包含数据条目的B+树。相邻的叶页面通过右兄弟和左兄弟指针连接在一起，因此范围扫描只需要一次通过根节点和内部节点的初始搜索来找到第一个叶页面，后续的叶页面通过跟随右（或左）兄弟指针找到。

## 1. 开始

您应该从Lab 4的代码开始（如果您没有提交Lab 4的代码，或者您的解决方案不能正常工作，请联系我们讨论选项）。此外，我们为此实验提供了原始代码分发包中没有的额外源文件和测试文件。

您需要将这些新文件添加到您的发布版并设置lab4分支。最简单的方法是切换到您的项目目录（可能叫做`simple-db-hw-2022`），设置分支，然后从主GitHub仓库拉取：

```bash
$ cd simple-db-hw-2022
$ git pull upstream main
```


## 2. 搜索

查看`index/`目录和[BTreeFile.java](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreeFile.java)文件。这是B+树实现的核心文件，也是您在此实验中编写所有代码的地方。与[HeapFile](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\HeapFile.java#L23-L315)不同，[BTreeFile](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreeFile.java#L28-L1023)由四种不同的页面组成。如您所料，树的节点有两种不同的页面：内部页面和叶页面。内部页面在[BTreeInternalPage.java](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreeInternalPage.java)中实现，叶页面在[BTreeLeafPage.java](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreeLeafPage.java)中实现。为了方便，我们在[BTreePage.java](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreePage.java)中创建了一个抽象类，其中包含了叶页面和内部页面的公共代码。此外，头页面在[BTreeHeaderPage.java](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreeHeaderPage.java)中实现，用于跟踪文件中哪些页面正在使用。最后，在每个[BTreeFile](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreeFile.java#L28-L1023)的开头有一个页面，指向树的根页面和第一个头页面。这个单例页面在[BTreeRootPtrPage.java](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreeRootPtrPage.java)中实现。请熟悉这些类的接口，特别是[BTreePage](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreePage.java#L18-L146)、[BTreeInternalPage](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreeInternalPage.java#L20-L673)和[BTreeLeafPage](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreeLeafPage.java#L17-L520)。您将在B+树的实现中使用这些类。

您的第一项任务是在[BTreeFile.java](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreeFile.java)中实现[findLeafPage()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreeFile.java#L201-L205)函数。该函数用于根据特定的键值找到相应的叶页面，用于搜索和插入操作。例如，假设我们有一个有两个叶页面的B+树（见图1）。根节点是一个内部页面，包含一个条目，该条目包含一个键（在这个例子中是6）和两个子指针。给定值1，该函数应返回第一个叶页面。同样，给定值8，该函数应返回第二个页面。不太明显的情况是我们给定键值6。可能存在重复键，所以两个叶页面上都可能有6。在这种情况下，函数应返回第一个（左边的）叶页面。

<p align="center"> <img width=500 src="lab5-simple_tree.png"><br> <i>图1：具有重复键的简单B+树</i> </p>

您的[findLeafPage()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreeFile.java#L201-L205)函数应递归搜索内部节点，直到到达与所提供键值对应的叶页面。为了在每一步找到适当的子页面，您应遍历内部页面中的条目并将条目值与提供的键值进行比较。[BTreeInternalPage.iterator()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreeInternalPage.java#L609-L611)使用[BTreeEntry.java](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreeEntry.java)中定义的接口提供对内部页面中条目的访问。该迭代器允许您遍历内部页面中的键值，并访问每个键的左右子页面ID。递归的基本情况是传入的[BTreePageId](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreePageId.java#L11-L121)的[pgcateg()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreePageId.java#L69-L71)等于[BTreePageId.LEAF](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreePageId.java#L15-L15)，表明这是一个叶页面。在这种情况下，您应该直接从缓冲池获取页面并返回。您不需要确认它实际包含提供的键值f。

您的[findLeafPage()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreeFile.java#L201-L205)代码还必须处理提供的键值f为null的情况。如果提供的值为null，每次都应在最左边的子节点上递归，以找到最左边的叶页面。找到最左边的叶页面对于扫描整个文件很有用。一旦找到正确的叶页面，您应该返回它。如上所述，您可以使用[BTreePageId.java](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreePageId.java)中的[pgcateg()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreePageId.java#L69-L71)函数检查页面类型。您可以假设只有叶页面和内部页面会被传递给此函数。

与其直接调用[BufferPool.getPage()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\BufferPool.java#L89-L101)来获取每个内部页面和叶页面，我们建议调用我们提供的包装函数[BTreeFile.getPage()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreeFile.java#L392-L403)。它的功能与[BufferPool.getPage()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\BufferPool.java#L89-L101)完全相同，但需要一个额外的参数来跟踪脏页面列表。在接下来的两个练习中，您将实际更新数据，因此需要跟踪脏页面，这个函数将非常重要。

您的[findLeafPage()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreeFile.java#L201-L205)实现在访问的每个内部（非叶）页面都应该以READ_ONLY权限获取，除了返回的叶页面，应该以作为参数提供给函数的权限获取。这些权限级别在此实验中并不重要，但对于未来实验中代码的正确功能很重要。

----------

**练习1：BTreeFile.findLeafPage()**

实现[BTreeFile.findLeafPage()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreeFile.java#L201-L205)。

完成此练习后，您应该能够通过[BTreeFileReadTest.java](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\test\simpledb\BTreeFileReadTest.java)中的所有单元测试和[BTreeScanTest.java](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\test\simpledb\systemtest\BTreeScanTest.java)中的系统测试。

----------

## 3. 插入

为了保持B+树元组的有序性和树的完整性，我们必须将元组插入到包含键范围的叶页面中。如上所述，[findLeafPage()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreeFile.java#L201-L205)可用于找到我们应该插入元组的正确叶页面。然而，每个页面都有有限的槽位，我们需要能够在对应叶页面已满的情况下插入元组。

如教科书中所述，试图向已满的叶页面插入元组会导致该页面分裂，使元组在两个新页面之间均匀分布。每次叶页面分裂时，都需要向父节点添加一个与第二个页面中第一个元组对应的新条目。偶尔，内部节点也可能已满且无法接受新条目。在这种情况下，父节点应该分裂并向其父节点添加一个新条目。这可能导致递归分裂，最终创建一个新的根节点。

在此练习中，您将在[BTreeFile.java](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreeFile.java)中实现[splitLeafPage()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreeFile.java#L227-L239)和[splitInternalPage()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreeFile.java#L262-L275)。如果被分割的页面是根页面，您需要创建一个新的内部节点成为新的根页面，并更新[BTreeRootPtrPage](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreeRootPtrPage.java#L15-L232)。否则，您需要以READ_WRITE权限获取父页面，必要时递归分割它，并添加一个新条目。您会发现函数[getParentWithEmptySlots()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreeFile.java#L293-L325)对于处理这些不同情况非常有用。在[splitLeafPage()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreeFile.java#L227-L239)中，您应该将键"复制"到父页面，而在[splitInternalPage()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreeFile.java#L262-L275)中，您应该将键"推"到父页面。如果这令人困惑，请参见图2并复习教科书中的第10.5节。记住根据需要更新新页面的父指针（为简单起见，我们在图中没有显示父指针）。当内部节点分裂时，您需要更新所有被移动子节点的父指针。您可能会发现函数[updateParentPointers()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreeFile.java#L360-L372)对此任务很有用。此外，记得更新任何被分割叶页面的兄弟指针。最后，返回应该插入新元组或条目的页面，如提供的键字段所示。（提示：您不需要担心提供的键可能恰好落在要分割的元组/条目的正中心。您应该在分割过程中忽略键，只使用它来确定返回两个页面中的哪一个。）

<p align="center"> <img width=500 src="lab5-splitting_leaf.png"><br> <img width=500
src="lab5-splitting_internal.png"><br> <i>图2：页面分割</i> </p>

每当您创建一个新页面，无论是因为页面分割还是创建新根页面，都要调用[getEmptyPage()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreeFile.java#L893-L910)来获取新页面。这个函数是一个抽象，它将允许我们重用因合并而删除的页面（在下一节中介绍）。

我们期望您使用[BTreeLeafPage.iterator()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreeLeafPage.java#L483-L485)和[BTreeInternalPage.iterator()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreeInternalPage.java#L609-L611)与叶页面和内部页面交互，以遍历每个页面中的元组/条目。为了方便，我们还为两种类型的页面提供了反向迭代器：[BTreeLeafPage.reverseIterator()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreeLeafPage.java#L491-L493)和[BTreeInternalPage.reverseIterator()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreeInternalPage.java#L617-L619)。这些反向迭代器对于将元组/条目的子集从页面移动到其右侧兄弟页面特别有用。

如上所述，内部页面迭代器使用[BTreeEntry.java](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreeEntry.java)中定义的接口，该接口有一个键和两个子指针。它还有一个recordId，用于标识底层页面上键和子指针的位置。我们认为一次处理一个条目是与内部页面交互的自然方式，但重要的是要记住，底层页面实际上并不存储条目列表，而是存储有序的*m*个键和*m*+1个子指针列表。由于[BTreeEntry](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreeEntry.java#L21-L142)只是一个接口，而不是实际存储在页面上的对象，更新[BTreeEntry](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreeEntry.java#L21-L142)的字段不会修改底层页面。为了更改页面上的数据，您需要调用[BTreeInternalPage.updateEntry()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreeInternalPage.java#L407-L437)。此外，删除条目实际上只删除一个键和一个子指针，所以我们提供了[BTreeInternalPage.deleteKeyAndLeftChild()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreeInternalPage.java#L394-L396)和[BTreeInternalPage.deleteKeyAndRightChild()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreeInternalPage.java#L380-L382)函数来明确这一点。条目的recordId用于找到要删除的键和子指针。插入条目也只插入一个键和一个子指针（除非是第一个条目），所以[BTreeInternalPage.insertEntry()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreeInternalPage.java#L447-L547)会检查提供的条目中的一个子指针是否与页面上的现有子指针重叠，并且在该位置插入条目将保持键的排序。

在[splitLeafPage()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreeFile.java#L227-L239)和[splitInternalPage()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreeFile.java#L262-L275)中，您都需要使用任何新创建的页面以及由于新指针或新数据而修改的页面更新`dirtypages`集合。这就是[BTreeFile.getPage()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreeFile.java#L392-L403)派上用场的地方。每次获取页面时，[BTreeFile.getPage()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreeFile.java#L392-L403)都会检查页面是否已经存储在本地缓存（`dirtypages`）中，如果在那里找不到请求的页面，它就会从缓冲池中获取。如果以读写权限获取页面，[BTreeFile.getPage()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreeFile.java#L392-L403)还会将页面添加到`dirtypages`缓存中，因为它们很可能很快就会变脏。这种方法的一个优点是，如果在单次元组插入或删除期间多次访问相同页面，它可以防止更新丢失。

需要注意的是，与[HeapFile.insertTuple()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\HeapFile.java#L145-L177)有很大不同，[BTreeFile.insertTuple()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreeFile.java#L415-L440)可能返回大量脏页面，特别是如果有内部页面被分割。正如您可能还记得之前的实验，返回脏页面集合是为了防止缓冲池在页面被刷新之前驱逐脏页面。

----------

**警告**：由于B+树是一种复杂的数据结构，在修改之前了解每个合法B+树所需的属性是有帮助的。以下是一个非正式的列表：

1. 如果父节点指向子节点，则子节点必须指回相同的父节点。
2. 如果叶节点指向右兄弟，则右兄弟必须指回该叶节点作为左兄弟。
3. 第一个和最后一个叶节点必须分别指向null的左和右兄弟。
4. 记录ID必须与它们实际所在的页面匹配。
5. 具有非叶节点子节点的节点中的`键`必须大于左子节点中的任何键，小于右子节点中的任何键。
6. 具有叶节点子节点的节点中的`键`必须大于或等于左子节点中的任何键，小于或等于右子节点中的任何键。
7. 节点要么具有全部非叶节点子节点，要么具有全部叶节点子节点。
8. 非根节点不能为空少于一半。

我们在[BTreeChecker.java](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreeChecker.java)文件中实现了对所有这些属性的机械化检查。此方法也用于`systemtest/BTreeFileDeleteTest.java`中的B+树实现测试。请随时添加对此函数的调用来帮助调试您的实现，就像我们在[BTreeFileDeleteTest.java](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\test\simpledb\BTreeFileDeleteTest.java)中所做的那样。

**注意：**
1. 检查器方法应在树初始化后以及开始和完成完整的键插入或删除调用前后始终通过，但在内部方法中不一定通过。

2. 树可能是格式良好的（因此通过[checkRep()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreeInternalPage.java#L28-L42)），但仍可能不正确。例如，空树将始终通过[checkRep()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreeInternalPage.java#L28-L42)，但可能并非总是正确（如果您刚刚插入了一个元组，树不应为空）。***

**练习2：页面分割**

实现[BTreeFile.splitLeafPage()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreeFile.java#L227-L239)和[BTreeFile.splitInternalPage()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreeFile.java#L262-L275)。

完成此练习后，您应该能够通过[BTreeFileInsertTest.java](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\test\simpledb\BTreeFileInsertTest.java)中的单元测试。您还应该能够通过`systemtest/BTreeFileInsertTest.java`中的系统测试。某些系统测试用例可能需要几秒钟才能完成。这些文件将测试您的代码是否正确插入元组和分割页面，以及如何处理重复元组。

----------

## 4. 删除

为了保持树的平衡且不浪费不必要的空间，B+树中的删除操作可能导致页面重新分配元组（图3）或最终合并（见图4）。您可能会发现复习教科书中的第10.6节很有用。

<p align="center"> <img width=500 src="lab5-redist_leaf.png"><br> <img width=500
src="lab5-redist_internal.png"><br> <i>图3：页面重新分配</i> </p>

<p align="center"> <img width=500 src="lab5-merging_leaf.png"><br> <img width=500
src="lab5-merging_internal.png"><br> <i>图4：页面合并</i> </p>

如教科书中所述，试图从少于半满的叶页面删除元组应导致该页面要么从其兄弟页面之一窃取元组，要么与其兄弟页面之一合并。如果页面的兄弟页面有多余的元组，则元组应在两个页面之间均匀分配，并相应地更新父节点的条目（见图3）。然而，如果兄弟页面也处于最小占用率，则两个页面应合并，并从父节点删除条目（图4）。反过来，从父节点删除条目可能导致父节点变得少于半满。在这种情况下，父节点应从其兄弟页面窃取条目或与兄弟页面合并。这可能导致递归合并，甚至在从根节点删除最后一个条目时删除根节点。

在此练习中，您将在[BTreeFile.java](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreeFile.java)中实现[stealFromLeafPage()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreeFile.java#L546-L553)、[stealFromLeftInternalPage()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreeFile.java#L619-L627)、[stealFromRightInternalPage()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreeFile.java#L645-L653)、[mergeLeafPages()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreeFile.java#L672-L682)和[mergeInternalPages()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreeFile.java#L703-L714)。在前三个函数中，您将实现代码以在兄弟页面有元组/条目可分享时均匀重新分配元组/条目。记住要更新父节点中相应的键字段（仔细观察图3中是如何做到的——键有效地通过父节点"旋转"）。在[stealFromLeftInternalPage()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreeFile.java#L619-L627)/[stealFromRightInternalPage()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreeFile.java#L645-L653)中，您还需要更新被移动子节点的父指针。您应该能够重用[updateParentPointers()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreeFile.java#L360-L372)函数来达到此目的。

在[mergeLeafPages()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreeFile.java#L672-L682)和[mergeInternalPages()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreeFile.java#L703-L714)中，您将实现合并页面的代码，实际上是执行[splitLeafPage()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreeFile.java#L227-L239)和[splitInternalPage()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreeFile.java#L262-L275)的逆操作。您会发现[deleteParentEntry()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreeFile.java#L733-L759)函数对于处理所有不同的递归情况非常有用。一定要在删除的页面上调用[setEmptyPage()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreeFile.java#L924-L995)使其可用于重用。与前面的练习一样，我们建议使用[BTreeFile.getPage()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreeFile.java#L392-L403)来封装获取页面的过程并保持脏页面列表的最新状态。

----------

**练习3：页面重新分配**

实现[BTreeFile.stealFromLeafPage()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreeFile.java#L546-L553)、[BTreeFile.stealFromLeftInternalPage()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreeFile.java#L619-L627)、[BTreeFile.stealFromRightInternalPage()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreeFile.java#L645-L653)。

完成此练习后，您应该能够通过[BTreeFileDeleteTest.java](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\test\simpledb\BTreeFileDeleteTest.java)中的一些单元测试（如[testStealFromLeftLeafPage](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\test\simpledb\BTreeFileDeleteTest.java#L58-L95)和[testStealFromRightLeafPage](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\test\simpledb\BTreeFileDeleteTest.java#L97-L134)）。系统测试可能需要几秒钟才能完成，因为它们创建了一个大型B+树来全面测试系统。

**练习4：页面合并**

实现[BTreeFile.mergeLeafPages()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreeFile.java#L672-L682)和[BTreeFile.mergeInternalPages()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreeFile.java#L703-L714)。

现在您应该能够通过[BTreeFileDeleteTest.java](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\test\simpledb\BTreeFileDeleteTest.java)中的所有单元测试和`systemtest/BTreeFileDeleteTest.java`中的系统测试。

----------

## 5. 事务

您可能还记得，B+树可以通过使用next-key锁定来防止幻像元组在两次连续的范围扫描之间出现。由于SimpleDB使用页面级严格两阶段锁定，如果B+树实现正确，防幻像保护实际上是免费的。因此，此时您也应该能够通过[BTreeNextKeyLockingTest](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\test\simpledb\BTreeNextKeyLockingTest.java#L23-L252)。

此外，如果您在B+树代码中正确实现了锁定，您应该能够通过`test/simpledb/BTreeDeadlockTest.java`中的测试。

如果一切实现正确，您还应该能够通过BTreeTest系统测试。我们预计许多人会觉得[BTreeTest](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\test\simpledb\systemtest\BTreeTest.java#L26-L223)很难，所以这不是必需的，但我们会给成功运行它的任何人额外加分。请注意，此测试可能需要长达一分钟才能完成。

## 6. 额外学分

----------

**奖励练习5：（额外10%学分）**

创建并实现一个名为`BTreeReverseScan`的类，该类可以在给定可选[IndexPredicate](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\execution\IndexPredicate.java#L11-L53)的情况下反向扫描[BTreeFile](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreeFile.java#L28-L1023)。

您可以使用[BTreeScan](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreeScan.java#L19-L149)作为起点，但您可能需要在[BTreeFile](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreeFile.java#L28-L1023)中实现一个反向迭代器。您还可能需要实现[BTreeFile.findLeafPage()](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreeFile.java#L201-L205)的单独版本。我们已经在[BTreeLeafPage](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreeLeafPage.java#L17-L520)和[BTreeInternalPage](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\index\BTreeInternalPage.java#L20-L673)上提供了反向迭代器，您可能会觉得有用。您还应该编写代码来测试您的实现是否正常工作。[BTreeScanTest.java](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\test\simpledb\systemtest\BTreeScanTest.java)是一个寻找想法的好地方。

----------

## 7. 提交要求

您必须提交您的代码（见下文）以及描述您方法的简短（最多1页）说明文档。该说明文档应包括：

* 描述您做出的任何设计决策，包括任何困难或意外的事情。

* 讨论并证明您在BTreeFile.java之外所做的任何更改。

* 这个实验花了您多长时间？您有什么改进建议吗？

* 可选：如果您完成了额外学分练习，请解释您的实现并向我们展示您进行了彻底的测试。

### 7.1. 合作

这个实验一个人应该可以完成，但如果您更愿意与合作伙伴一起工作，这也是可以的。不允许更大的小组。请在您的说明文档中清楚地指出您与谁合作（如果有的话）。

### 7.2. 提交作业

我们将使用Gradescope自动评分所有编程作业。您应该都已经收到了班级实例的邀请；如果没有，请告知我们，我们可以帮您设置。您可以在截止日期前多次提交代码；我们将按照Gradescope确定的最新版本进行评分。将说明文档放在名为`lab5-writeup.txt`的文件中与您的提交一起。您还需要显式添加您创建的任何其他文件，如新的`*.java`文件。

如果您与合作伙伴一起工作，只需要一个人提交到Gradescope。但是，请确保将其他人添加到您的小组中。还要注意每个成员必须有自己的说明文档。请在文件名和说明文档本身中添加您的Kerberos用户名（例如，`lab5-writeup-username1.txt`和`lab5-writeup-username2.txt`）。

提交到Gradescope的最简单方法是使用包含您的代码的`.zip`文件。在Linux/macOS上，您可以通过运行以下命令来实现：

```bash
$ zip -r submission.zip src/ lab5-writeup.txt

# 如果您与合作伙伴一起工作：
$ zip -r submission.zip src/ lab5-writeup-username1.txt lab5-writeup-username2.txt
```


<a name="bugs"></a>

### 7.3. 提交错误

SimpleDB是一段相对复杂的代码。您很可能会发现错误、不一致之处，以及糟糕、过时或不正确的文档等。

因此，我们要求您以冒险的心态来做这个实验。如果有什么不清楚，甚至是错误的，不要生气；相反，试着自己弄清楚或者给我们发送一封友好的电子邮件。

请将（友好的！）错误报告提交到<a href="mailto:6.5830-staff@mit.edu">6.5830-staff@mit.edu</a>。当您这样做时，请尽量包括：

* 错误的描述。
* 一个我们可以放入`test/simpledb`目录、编译并运行的[.java](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\Parser.java)文件。
* 一个包含重现错误数据的[.txt](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\bin\depcache\dependencies.txt)文件。我们应该能够使用[HeapFileEncoder](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\HeapFileEncoder.java#L17-L227)将其转换为`.dat`文件。

如果您认为遇到了错误，也可以在Piazza的班级页面上发帖。

### 7.4. 评分

您的成绩75%将基于您的代码是否通过我们将运行的系统测试套件。这些测试将是我们提供的测试的超集。在提交代码之前，您应该确保它在`ant test`和`ant systemtest`中都不产生错误（通过所有测试）。

**重要提示：** 在测试之前，Gradescope将替换您的[build.xml](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\build.xml)、[HeapFileEncoder.java](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\storage\HeapFileEncoder.java)、[Parser.java](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\Parser.java)和`test`目录的全部内容为我们版本的这些文件。这意味着您不能更改`.dat`文件的格式，也不能依赖[Parser.java](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\Parser.java)中的任何自定义功能。您也应该小心更改我们的API。您应该测试您的代码是否能编译未修改的测试。

提交后，您应该从Gradescope获得即时反馈和失败测试的错误输出（如果有的话）。给出的分数将是您作业自动评分部分的成绩。另外25%的成绩将基于您的说明文档质量和我们对您代码的主观评估。这部分也将在我们完成评分后在Gradescope上公布。

我们在设计这个作业时感到非常有趣，希望您喜欢在这个项目上工作！