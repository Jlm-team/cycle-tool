package team.jlm.coderefactor.code

import com.intellij.psi.PsiClass
import guru.nidi.graphviz.attribute.Style
import guru.nidi.graphviz.model.Factory
import guru.nidi.graphviz.model.Factory.graph
import guru.nidi.graphviz.model.Factory.node
import mu.KotlinLogging
import team.jlm.dependency.DependenciesBuilder
import team.jlm.dependency.DependencyInfo
import team.jlm.dependency.DependencyProviderType
import team.jlm.dependency.DependencyUserType
import team.jlm.psi.cache.IPsiCache
import team.jlm.utils.graph.GEdge
import team.jlm.utils.graph.Graph
import guru.nidi.graphviz.model.Graph as VizGraph

private val logger = KotlinLogging.logger {}
private val emptyType = PsiClass::class.java

private inline fun <R> igDebug(block: () -> R) {
    block()
}

/**
 * 继承与依赖图 ----- TODO 待完善
 */
open class IG : Graph<String> {
    constructor(classes: MutableList<PsiClass>) : super() {
        this.classes = classes
        this.classWhoseParentsAdded = HashSet()
        this.dependencyMap = HashMap()
        for (clazz in classes) {
            addClassAndParents(clazz)
        }
        // 置空以gc无用的引用
        this.classes = null
        classWhoseParentsAdded = null
    }

    constructor(psiClass: PsiClass) : super() {
        dependencyMap = HashMap()
        addOneClass(psiClass)
    }

    private var classes: MutableList<PsiClass>? = null
    private var classWhoseParentsAdded: HashSet<String>? = null
    val dependencyMap: HashMap<GEdge<String>, MutableList<DependencyInfo>>

    fun addEdge(
        from: String, to: String,
        providerType: DependencyProviderType = DependencyProviderType.OTHER,
        userType: DependencyUserType,
        providerCache: IPsiCache<*> = IPsiCache.EMPTY,
        userCache: IPsiCache<*> = IPsiCache.EMPTY,
    ) {
//        vizNodes[from.data]?.let {
//            vizGraph = vizGraph.with(it.link(vizNodes[to.data]))
//        }
//        vizGraph = vizGraph.with(node(from).link(node(to)))
        val edge = super.addEdge(from, to, 1)
        dependencyMap.getOrPut(edge) { ArrayList() }
            .add(DependencyInfo(userType, providerType, userCache, providerCache))
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
        if (classWhoseParentsAdded!!.contains(clazz.qualifiedName)) {
            return
        }
        val clazzQualifiedName = clazz.qualifiedName ?: return
        //不存在当前扫描到的类则添加这个类
        clazzQualifiedName.let {
            classWhoseParentsAdded!!.add(it)
            super.addNode(it)
        }
//        clazzQualifiedName?.let { super.addNode(it) }
        if (!classes!!.contains(clazz)) {//排除不在项目里的类的父类
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

                clazz.qualifiedName?.let {
                    parent.qualifiedName?.let { it1 ->
                        addEdge(it, it1, DependencyProviderType.EXTENDS, DependencyUserType.EXTENDS)
                    }
                }
            }
        }
        DependenciesBuilder.analyzePsiDependencies(
            clazz,
            filter@{ providerClass ->
                val providerClassName = providerClass.qualifiedName ?: return@filter false
                if (providerClassName == clazzQualifiedName
                    || providerClassName.startsWith(clazzQualifiedName)
                    || clazzQualifiedName.startsWith(providerClassName)
                    || !classes!!.contains(providerClass)
                ) {
                    return@filter false
                }
                return@filter true
            }
        ) { _, providerClass, providerType, userType, providerPsiCache, userPsiCache ->
            val providerName = providerClass.qualifiedName ?: return@analyzePsiDependencies
            addEdge(clazzQualifiedName, providerName, providerType, userType, providerPsiCache, userPsiCache)
        }
    }

    private fun addOneClass(psiClass: PsiClass) {
        val clazzQualifiedName = psiClass.qualifiedName ?: return
        val providers = HashSet<PsiClass>()
        DependenciesBuilder.analyzePsiDependencies(
            psiClass,
            filter@{ providerClass ->
                val providerClassName = providerClass.qualifiedName ?: return@filter false
                if (providerClassName == clazzQualifiedName
                    || providerClassName.startsWith(clazzQualifiedName)
                    || clazzQualifiedName.startsWith(providerClassName)
                ) {
                    return@filter false
                }
                return@filter true
            }
        ) { _, providerClass, providerType, userType, providerPsiCache, userPsiCache ->
            val providerName = providerClass.qualifiedName ?: return@analyzePsiDependencies
            providers.add(providerClass)
            addEdge(clazzQualifiedName, providerName, providerType, userType, providerPsiCache, userPsiCache)
        }
        for (provider in providers) {
            DependenciesBuilder.analyzePsiDependencies(
                psiClass,
                filter@{ providerClass ->
                    providerClass !== psiClass
                }
            ) { _, providerClass, providerType, userType, providerPsiCache, userPsiCache ->
                val providerName = providerClass.qualifiedName ?: return@analyzePsiDependencies
                addEdge(providerName, clazzQualifiedName, providerType, userType, providerPsiCache, userPsiCache)
            }
        }
    }

    fun toGraphvizGraph(): VizGraph {
        var viz = graph().directed()
        for (pair in adjList) {
            for (edgeOut in pair.value.edgeOut) {
                viz = if (dependencyMap[edgeOut]?.contains(DependencyInfo.Extends) == true) {
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
