# 重构两文件循环依赖的IDEA插件

## Requirements

1. jbr >= 11 (JetBrains Runtime)

2. Intellij IDEA

3. Graphviz  [more details](./docs/install_graphviz.md)

## 功能介绍

### 1. 查看所有类

分析整个项目并且将所有java类间的依赖关系，并将含有循环依赖的类进行绘图，结果以图片输出。

[more details](./docs/details/show_all_classes.md)

### 2. 分析git提交

分析整个项目中的提交中的代码变动的依赖情况，并在控制台进行输出。

[more details](./docs/details/commits_analyse.md)

### 3. 循环依赖分析

分析项目当前的循环依赖情况，并自动重构可以执行重构操作的循环依赖项。

## Getting Start

使用Itellj IDEA打开本项目，将项目的gradle JVM设置为jbr >= 11，如下图所示

![gradle_setting](./docs/img/readme/gradle_setting.png)

按Ctrl F9或者点击构建进行项目的构建。期间需要联网并且保证能连接到github。

在右上角的配置中新建一条运行配置来启动idea插件。如下所示：

![](./docs/img/readme/add_configure.png)

![](./docs/img/readme/add_gradle.png)

![](./docs/img/readme/run_ide.png)

添加运行配置后即可直接运行。运行后弹出新的idea窗口，在新窗口中点击“Code”即可看到新加入的功能。

![](./docs/img/readme/running.png)

## 示例

[示例项目](https://github.com/Jlm-team/cycleToolSamples/)