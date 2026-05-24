# Decaf 语言编译器设计实验

> 编译技术课程实验项目 —— 面向 Decaf 语言的完整编译器前端实现，涵盖词法分析、语法分析与三地址中间代码生成三大核心编译阶段。

---

## 1. 项目简介

### 1.1 Decaf 语言特性

Decaf 是一种类 C/C++/Java 风格的教学编程语言，支持以下核心语言特性：

- **基本数据类型**：`int`（整型）、`float`（浮点型）、`bool`（布尔型）、`char`（字符型）
- **复合数据类型**：多维数组（如 `float[100] a`）
- **控制流语句**：`if` / `if-else` 条件分支、`while` 循环、`do-while` 循环、`for` 循环、`break` 跳转
- **表达式体系**：算术运算（`+` `-` `*` `/`）、关系比较（`<` `<=` `>=` `>`）、等价判断（`==` `!=`）、逻辑运算（`&&` `||` `!`）、一元取负（`-`）
- **变量声明与赋值**：类型前置声明的变量定义，标量与数组元素的赋值操作

### 1.2 项目目标

本实验旨在从零构建一个 Decaf 语言的编译器前端，实现从源程序文本到三地址中间代码的完整翻译流程。核心目标包括：

1. **词法分析（Lexical Analysis）**：将 Decaf 源程序字符流扫描解析为 Token 序列，识别关键字、标识符、常量字面量、运算符与界符，正确处理空白字符与跨平台换行符（`\r\n` / `\r` / `\n`）
2. **语法分析（Syntax Analysis）**：基于递归下降技术构建语法解析器，按照 Decaf 文法规则构造抽象语法树（AST），同时进行符号表管理与类型检查
3. **中间代码生成（Intermediate Code Generation）**：以三地址码（Three-Address Code）形式输出与目标平台无关的中间表示，生成跳转指令、标号与临时变量，支持短路求值

---

## 2. 开发环境与技术栈

| 类别 | 详情 |
|------|------|
| **编程语言** | Java（JDK 8+） |
| **集成开发环境** | IntelliJ IDEA |
| **版本控制** | Git |
| **编译方式** | `javac` 命令行编译或 IDE 构建 |
| **运行依赖** | 无第三方依赖，基于 Java 标准库 |
| **目标平台** | 跨平台（Windows / Linux / macOS） |
| **字符编码** | UTF-8 |

---

## 3. 整体编译架构

项目采用经典的**两遍编译前端架构**，由词法扫描器（Scanner）和语法分析器（Parser）两大模块构成，语法分析器内部集成中间代码生成器。模块间自上而下的层级关系如下：

```
┌─────────────────────────────────────────────────────┐
│                    Decaf 源程序                       │
│                  （test.txt）                         │
└─────────────────────┬───────────────────────────────┘
                      │ 字符流
                      ▼
┌─────────────────────────────────────────────────────┐
│              词法扫描器（Lexer）                       │
│  · InputStream Reader 逐字符读取                      │
│  · 空白符/换行符过滤（支持 \r\n \r \n）                │
│  · 关键字/标识符/数字/运算符识别                        │
│  · 输出 Token 序列                                    │
└─────────────────────┬───────────────────────────────┘
                      │ Token 流
                      ▼
┌─────────────────────────────────────────────────────┐
│            语法分析器（Parser）                        │
│  · 递归下降解析（Recursive Descent）                   │
│  · 自顶向下推导 Decaf 文法                             │
│  · 符号表（Env）管理：变量声明、类型检查                │
│  · 抽象语法树（AST）构建                               │
│  · 第一阶段：program_phase1() → 语法树展示             │
│  · 第二阶段：program_phase2() → 中间代码生成            │
└─────────────────────┬───────────────────────────────┘
                      │ AST / IR Node
                      ▼
┌─────────────────────────────────────────────────────┐
│          中间表示层（inter 包）                        │
│  · Node → Expr / Stmt 抽象基类                        │
│  · 控制流：If / Else / While / Do / For / Break       │
│  · 表达式：Arith / Rel / And / Or / Not / Unary        │
│  · 存取：Id / Access / Set / SetElem                  │
│  · 临时变量与标号生成                                   │
└─────────────────────┬───────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────┐
│              三地址中间代码输出                         │
│     L1:  t1 = i * 8                                  │
│           t2 = a [ t1 ]                               │
│           if t2 < v goto L3                           │
│           ...                                         │
└─────────────────────────────────────────────────────┘
```

### 3.1 模块职责划分

| 模块 | 包路径 | 核心职责 |
|------|--------|----------|
| **Scanner 端 Lexer** | `scanner/src/lexer/` | 增强型词法扫描器，支持注释、字符串字面量、十六进制、科学计数法 |
| **Parser 端 Lexer** | `parser/src/lexer/` | 核心词法扫描器，为语法分析器服务的精简版本 |
| **Parser** | `parser/src/parser/` | 递归下降语法分析器，驱动整个编译流程 |
| **inter** | `parser/src/inter/` | 中间表示（IR）节点类层次结构，含代码生成逻辑 |
| **symbols** | `parser/src/symbols/` | 符号表与类型系统（Type / Array / Env） |

---

## 4. 核心功能实现

### 4.1 词法识别（Lexical Recognition）

词法分析器 `Lexer` 基于 `InputStreamReader` 逐字符读取源文件，采用**前瞻字符（peek）**机制实现单字符预读。

**识别流程：**

1. **空白符与换行符处理**：跳过空格（`' '`）与制表符（`'\t'`）；对 `\r\n`（Windows）、`\r`（旧 MacOS）、`\n`（Unix）三种换行格式进行统一处理，保证跨平台行号计数的准确性
2. **关键字与标识符**：字母开头、字母数字组成的词素（lexeme），通过 `Hashtable` 查找预留关键字集合（`if`、`while`、`for`、`int`、`float` 等），命中则返回关键字 Token，否则创建为标识符（`ID`）Token
3. **数字常量**：整数（`Num`，tag=270）与浮点数（`Real`，tag=272），支持小数点解析
4. **双字符运算符**：向前预读一位以区分 `&&`/`&`、`||`/`|`、`==`/`=` 等
5. **界符**：单字符界符（`{` `}` `(` `)` `[` `]` `;` `+` `-` `*` `/` 等）直接返回对应 Token

**Scanner 模块增强特性：**
- 行注释 `//` 与块注释 `/* */` 的识别与过滤
- 字符串字面量 `"..."` 扫描
- 十六进制整数字面量（`0x` / `0X` 前缀）
- 科学计数法浮点数（`E` 指数表示）

### 4.2 语法规则校验（Grammar Validation）

语法分析器采用**递归下降（Recursive Descent）**策略，为每个非终结符设计对应的解析方法，严格按照以下 Decaf 文法进行推导：

```
program     → block
block       → { decls stmts }
decls       → decls decl | ε
decl        → type ID ;
type        → basic | basic [ NUM ] { [ NUM ] }
stmts       → stmts stmt | ε
stmt        → block
            | if ( bool ) stmt
            | if ( bool ) stmt else stmt
            | while ( bool ) stmt
            | do stmt while ( bool ) ;
            | for ( opt ; opt ; opt ) stmt
            | break ;
            | assign ;
            | ;
assign      → ID = bool ;
            | L = bool ;
L           → ID [ bool ] { [ bool ] }
bool        → bool || join | join
join        → join && equality | equality
equality    → equality == rel | equality != rel | rel
rel         → expr < expr | expr <= expr | expr >= expr | expr > expr | expr
expr        → expr + term | expr - term | term
term        → term * unary | term / unary | unary
unary       → - unary | ! unary | factor
factor      → ( bool ) | NUM | REAL | true | false | ID | ID [ bool ]
```

**错误处理机制：** 遇到语法错误时抛出 `ParseException`，精确报告错误发生的行号、期望 Token 与实际 Token，辅助快速定位源代码问题。

### 4.3 语句解析（Statement Parsing）

| 语句类型 | 解析方法 | IR 生成策略 |
|----------|----------|-------------|
| **if** | `stmt()` case `IF` | 生成条件跳转 + 真分支标号，`iffalse` 跳过真分支体 |
| **if-else** | `stmt()` case `IF` + `ELSE` | 生成两路分支标号，真分支末尾 `goto after` 跳过假分支 |
| **while** | `stmt()` case `WHILE` | 条件测试在前，`iffalse goto after`，循环体末尾 `goto begin` |
| **do-while** | `stmt()` case `DO` | 循环体在前，条件测试在后，`if true goto begin` |
| **for** | `stmt()` case `FOR` | 四段结构：init → test → body → incr → `goto test` |
| **break** | `stmt()` case `BREAK` | 生成 `goto L<after>` 跳转到最近外层循环的出口标号 |
| **block** | `stmt()` case `{` | 递归调用 `block()`，创建嵌套作用域 |
| **assign** | `stmt()` default | 解析标识符/数组元素为左值，`=` 号右侧表达式求值，生成赋值指令 |

### 4.4 循环与判断处理

**短路求值（Short-Circuit Evaluation）：**

逻辑运算 `&&` 和 `||` 采用跳转指令实现短路语义：
- `&&`：左操作数为假时跳转到假出口，为真时继续评估右操作数
- `||`：左操作数为真时跳转到真出口，为假时继续评估右操作数
- `!`：反转真假出口

**循环嵌套与 break 支持：**

通过静态变量 `Stmt.Enclosing` 维护当前最近外层循环的引用。`Break` 构造时捕获该引用，在代码生成阶段输出 `goto L<after>` 跳转到外层循环出口，确保嵌套循环中 `break` 的正确语义。

### 4.5 数组运算处理

多维数组通过 `Access` 类处理，支持 `ID [ expr ] { [ expr ] }` 形式的数组元素访问：

- **地址计算**：`offset = index × element_width`，将数组访问归结为基地址加偏移
- **多维嵌套**：递归调用 `offset()` 逐维度计算最终偏移量
- **左值赋值**：`SetElem` 类处理 `array [ index ] = expr` 形式的数组元素更新
- **宽度参数**：`int` 宽度 4 字节，`float` 宽度 8 字节，`char`/`bool` 宽度 1 字节

### 4.6 跳转指令与标号生成

中间代码以带标号的三地址码形式输出。`Node` 基类提供统一的标号管理与代码发射接口：

- `newlabel()`：生成全局唯一递增标号（`L1`, `L2`, ...）
- `emitlabel(i)`：输出标号定义 `Li:`
- `emit(s)`：输出一条三地址码指令
- 跳转指令类型：
  - `if test goto Ln`：条件为真时跳转
  - `iffalse test goto Ln`：条件为假时跳转
  - `goto Ln`：无条件跳转
- 临时变量 `t1`, `t2`, ... 用于存储子表达式求值结果

---

## 5. 项目目录结构

```
Decaf_Code/
│
├── README.md                           ← 项目说明文档
├── test.txt                            ← 语法分析模块测试用例
├── scanner_test.txt                    ← 词法扫描模块测试用例
├── parser_test.txt                     ← 解析器测试用例
│
├── scanner/                            ← 独立词法扫描器模块
│   └── src/
│       ├── lexer/
│       │   ├── Lexer.java              ← 增强型词法分析器（注释、字符串、十六进制、科学计数法）
│       │   ├── Tag.java                ← Token 标签常量定义（扩展版）
│       │   ├── Token.java              ← Token 基类
│       │   ├── Word.java               ← 关键字/标识符 Token
│       │   ├── Num.java                ← 整数常量 Token
│       │   └── Real.java               ← 浮点数常量 Token
│       └── main/
│           └── ScannerMain.java        ← Scanner 独立运行入口
│
├── parser/                             ← 语法分析与中间代码生成模块
│   └── src/
│       ├── lexer/
│       │   ├── Lexer.java              ← 核心词法分析器（服务于 Parser）
│       │   ├── Tag.java                ← Token 标签常量定义
│       │   ├── Token.java              ← Token 基类
│       │   ├── Word.java               ← 关键字/标识符 Token（含运算符常量定义）
│       │   ├── Num.java                ← 整数常量 Token
│       │   └── Real.java               ← 浮点数常量 Token
│       │
│       ├── parser/
│       │   └── Parser.java             ← 递归下降语法分析器（语法树 + 中间代码生成）
│       │
│       ├── symbols/
│       │   ├── Type.java               ← 类型系统（int/float/char/bool 宽度与类型检查）
│       │   ├── Array.java              ← 数组类型（维度 × 元素类型宽度）
│       │   └── Env.java                ← 链式符号表（作用域嵌套与变量查找）
│       │
│       ├── inter/                      ← 中间表示（IR）节点类层次结构
│       │   ├── Node.java               ← 抽象基类（标号管理、代码发射）
│       │   ├── Expr.java               ← 表达式基类（跳转代码 emitjumps）
│       │   ├── Op.java                 ← 运算符基类（reduce 归约方法）
│       │   ├── Stmt.java               ← 语句基类（gen / display 抽象方法）
│       │   ├── Seq.java                ← 语句序列（顺序复合）
│       │   ├── Id.java                 ← 标识符节点（含偏移量）
│       │   ├── Temp.java               ← 临时变量（t1, t2, ...）
│       │   ├── Constant.java           ← 常量（True / False / 数值）
│       │   ├── Arith.java              ← 算术运算（+ - * /）
│       │   ├── Unary.java              ← 一元取负（-）
│       │   ├── Access.java             ← 数组元素访问
│       │   ├── Set.java                ← 标量赋值语句
│       │   ├── SetElem.java            ← 数组元素赋值语句
│       │   ├── Logical.java            ← 逻辑运算基类（短路求值）
│       │   ├── And.java                ← 逻辑与 &&
│       │   ├── Or.java                 ← 逻辑或 ||
│       │   ├── Not.java                ← 逻辑非 !
│       │   ├── Rel.java                ← 关系比较（< <= >= > == !=）
│       │   ├── If.java                 ← if 条件语句
│       │   ├── Else.java               ← if-else 条件语句
│       │   ├── While.java              ← while 循环
│       │   ├── Do.java                 ← do-while 循环
│       │   ├── For.java                ← for 循环
│       │   └── Break.java              ← break 跳转语句
│       │
│       └── main/
│           └── ParserMain.java         ← Parser 入口（两阶段：语法展示 + IR 生成）
│
└── .idea/                              ← IntelliJ IDEA 项目配置
```

---

## 6. 运行使用说明

### 6.1 编译命令

```bash
# 在项目根目录 Decaf_Code/ 下执行
javac -encoding UTF-8 -cp "parser/src;src" parser/src/main/ParserMain.java -d out
```

### 6.2 启动入口

```bash
java -cp "parser/src;src;out" main.ParserMain
```

程序默认读取项目根目录下的 `test.txt` 作为输入源文件。若需更换测试用例，直接修改 `test.txt` 内容即可。

### 6.3 输入测试样例

**示例：快速排序分区（Partition）片段**

```java
{
int i; int j; float v; float x; float[100] a;
while (true) {
    do i = i + 1; while (a[i] < v);
    do j = j + 1; while (a[j] > v);
    if (i >= j) break;
    x = a[i]; a[i] = a[j]; a[j] = x;
}
}
```

该样例包含了变量声明（`int` / `float` / 数组）、`while` 循环、`do-while` 循环、条件判断 `if`、`break` 跳转、标量赋值与数组元素交换，覆盖了 Decaf 语言的主要语法结构。

### 6.4 输出结果说明

程序分两阶段输出：

**第一阶段 —— 语法树展示（Parsing）：**

以缩进文本形式展示识别到的语法结构：

```
stmt : while begin
stmt : do begin
 assignment
stmt : do end
stmt : do begin
 assignment
stmt : do end
stmt : if begin
 break
stmt : if end
 assignment
 assignment
 assignment
stmt : while end
```

**第二阶段 —— 三地址中间代码（Intermediate Code）：**

以带标号的三地址指令序列输出：

```
L1:L3:   i = i + 1
L5:      t1 = i * 8
         t2 = a [ t1 ]
         if t2 < v goto L3
L4:      j = j + 1
L7:      t3 = j * 8
         t4 = a [ t3 ]
         if t4 > v goto L4
L6:      iffalse i >= j goto L8
L9:      goto L2
L8:      t5 = i * 8
         x = a [ t5 ]
L10:     t6 = i * 8
         t7 = j * 8
         t8 = a [ t7 ]
         a [ t6 ] = t8
L11:     t9 = j * 8
         a [ t9 ] = x
         goto L1
L2:
```

输出中 `t1`, `t2`, ... 为编译器生成的临时变量，`L1`, `L2`, ... 为自动分配的跳转标号。数组元素访问被展开为基址加偏移的显式计算，循环与分支语句被转化为条件跳转与无条件跳转的组合。

---

## 7. 实验核心成果

### 7.1 语法树结构输出

第一阶段成功构建并输出了抽象语法树的可视化展示。递归下降解析器按照 Decaf 文法规则自顶向下推导，将线性 Token 序列转化为结构化的树形表示。语法树直观反映了源程序的层次结构：外层 `while` 循环包含两个 `do-while` 子循环、一个 `if` 判断（含 `break`）和三条赋值语句。

### 7.2 三地址码中间代码生成效果

第二阶段成功将语法树翻译为标准的三地址中间代码（Three-Address Code）：

- **指令格式统一**：每条指令至多包含一个运算符和三个操作数地址
- **临时变量管理**：子表达式求值结果存入自动编号的临时变量（`t1`, `t2`, ...），遵循静态单赋值（SSA）风格
- **控制流结构化**：高级控制流（循环、分支）被精确翻译为标号（Label）与条件/无条件跳转指令的组合
- **数组扁平化**：多维数组访问被展开为显式的地址计算（`index × width` + 基址）
- **短路求值实现**：逻辑运算符 `&&` 和 `||` 通过跳转指令实现短路语义，避免了不必要的右操作数求值

### 7.3 关键编译技术应用

| 技术 | 应用位置 | 说明 |
|------|----------|------|
| **有限状态自动机（DFA）** | Lexer.scan() | 基于字符分类与状态转移识别 Token |
| **递归下降分析** | Parser 全部解析方法 | 每个非终结符对应一个解析函数 |
| **链式符号表** | Env（prev 指针实现作用域嵌套） | 支持块级作用域与变量遮蔽 |
| **语法制导翻译** | inter 包各节点的 gen() 方法 | 在语法分析同时生成中间代码 |
| **回填技术** | break 的 after 标号引用 | 循环出口标号在 break 生成时尚未确定，通过 Enclosing 引用引用外层循环的 after 字段实现隐式回填 |

---

## 8. 项目总结与实验心得

### 8.1 技术收获

1. **编译器前端全流程实践**：从字符流到 Token 流，再到抽象语法树，最终到中间代码，完整走通了编译器前端的每一个阶段，对编译原理课程中的形式语言、自动机、文法推导等理论概念形成了具象化的工程认知。

2. **递归下降解析器的设计与实现**：在消除左递归、提取公共左因子的文法改造过程中，深刻理解了自顶向下语法分析的前提条件与递归下降技术的适用边界。将文法产生式一对一映射为解析方法，使程序结构与语言定义高度一致，大幅降低了实现复杂度和后期维护成本。

3. **跨平台字符处理**：Windows 环境下的 `\r\n` 换行符问题是一个典型的工程细节陷阱。在 `InputStreamReader` 底层字符读取层面统一处理三种换行格式（`\r\n` / `\r` / `\n`），保证了词法分析阶段行号计数的准确性，避免因平台差异导致的 EOF 误判或 Token 丢失。

4. **中间代码生成与优化意识**：通过三地址码的生成实践，初步建立了"高级语言 → 中间表示 → 目标代码"的分层编译思维。标号分配、临时变量归约、短路求值等设计为后续代码优化和目标代码生成奠定了坚实的基础。

### 8.2 遇到的问题与解决方案

| 问题描述 | 解决方案 |
|----------|----------|
| Windows `\r\n` 换行符导致词法分析器在文件结束前误返回 EOF Token | 统一处理 `\r\n` 为单一换行符，单独 `\r` 视作换行计入行号 |
| 第一阶段语法分析消耗全部 Token 后，第二阶段（IR 生成）无法重新解析 | 为 Lexer 和 Parser 增加 `reset()` 方法，在阶段间重置输入流 |
| 数组元素地址计算的类型宽度处理 | 在 Type/Array 类型中预设 width 字段，多维数组嵌套时累乘维度大小 |

### 8.3 后续扩展方向

- **语义分析增强**：引入更完善的类型检查、作用域分析、未声明变量与不可达代码检测
- **代码优化**：实现常量折叠、公共子表达式消除、死代码删除等基本块优化 Pass
- **目标代码生成**：基于三地址码生成 x86 汇编或 JVM 字节码，实现端到端的可执行编译
- **错误恢复**：引入恐慌模式（Panic Mode）错误恢复，在一次编译中报告多个语法错误
