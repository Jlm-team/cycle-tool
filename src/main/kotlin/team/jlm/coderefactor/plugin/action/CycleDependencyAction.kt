package team.jlm.coderefactor.plugin.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.RegisterToolWindowTask
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.packageDependencies.DependencyVisitorFactory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiJvmMember
import com.intellij.psi.PsiModifier
import com.intellij.refactoring.JavaRefactoringFactory
import com.intellij.refactoring.Refactoring
import com.intellij.ui.content.ContentFactory
import com.xyzboom.algorithm.graph.GEdge
import com.xyzboom.algorithm.graph.Tarjan
import team.jlm.coderefactor.code.DependVisitor
import team.jlm.dependency.DependencyType
import team.jlm.coderefactor.code.IG
import team.jlm.coderefactor.plugin.ui.DependencyToolWindow
import team.jlm.coderefactor.plugin.ui.DependencyToolWindowFactory
import team.jlm.psi.cache.PsiMemberCacheImpl
import team.jlm.utils.getAllClassesInProject

import mu.KotlinLogging
import team.jlm.dependency.DependencyInfo

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
        val refactors = result.filter { it.size == 2 }.mapNotNull {
            val row = it
            logger.debug { "${row[0]} ${row[1]}" }
            val edge0 = GEdge(row[0], row[1])
            val edge1 = GEdge(row[1], row[0])
            val refactor = handleEdge(ig, edge0, project) ?: handleEdge(ig, edge1, project)
            refactor?.let { r ->
                edge0 to r
            }
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
        val content = ContentFactory.SERVICE.getInstance()
            .createContent(DependencyToolWindow().getWindow(refactors), "重构", false)
        toolWindow.contentManager.addContent(content)
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
        return if (dpList.all { it.posType.static }) {
            logger.debug { edge }
            return handleOnlyStaticMembersInOneClass(dpList, ig, edge, project)
        } else null
    }

    private fun handleOnlyStaticMembersInOneClass(
        dpList: MutableList<DependencyInfo>,
        ig: IG,
        edge: GEdge<String>,
        project: Project,
    ): Refactoring? {
        val staticMembers = HashSet<PsiJvmMember>()
        for (i in dpList.indices) {
            val cache = ig.dependencyPsiMap[edge]?.get(i) ?: continue
            if (cache is PsiMemberCacheImpl) {
                val directDependPsi = cache.getPsi(project)
                staticMembers.add(directDependPsi)
            }
        }
        if (staticMembers.isEmpty()) return null
        return JavaRefactoringFactory.getInstance(project)
            .createMoveMembers(
                staticMembers.toArray(arrayOf()),
                edge.nodeTo.data, "public"
            )
    }
}
