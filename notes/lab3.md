# Lab 3 实验总结

## Exercise 1: 成本计算

### 连接操作成本分析
对于查询 `p = t1 join t2 join ... tn`，成本包括：
- 扫描表 [t1](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\execution\HashEquiJoin.java#L18-L18) 和 [t2](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\execution\HashEquiJoin.java#L19-L19)，连接 [t1](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\execution\HashEquiJoin.java#L18-L18) 和 [t2](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\src\java\simpledb\execution\HashEquiJoin.java#L19-L19)
- 扫描表 `t3`，连接 [(t1 join t2)](file://D:\AppDate\Code\JetBrains\Idea\simple-db-hw-2022\test\simpledb\JoinTest.java#L117-L119) 和 `t3`

成本分为两部分：
1. **扫描成本**：页面数 × `SCALING_FACTOR`
2. **连接成本**：
   ```
   joincost(t1 join t2) = scancost(t1) + tupleNum(t1) × scancost(t2) // IO cost
                        + tupleNum(t1) × tupleNum(t2)               // CPU cost
   ```


## Exercise 2: TableStats

### Histogram 构建过程
1. 遍历第一遍：找出每个字段的最大值和最小值，创建 `Histogram`
2. 遍历第二遍：将元素加入 `Histogram`

### 问题处理
- 测试时出现大于1的情况，通过特判解决
- 对于边界情况，可以对偏移量为0的情况进行特殊处理

## Exercise 3
(待补充)

## Exercise 4: 基于代价的优化器

### 核心实现
- 实现了简单的基于代价的优化器
- 采用动态规划算法
- 重新实现了排列组合方法

## 学习收获
1. 掌握了成本计算的基本思路
2. 复习了位运算
3. 温习了动态规划算法