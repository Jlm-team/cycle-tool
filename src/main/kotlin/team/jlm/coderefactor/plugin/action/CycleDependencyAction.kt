package team.jlm.coderefactor.plugin.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.RegisterToolWindowTask
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.psi.PsiJvmMember
import com.intellij.refactoring.Refactoring
import com.intellij.ui.content.ContentFactory
import mu.KotlinLogging
import team.jlm.coderefactor.code.IG
import team.jlm.coderefactor.plugin.ui.toolwindow.*
import team.jlm.dependency.DependencyInfo
import team.jlm.dependency.DependencyProviderType
import team.jlm.dependency.DependencyUserType
import team.jlm.psi.cache.PsiMemberCacheImpl
import team.jlm.refactoring.handleDeprecatedMethod
import team.jlm.refactoring.move.callchain.CallChain
import team.jlm.refactoring.move.callchain.detectCallChain
import team.jlm.refactoring.move.staticA2B.MoveStaticMembersBetweenTwoClasses
import team.jlm.refactoring.remove.unusedimport.removeUnusedImport
import team.jlm.utils.graph.GEdge
import team.jlm.utils.graph.Tarjan
import team.jlm.utils.psi.getAllClassesInProject

private val logger = KotlinLogging.logger {}
val importDependencySet = HashSet<DependencyProviderType>(
    arrayListOf(
        DependencyProviderType.IMPORT_LIST,
        DependencyProviderType.IMPORT_STATIC_STATEMENT,
        DependencyProviderType.IMPORT_STATEMENT,
        DependencyProviderType.IMPORT_STATIC_FIELD,
        DependencyProviderType.STATIC_FIELD,
        DependencyProviderType.STATIC_METHOD,
    )
)

class CycleDependencyAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

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

        if (Messages.showYesNoDialog(
                "在项目较大时收集Import将会锁定UI一段时间，是否继续",
                "提示",
                Messages.getQuestionIcon()
            ) == Messages.YES
        ) {
            val map = removeUnusedImport(project)
            val unUsedImportContent = ContentFactory.SERVICE.getInstance()
                .createContent(UnUsedImportWindow.getWindow(map), "未使用的Import", false)
            Thread {
                ApplicationManager.getApplication().invokeLater {
                    toolWindow.contentManager.addContent(unUsedImportContent)
                }
            }.start()
        }
        val task = object : Task.Modal(project, "循环依赖分析中", true) {
            override fun run(indicator: ProgressIndicator) {
                ApplicationManager.getApplication().runReadAction {
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
                    val allCheckedOutWindow = ContentFactory.SERVICE.getInstance()
                        .createContent(DependencyWindow.getWindow(candidate), "总览", false)
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

                    val callChainSet = HashSet<CallChain>(32)

                    candidate.forEach {
                        val row = it
                        val edge1 = GEdge(row[0], row[1])
                        val edge2 = GEdge(row[1], row[0])
                        callChainSet.addAll(handleCallChain(ig, edge1, edge2, project))
                    }

                    val staticTableContent = ContentFactory.SERVICE.getInstance()
                        .createContent(StaticMembersWindow.getWindow(refactors), "静态的依赖", false)
                    val deprecatedTableContent = ContentFactory.SERVICE.getInstance().createContent(
                        DeprecatedMethodWindow.getWindow(deprecatedCollection), "已弃用方法", false
                    )
                    val callChainWindow = ContentFactory.SERVICE.getInstance().createContent(
                        CallChainWindow.getWindow(callChainSet), "可缩短的调用链", false
                    )
                    Thread {
                        ApplicationManager.getApplication().invokeLater {
                            toolWindow.contentManager.addContent(allCheckedOutWindow, 0)
                            toolWindow.contentManager.addContent(staticTableContent)
                            toolWindow.contentManager.addContent(deprecatedTableContent)
                            toolWindow.contentManager.addContent(callChainWindow)
                            toolWindow.activate(null)
                        }
                    }.start()
                }
            }

        }
        ProgressManager.getInstance().run(task)
    }

    private fun handleEdge(
        ig: IG,
        edge: GEdge<String>,
        project: Project,
    ): Refactoring? {
        val dpList = ig.dependencyMap[edge] ?: return null
        return if (dpList.all { it.userType.static || it.providerType.static }) {
            logger.debug { edge }
            return handleOnlyStaticMembersInOneClass(dpList, edge, project)
        } else null
    }

    private fun handleCallChain(
        ig: IG,
        edge1: GEdge<String>,
        edge2: GEdge<String>,
        project: Project,
    ): ArrayList<CallChain> {
        val dpList = ig.dependencyMap[edge1] ?: mutableListOf()
        ig.dependencyMap[edge2]?.let { dpList.addAll(it) }
        val element = ArrayList<DependencyInfo>()
        for (el in dpList) {
            if ((el.providerType == DependencyProviderType.NONSTATIC_METHOD || el.providerType == DependencyProviderType.STATIC_METHOD) &&
                (el.userType == DependencyUserType.METHOD || el.userType == DependencyUserType.METHOD_STATIC)
            ) {
                if (el.providerCache is PsiMemberCacheImpl) {
                    element.add(el)
                }
            }
        }
        return detectCallChain(project, element)
    }

    private fun handleOnlyStaticMembersInOneClass(
        dpList: MutableList<DependencyInfo>,
        edge: GEdge<String>,
        project: Project,
    ): Refactoring? {
        val membersFrom = HashSet<PsiJvmMember>()
        val membersTo = HashSet<PsiJvmMember>()
        for (info in dpList) {
            if (info.providerType.static) {
                if (info.providerCache is PsiMemberCacheImpl) {
                    membersFrom.add(info.providerCache.getPsi(project))
                }
            } else if (info.userType.static) {
                if (info.userCache is PsiMemberCacheImpl) {
                    membersTo.add(info.userCache.getPsi(project))
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
