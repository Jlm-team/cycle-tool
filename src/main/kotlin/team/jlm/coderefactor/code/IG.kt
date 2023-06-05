package team.jlm.coderefactor.code

import com.intellij.packageDependencies.ForwardDependenciesBuilder
import com.intellij.psi.*
import com.intellij.psi.impl.source.tree.JavaElementType
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.elementType
import com.intellij.psi.util.parentsOfType
import com.intellij.refactoring.suggested.startOffset
import team.jlm.utils.graph.GEdge
import team.jlm.utils.graph.Graph
import guru.nidi.graphviz.attribute.Style
import guru.nidi.graphviz.model.Factory
import guru.nidi.graphviz.model.Factory.graph
import guru.nidi.graphviz.model.Factory.node
import mu.KotlinLogging
import team.jlm.dependency.DependencyInfo
import team.jlm.dependency.DependencyPosType
import team.jlm.dependency.DependencyType
import team.jlm.psi.cache.IPsiCache
import team.jlm.psi.cache.PsiMemberCacheImpl
import team.jlm.utils.psi.getOuterClass
import guru.nidi.graphviz.model.Graph as VizGraph

private val logger = KotlinLogging.logger {}
private val emptyType = PsiClass::class.java

private inline fun <R> igDebug(block: () -> R) {
    block()
}

/**
 * 继承与依赖图 ----- TODO 待完善
 */
open class IG(private var classes: MutableList<PsiClass>) : Graph<String>() {
    private val classWhoseParentsAdded = HashSet<String>()
    val dependencyMap = HashMap<GEdge<String>, MutableList<DependencyInfo>>()
    private val dependTypeSet =
        HashMap<IElementType?,
                HashMap<IElementType?,
                        HashMap<IElementType?,
                                HashMap<IElementType?, ArrayList<Triple<String, String, String>>>>>>()

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

    fun addEdge(
        from: String, to: String, dependency: DependencyType = DependencyType.OTHER,
        dependencyPosType: DependencyPosType,
        posPsiCache: IPsiCache<*> = IPsiCache.EMPTY,
        psiCache: IPsiCache<*> = IPsiCache.EMPTY,
    ) {
//        vizNodes[from.data]?.let {
//            vizGraph = vizGraph.with(it.link(vizNodes[to.data]))
//        }
//        vizGraph = vizGraph.with(node(from).link(node(to)))
        val edge = super.addEdge(from, to, 1)
        dependencyMap.getOrPut(edge) { ArrayList() }
            .add(DependencyInfo(dependencyPosType, dependency, posPsiCache, psiCache))
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
        classes.removeIf { it.name?.equals(data) == true }
    }

    private fun addClassAndParents(clazz: PsiClass) {
        //如果这个类的所有子类全部正确添加，则不执行这个函数的剩余部分
        if (classWhoseParentsAdded.contains(clazz.qualifiedName)) {
            return
        }
        val clazzQualifiedName = clazz.qualifiedName ?: return
        //不存在当前扫描到的类则添加这个类
        clazzQualifiedName.let {
            classWhoseParentsAdded.add(it)
            super.addNode(it)
        }
//        clazzQualifiedName?.let { super.addNode(it) }
        if (!classes.contains(clazz)) {//排除不在项目里的类的父类
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
                        addEdge(it, it1, DependencyType.EXTENDS, DependencyPosType.EXTENDS)
                    }
                }
            }
        }
        ForwardDependenciesBuilder.analyzeFileDependencies(clazz.containingFile as PsiJavaFile)
        { dependPosEle: PsiElement, dependEle: PsiElement ->
//            logger.debug { "${dependEle.javaClass}" }
            val dependClass =
                dependEle.getOuterClass(false)
                    ?: return@analyzeFileDependencies
            val dependPosClass =
                dependPosEle.getOuterClass(false)
                    ?: return@analyzeFileDependencies
            val dependClassName = dependEle.let {
                if (it is PsiClass) {
                    it.qualifiedName ?: return@analyzeFileDependencies
                } else {
                    dependClass.qualifiedName ?: return@analyzeFileDependencies
                }
            }
            val dependPosClassName = dependPosEle.let {
                if (it is PsiClass) {
                    it.qualifiedName ?: return@analyzeFileDependencies
                } else {
                    dependPosClass.qualifiedName ?: return@analyzeFileDependencies
                }
            }
            if (dependClassName == clazzQualifiedName
                || dependClassName.startsWith(clazzQualifiedName)
                || clazzQualifiedName.startsWith(dependClassName)
                || !classes.contains(dependClass)
            ) {
                return@analyzeFileDependencies
            }
            var dependencyType = DependencyType.OTHER
            var dependencyPosType = DependencyPosType.OTHER
            var psiPosCache = IPsiCache.EMPTY
            var psiCache = IPsiCache.EMPTY
            if (dependEle !is PsiClass) {
                if (dependPosEle.elementType == JavaElementType.METHOD_REF_EXPRESSION) {
                    dependencyType = when (dependEle) {
                        is PsiMethod -> {
                            if (dependEle.modifierList.hasModifierProperty(PsiModifier.STATIC)) {
                                DependencyType.STATIC_METHOD
                            }
                            DependencyType.NONSTATIC_METHOD
                        }

                        else -> {
                            DependencyType.OTHER
                        }
                    }
                } else if (dependEle is PsiJvmMember) {
                    if (dependEle.startOffset - dependClass.startOffset < 0) {
                        logger.debug { dependEle.startOffset }
                        logger.debug { dependClass.startOffset }
                        logger.debug { dependEle.text }
                        logger.debug { dependEle.parent.text }
                        logger.debug { dependEle.parent.parent.text }
                        logger.debug { dependEle.parent.parent.parent.text }
                    }
                    psiCache = PsiMemberCacheImpl(
                        dependEle.startOffset - dependClass.startOffset,
                        dependClassName, dependEle.javaClass
                    )
                    dependencyType = if (dependEle.hasModifierProperty(PsiModifier.STATIC)) {
                        when (dependEle) {
                            is PsiMethod -> DependencyType.STATIC_METHOD
                            is PsiField -> DependencyType.STATIC_FIELD
                            else -> DependencyType.OTHER
                        }
                    } else {
                        when (dependEle) {
                            is PsiMethod -> DependencyType.NONSTATIC_METHOD
                            is PsiField -> DependencyType.NONSTATIC_FIELD
                            else -> DependencyType.OTHER
                        }
                    }
                }
            } else {
                dependencyType = dependPosEle.dependencyType
                psiPosCache = IPsiCache.EMPTY
            }
            val fieldSet = dependPosEle.parentsOfType<PsiField>().toSet()
            val methodSet = dependPosEle.parentsOfType<PsiMethod>().toSet()
            if (fieldSet.isNotEmpty()) {
                val fieldEle = fieldSet.first()
                psiPosCache = PsiMemberCacheImpl(
                    fieldEle.startOffset - dependPosClass.startOffset,
                    dependClassName,
                    fieldEle.javaClass
                )
                dependencyPosType =
                    if (fieldEle.modifierList?.hasModifierProperty(PsiModifier.STATIC) == true) {
                        DependencyPosType.FIELD_STATIC
                    } else {
                        DependencyPosType.FIELD
                    }
            } else if (methodSet.isNotEmpty()) {
                val methodEle = methodSet.first()
                psiPosCache = PsiMemberCacheImpl(
                    methodEle.startOffset - dependPosClass.startOffset,
                    dependPosClassName,
                    methodEle.javaClass
                )
                dependencyPosType = if (methodEle.modifierList.hasModifierProperty(PsiModifier.STATIC)) {
                    DependencyPosType.METHOD_STATIC
                } else {
                    DependencyPosType.METHOD
                }
            }
            igDebug {
                if (dependencyType == DependencyType.OTHER) {
                    val dependName: String = when (dependEle) {
                        is PsiClass -> {
                            dependEle.qualifiedName ?: ""
                        }

                        is PsiJvmMember -> {
                            dependEle.containingClass?.qualifiedName ?: ""
                        }

                        else -> {
                            dependEle.containingFile?.name ?: ""
                        }
                    }
                    val dependFileName = dependEle.containingFile?.name
                    val dependPosFileName = dependPosEle.containingFile?.name
                    if (clazzQualifiedName == dependName || clazzQualifiedName.startsWith(dependName)
                        || dependName.startsWith(clazzQualifiedName)
                        || dependFileName == dependPosFileName
                    ) {
                        return@igDebug
                    }
                    dependTypeSet.getOrPut(dependPosEle.elementType, ::HashMap)
                        .getOrPut(dependPosEle.parent.elementType, ::HashMap)
                        .getOrPut(dependPosEle.parent.parent.elementType, ::HashMap)
                        .merge(
                            dependEle.elementType,
                            arrayListOf(
                                Triple(
                                    dependPosEle.text ?: "",
                                    clazzQualifiedName,
                                    dependName
                                )
                            )
                        ) { a, b ->
                            a.addAll(b)
                            a
                        }
                }
            }
            addEdge(
                clazzQualifiedName, dependClassName,
                dependencyType, dependencyPosType, psiPosCache, psiCache
            )
            return@analyzeFileDependencies
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
