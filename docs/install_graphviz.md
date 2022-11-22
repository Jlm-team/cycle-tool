# 安装Graphviz

[Graphviz官方网站](https://www.graphviz.org/download/)

## 安装

#### Linux

- [Ubuntu packages](https://packages.ubuntu.com/search?keywords=graphviz&searchon=names)*

  ```bash
  sudo apt install graphviz
  ```

- [Fedora project](https://apps.fedoraproject.org/packages/graphviz)*

  ```bash
  sudo yum install graphviz
  ```

- [Debian packages](http://packages.debian.org/search?suite=all&searchon=names&keywords=graphviz)*

  ```bash
  sudo apt install graphviz
  ```

- [Stable and development rpms for Redhat Enterprise, or CentOS systems](http://rpmfind.net/linux/rpm2html/search.php?query=graphviz)* available but are out of date.

  ```bash
  sudo yum install graphviz
  ```

### Windows

请访问[Graphviz官方网站](https://www.graphviz.org/download/)下载安装包

### Mac

- [MacPorts](https://www.macports.org/)* provides both stable and development versions of Graphviz and the Mac GUI Graphviz.app. These can be obtained via the ports [graphviz](https://ports.macports.org/port/graphviz/), [graphviz-devel](https://ports.macports.org/port/graphviz-devel/), [graphviz-gui](https://ports.macports.org/port/graphviz-gui/) and [graphviz-gui-devel](https://ports.macports.org/port/graphviz-gui-devel/).

  ```bash
  sudo port install graphviz
  ```

- [Homebrew](https://brew.sh/)* [has a Graphviz port](https://formulae.brew.sh/formula/graphviz).

  ```bash
  brew install graphviz
  ```

## 配置

安装完成后，在命令行输入

```bash
dot -version
```

若能正确输出版本号，则表明配置成功，否则请将安装路径下的bin目录添加至您系统的环境变量内。
