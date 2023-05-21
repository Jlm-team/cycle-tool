package team.jlm.coderefactor.plugin.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.RegisterToolWindowTask
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.psi.PsiJvmMember
import com.intellij.refactoring.Refactoring
import com.intellij.ui.content.ContentFactory
import com.xyzboom.algorithm.graph.GEdge
import com.xyzboom.algorithm.graph.Tarjan
import mu.KotlinLogging
import team.jlm.coderefactor.code.IG
import team.jlm.coderefactor.plugin.ui.DependencyToolWindow
import team.jlm.coderefactor.plugin.ui.DependencyToolWindowFactory
import team.jlm.coderefactor.plugin.ui.DeprecatedMethodWindow
import team.jlm.dependency.DependencyInfo
import team.jlm.dependency.DependencyPosType
import team.jlm.dependency.DependencyType
import team.jlm.psi.cache.PsiMemberCacheImpl
import team.jlm.refactoring.MoveStaticMembersBetweenTwoClasses
import team.jlm.refactoring.handleDeprecatedMethod
import team.jlm.refactoring.handlerCallChain
import team.jlm.refactoring.removeUnusedImport
import team.jlm.utils.psi.getAllClassesInProject

private val logger = KotlinLogging.logger {}
val importDependencySet = HashSet<DependencyType>(
    arrayListOf(
        DependencyType.IMPORT_LIST,
        DependencyType.IMPORT_STATIC_STATEMENT,
        DependencyType.IMPORT_STATEMENT,
        DependencyType.IMPORT_STATIC_FIELD,
        DependencyType.STATIC_FIELD,
        DependencyType.STATIC_METHOD,
    )
)

class CycleDependencyAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        removeUnusedImport(project)
        val deprecatedCollection = handleDeprecatedMethod(project)
        val classes = getAllClassesInProject(project)
        classes.removeIf {
            val path = it.containingFile.originalFile.containingDirectory.toString()
            it.containingClass != null ||
                    path.contains("after", true) /*|| path.contains("docs", false)
                    || path.contains("examples", true)*/
        }
        val ig = IG(classes)
        val tarjan = Tarjan(ig)
        val result = tarjan.result
        for (row in result) {
            if (row.size != 2) {
                for (col in row) {
                    ig.delNode(col.data)
                }
            }
        }
        val candidate = result.filter { it.size == 2 }
        val refactors = candidate.mapNotNull {
            val row = it
            logger.debug { "${row[0]} ${row[1]}" }
            val edge0 = GEdge(row[0], row[1])
            val edge1 = GEdge(row[1], row[0])
            val refactor = handleEdge(ig, edge0, project) ?: handleEdge(ig, edge1, project)
            refactor?.let { r ->
                edge0 to r
            }
        }

        candidate.forEach {
            val row = it
            val edge1 = GEdge(row[0], row[1])
            val edge2 = GEdge(row[1], row[0])
            handleCallChain(ig, edge1, edge2, project)
        }

        var toolWindow = ToolWindowManager.getInstance(project).getToolWindow("dependenciesToolWindow")
        if (toolWindow == null) {
            toolWindow = ToolWindowManager.getInstance(project).registerToolWindow(
                RegisterToolWindowTask(
                    "dependenciesToolWindow",
                    contentFactory = DependencyToolWindowFactory()
                )
            )
        }
        toolWindow.contentManager.removeAllContents(true)
        val staticTableContent = ContentFactory.SERVICE.getInstance()
            .createContent(DependencyToolWindow.getWindow(refactors), "重构", false)
        val deprecatedTableContent = ContentFactory.SERVICE.getInstance().createContent(
            DeprecatedMethodWindow.getWindow(deprecatedCollection), "已弃用方法", false
        )
        toolWindow.contentManager.addContent(deprecatedTableContent)
        toolWindow.contentManager.addContent(staticTableContent)
        toolWindow.activate(null)

        /*for (p in ig.adjList) {
            val edgePair = p.value
            for (edge in edgePair.edgeOut) {
                val dpList = ig.dependencyMap[edge] ?: continue
                val dpSet = HashSet(dpList)
                dpSet.removeAll(importDependencySet)
                if (dpSet.size == 0) {
                    handleOnlyStaticFieldsInOneClass(dpList, ig, edge, project)
                    logger.debug{ (edge)
                }
            }
        }*/
        logger.debug { }
    }

    private fun handleEdge(
        ig: IG,
        edge: GEdge<String>,
        project: Project,
    ): Refactoring? {
        val dpList = ig.dependencyMap[edge] ?: return null
        return if (dpList.all { it.posType.static || it.type.static }) {
            logger.debug { edge }
            return handleOnlyStaticMembersInOneClass(dpList, edge, project)
        } else null
    }

    private fun handleCallChain(
        ig: IG,
        edge1: GEdge<String>,
        edge2: GEdge<String>,
        project: Project,
    ): Any? {
        val dpList = ig.dependencyMap[edge1] ?: mutableListOf()
        ig.dependencyMap[edge2]?.let { dpList.addAll(it) }
        val element = ArrayList<DependencyInfo>()
        for (el in dpList) {
            if ((el.type == DependencyType.NONSTATIC_METHOD || el.type == DependencyType.STATIC_METHOD) &&
                (el.posType == DependencyPosType.METHOD || el.posType == DependencyPosType.METHOD_STATIC)
            ) {
                if (el.psi is PsiMemberCacheImpl) {
                    element.add(el)
                }
            }
        }
        val hcc = handlerCallChain(project, element)
        hcc.forEach(::println)
        return ""
    }

    private fun handleOnlyStaticMembersInOneClass(
        dpList: MutableList<DependencyInfo>,
        edge: GEdge<String>,
        project: Project,
    ): Refactoring? {
        val membersFrom = HashSet<PsiJvmMember>()
        val membersTo = HashSet<PsiJvmMember>()
        for (info in dpList) {
            if (info.type.static) {
                if (info.psi is PsiMemberCacheImpl) {
                    membersFrom.add(info.psi.getPsi(project))
                }
            } else if (info.posType.static) {
                if (info.posPsi is PsiMemberCacheImpl) {
                    membersTo.add(info.posPsi.getPsi(project))
                }
            }
        }
        if (membersFrom.isEmpty() && membersTo.isEmpty()) return null
        return MoveStaticMembersBetweenTwoClasses(
            project,
            members0 = membersFrom.toArray(arrayOf()),
            targetClassName0 = edge.nodeFrom.data,
            members1 = membersTo.toArray(arrayOf()),
            targetClassName1 = edge.nodeTo.data,
        )
    }
}
