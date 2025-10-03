### Exercise 1
B+树有5种页面,叶子节点页面,中间节点页面,根节点页面,头页面，开头有一个页面指向根节点和头页面.  
需要先根据id判断一下页面类型,若是叶子则直接返回,否则遍历内部节点找到一个合适的孩子节点
### Exercise 2
BTreeFile.splitLeafPage()
实现思路就是,计算需要移动个数，然后移动元组。
因为在这个实验经常涉及迭代器和移动,为了防止出现并发修改问题,选择将需要移动的元素收集起来,然后再进行删除添加  
接着修改叶子页面之间的指针,若是原页面右侧存在页面,则右侧页面指向左侧页面指针也要修改为新分裂出的页面  
然后将新页面的第一个元素上推到父节点  
最后更新父节点指针然后将变更页面加入脏页集合,根据与上推节点的比较结果返回页面
splitInternalPage类似

### Exercise 3
实现BTreeFile.stealFromLeafPage()、BTreeFile.stealFromLeftInternalPage()、BTreeFile.stealFromRightInternalPage()。
stealFromLeafPage()  
计算需要移动的元素数，根据兄弟与原页面位置选择合适的迭代器(正序和倒序)
最后需要更新父节点key
stealFromLeftInternalPage()
和叶子节点区别是,叶子节点具有父节点所有key,所以可以不下推直接更新,而在内部页面,父节点的值子节点未必有，故需要先下推然后移动  
然后将左边页面最后一个节点上推到父页面,
  最后更新指针更新脏页集
stealFromRightInternalPage()
和left的区别就是,因为二者移走元素位置不同删除方法调用也稍有区别

### Exercise 4
实现BTreeFile.mergeLeafPages()和BTreeFile.mergeInternalPages()。
mergeLeafPages()  
和分裂有些类似,计算移动元素数目，移动，更新指针  
多了释放右页面，删除父节点
最后更新脏页集合.
mergeInternalPages()
和叶子节点合并区别就是需要下推原父节点，删除移动到最后的节点