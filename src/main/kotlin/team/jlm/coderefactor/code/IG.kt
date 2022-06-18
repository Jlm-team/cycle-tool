package team.jlm.coderefactor.code

import com.intellij.packageDependencies.ForwardDependenciesBuilder
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.impl.source.tree.JavaElementType.*
import com.intellij.psi.impl.source.tree.TreeElement
import com.intellij.psi.util.elementType
import com.xyzboom.algorithm.graph.GEdge
import com.xyzboom.algorithm.graph.Graph
import guru.nidi.graphviz.attribute.Style
import guru.nidi.graphviz.model.Factory
import guru.nidi.graphviz.model.Factory.graph
import guru.nidi.graphviz.model.Factory.node
import guru.nidi.graphviz.model.Graph as VizGraph


/**
 * 继承与依赖图 ----- TODO 待完善
 */
open class IG(private var classes: List<PsiClass>) : Graph<String>() {
    private val classWhoseParentsAdded = HashSet<String>()
    private val dependencyMap = HashMap<GEdge<String>, DependencyType>()

    init {
        for (clazz in classes) {
            addClassAndParents(clazz)
        }

        //释放Set的内存
        classWhoseParentsAdded.clear()
//        for (vizNode in vizNodes.values) {
//            vizGraph = vizGraph.with(vizNode as LinkSource)
//        }
    }

    private fun addEdge(from: String, to: String, dependency: DependencyType = DependencyType.DEPEND) {
//        vizNodes[from.data]?.let {
//            vizGraph = vizGraph.with(it.link(vizNodes[to.data]))
//        }
//        vizGraph = vizGraph.with(node(from).link(node(to)))
        val edge = super.addEdge(from, to, 1)
        dependencyMap[edge] = dependency
    }

    override fun delNode(data: String) {
        val node = getNode(data)
        for (edge in adjList[node]!!.edgeOut) {
            dependencyMap.remove(edge)
            adjList[edge.nodeTo]!!.edgeIn.remove(edge)
        }
        for (edge in adjList[node]!!.edgeIn) {
            dependencyMap.remove(edge)
            adjList[edge.nodeFrom]!!.edgeOut.remove(edge)
        }
        adjList.remove(node)
    }

    private fun addClassAndParents(clazz: PsiClass) {
        //如果这个类的所有子类全部正确添加，则不执行这个函数的剩余部分
        if (classWhoseParentsAdded.contains(clazz.qualifiedName)) {
            return
        }
        val clazzQualifiedName = clazz.qualifiedName
        //不存在当前扫描到的类则添加这个类
        clazzQualifiedName?.let {
            classWhoseParentsAdded.add(it)
            super.addNode(it)
        }
//        clazzQualifiedName?.let { super.addNode(it) }
        if (!classes.contains(clazz) || clazzQualifiedName == null) {//排除不在项目里的类的父类
            return
        }
        val parents = clazz.supers
//         clazz.extendsList?.referenceElements
        //如果一个类的父类在psi中检查不到，说明它是java.lang.Object,因此这里排除了只继承了Object的类
        if (parents.size != 1 || parents[0].supers.isNotEmpty()) {
            for (parent in parents) {
                parent as PsiClass
                if (parent.supers.isEmpty()) {//父类是Object但是子类还实现了其他接口，因此此处忽略Object
                    continue
                }
                addClassAndParents(parent)

                clazz.name?.let {
                    parent.name?.let { it1 ->
                        addEdge(it, it1, DependencyType.EXTENDS)
                    }
                }
            }
        }
        ForwardDependenciesBuilder.analyzeFileDependencies(clazz.containingFile as PsiJavaFile)
        { dependElement: PsiElement, selfElement: PsiElement ->
            run {
                if (selfElement is PsiClass) {
                    println(dependElement.elementType)
                    var dependency: DependencyType = DependencyType.DEPEND
                    if (dependElement is TreeElement) {
                        dependency = dependElement.dependType
                    } else {
                        println("not java code")
                    }
                    selfElement.name?.let { it1 -> clazz.name?.let { addEdge(it, it1, dependency) } }
                }
            }
        }
//        for (dependency in dependencies) {
////            dependency.
//            if (dependency is PsiClass)
//                dependency.name?.let { it1 -> clazz.name?.let { addDependencyEdge(it, it1) } }
//        }
    }

    fun toGraphvizGraph(): VizGraph {
        var viz = graph().directed()
        for (pair in adjList) {
            for (edgeOut in pair.value.edgeOut) {
                viz = if (dependencyMap[edgeOut] == DependencyType.EXTENDS) {
                    viz.with(node(pair.key.data).link(node(edgeOut.nodeTo.data)))
                } else {
                    viz.with(
                        node(pair.key.data).link(
                            Factory.to(node(edgeOut.nodeTo.data)).with(Style.DASHED)
                        )
                    )
                }
            }
        }
        return viz
    }
}
