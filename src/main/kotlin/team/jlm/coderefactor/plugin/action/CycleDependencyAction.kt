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
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.parentOfType
import com.intellij.refactoring.Refactoring
import com.intellij.refactoring.inline.InlineMethodHandler
import com.intellij.ui.content.ContentFactory
import mu.KotlinLogging
import team.jlm.coderefactor.code.IG
import team.jlm.coderefactor.plugin.ui.toolwindow.*
import team.jlm.dependency.DependencyInfo
import team.jlm.dependency.DependencyProviderType
import team.jlm.dependency.DependencyUserType
import team.jlm.dependency.analyseMemberGranularityDependency
import team.jlm.psi.cache.PsiMemberCacheImpl
import team.jlm.refactoring.IRefactoringProcessor
import team.jlm.refactoring.RefactoringImpl
import team.jlm.refactoring.callchain.CallChain
import team.jlm.refactoring.callchain.createInlineMethodProcessor
import team.jlm.refactoring.callchain.detectCallChain
import team.jlm.refactoring.handleDeprecatedMethod
import team.jlm.refactoring.makeStatic.createMakeMethodStaticProcess
import team.jlm.refactoring.move.staticA2B.DefaultMoveA2BMemberOptions
import team.jlm.refactoring.move.staticA2B.MoveStaticMembersProcessor
import team.jlm.refactoring.multi.MultiRefactoringProcessor
import team.jlm.refactoring.remove.unusedimport.removeUnusedImport
import team.jlm.refactoring.replace.ReplaceByReflectProcessor
import team.jlm.utils.bundle.messageBundle
import team.jlm.utils.graph.GEdge
import team.jlm.utils.graph.GNode
import team.jlm.utils.graph.Graph
import team.jlm.utils.graph.Tarjan
import team.jlm.utils.psi.getAllClassesInProject

private val logger = KotlinLogging.logger {}

class CycleDependencyAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        logger.trace { "CycleDependencyAction Start" }
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
                        } else {
                            logger.trace { "find cycle dependency: ${row[0]} -- ${row[1]}" }
                        }
                    }
                    val candidate = result.filter { it.size == 2 }
                    val allCheckedOutWindow = ContentFactory.SERVICE.getInstance()
                        .createContent(DependencyWindow.getWindow(candidate), "总览", false)
                    val graphs = candidate.mapNotNull {
                        val row = it
                        analyseMemberGranularityDependency(project, row[0].data, row[1].data)
                    }
                    logger.trace { graphs }
                    val moveA2BRefactors = candidate.mapNotNull {
                        val row = it
                        logger.debug { "${row[0]} ${row[1]}" }
                        val edge0 = GEdge(row[0], row[1])
                        val edge1 = GEdge(row[1], row[0])
                        val refactor = handleMoveA2BEdge(ig.dependencyMap[edge0], edge0, project)
                            ?: handleMoveA2BEdge(ig.dependencyMap[edge1], edge1, project)
                        refactor?.let { r ->
                            edge0 to r
                        }
                    }

                    val leverageBuiltinsRefactors = candidate.mapNotNull {
                        val row = it
                        logger.debug { "${row[0]} ${row[1]}" }
                        val edge0 = GEdge(row[0], row[1])
                        val edge1 = GEdge(row[1], row[0])
                        val refactor = handleLeverageBuiltinsEdge(ig.dependencyMap[edge0], edge0, project)
                            ?: handleLeverageBuiltinsEdge(ig.dependencyMap[edge1], edge1, project)
                        refactor?.let { r ->
                            edge0 to r
                        }
                    }
                    logger.trace { leverageBuiltinsRefactors }
                    val callChainRefactors = candidate.mapNotNull { row ->
                        val edge0 = GEdge(row[0], row[1])
                        val edge1 = GEdge(row[1], row[0])
                        val memberGraph = analyseMemberGranularityDependency(project, row[0].data, row[1].data)
                            ?: return@mapNotNull null
                        val refactor = handleCallChainEdge(ig.dependencyMap[edge0], edge0, project, memberGraph)
                            ?: handleCallChainEdge(ig.dependencyMap[edge1], edge1, project, memberGraph)
                        refactor?.let { r ->
                            edge0 to r
                        }
                    }

                    val callChainSet = HashSet<CallChain>(32)

                    candidate.forEach {
                        val row = it
                        val edge1 = GEdge(row[0], row[1])
                        val edge2 = GEdge(row[1], row[0])
                        callChainSet.addAll(handleCallChainOld(ig, edge1, edge2, project))
                    }

                    val staticTableContent = ContentFactory.SERVICE.getInstance()
                        .createContent(StaticMembersWindow.getWindow(moveA2BRefactors), "静态的依赖", false)
                    val leverageBuiltinsContent = ContentFactory.SERVICE.getInstance()
                        .createContent(
                            StaticMembersWindow.getWindow(leverageBuiltinsRefactors),
                            "可替换为Built-in的依赖",
                            false
                        )
                    val shorternCallChainContent = ContentFactory.SERVICE.getInstance()
                        .createContent(
                            StaticMembersWindow.getWindow(callChainRefactors),
                            "可以缩短的调用链",
                            false
                        )

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
                            toolWindow.contentManager.addContent(leverageBuiltinsContent)
                            toolWindow.contentManager.addContent(callChainWindow)
                            toolWindow.contentManager.addContent(shorternCallChainContent)
                            toolWindow.activate(null)
                        }
                    }.start()
                }
            }

        }
        ProgressManager.getInstance().run(task)
        logger.trace { "CycleDependencyAction End" }
    }

    private fun handleMoveA2BEdge(
        dpList: MutableList<DependencyInfo>?,
        edge: GEdge<String>,
        project: Project,
    ): Refactoring? {
        dpList ?: return null
        return if (dpList.all { it.userType.isMethod || it.providerType.isMethod }) {
            logger.debug { edge }
            return handleMoveA2B(dpList, edge, project)
        } else null
    }

    private fun handleCallChainEdge(
        dpList: MutableList<DependencyInfo>?,
        edge: GEdge<String>,
        project: Project,
        memberGraph: Graph<PsiMember>,
    ): Refactoring? {
        dpList ?: return null
        if (dpList.all {
                if (!it.providerType.isMethod && it.providerType !== DependencyProviderType.CONTAIN) return@all false
                if (it.providerType === DependencyProviderType.CONTAIN) {
                    return@all true
                }
                val providerPsi = it.providerCache.getPsi(project) as? PsiMethod
                    ?: return@all true
                val visited = HashSet<PsiMember>(32)
                val deque = ArrayDeque<PsiMember>(32)
                deque.add(providerPsi)
                while (deque.isNotEmpty()) {
                    val now = deque.removeFirst()
                    // 此处处理隐含的递归调用，例如funcA -> funcB -> funcA
                    if (visited.contains(now)) {
                        return@all false
                    }
                    val edgePair = memberGraph.adjList[GNode(providerPsi)] ?: continue
                    for (next in edgePair.edgeOut) {
                        val nextPsi = next.nodeTo.data
                        if (nextPsi is PsiMethod) {
                            // 递归函数不能内联，因此不能缩短调用链
                            if (InlineMethodHandler.checkRecursive(nextPsi)) {
                                return@all false
                            }
                        }
                    }
                    visited.add(now)
                }
                true
            }) {
            val inlineProcessors = dpList.mapNotNull {
                val psiMethod = it.providerCache.getPsi(project) as? PsiMethod
                    ?: return@mapNotNull null
                val posCache = it.posCache.getPsi(project) ?: return@mapNotNull null
                createInlineMethodProcessor(
                    project,
                    psiMethod,
                    posCache.reference,
                )
            }
            return if (inlineProcessors.isEmpty()) {
                null
            } else {
                RefactoringImpl<IRefactoringProcessor>(
                    MultiRefactoringProcessor(
                        project,
                        inlineProcessors,
                        commandName = messageBundle.getString("callChain.shortenCallChain")
                    )
                )
            }
        }
        return null
    }

    @Deprecated("plan to remove in future")
    private fun handleCallChainOld(
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

    private fun handleMoveA2B(
        dpList: MutableList<DependencyInfo>,
        edge: GEdge<String>,
        project: Project,
    ): Refactoring? {
        val membersFrom = HashSet<PsiJvmMember>()
        val membersTo = HashSet<PsiJvmMember>()
        val needMakeStatic = HashSet<PsiMethod>()
        for (info in dpList) {
            if (info.providerType.static) {
                if (info.providerCache is PsiMemberCacheImpl) {
                    membersFrom.add(info.providerCache.getPsi(project))
                }
            } else if (info.userType.static) {
                if (info.userCache is PsiMemberCacheImpl) {
                    membersTo.add(info.userCache.getPsi(project))
                }
            } else if (info.userType.isMethod) {
                val method = info.userCache.getPsi(project) as PsiMethod
                needMakeStatic.add(method)
                membersTo.add(method)
            } else {
                val method = info.providerCache.getPsi(project) as PsiMethod
                needMakeStatic.add(method)
                membersFrom.add(method)
            }
        }
        if (membersFrom.isEmpty() && membersTo.isEmpty()) return null
        val refactoringProcessors = ArrayList<IRefactoringProcessor>()
        for (psiMethod in needMakeStatic) {
            refactoringProcessors.add(
                createMakeMethodStaticProcess(project, psiMethod)
            )
        }
        if (membersFrom.isNotEmpty()) {
            refactoringProcessors.add(
                MoveStaticMembersProcessor(
                    project, DefaultMoveA2BMemberOptions(
                        membersFrom.toArray(arrayOf()),
                        edge.nodeFrom.data
                    )
                )
            )
        }
        if (membersTo.isNotEmpty()) {
            refactoringProcessors.add(
                MoveStaticMembersProcessor(
                    project, DefaultMoveA2BMemberOptions(
                        membersTo.toArray(arrayOf()),
                        edge.nodeTo.data
                    )
                )
            )
        }
        return RefactoringImpl(
            MultiRefactoringProcessor(
                project, refactoringProcessors, commandName = "Move A to B"
            ).apply { isPreviewUsages = true }
        )
    }

    private fun handleLeverageBuiltinsEdge(
        dpList: MutableList<DependencyInfo>?,
        edge: GEdge<String>,
        project: Project,
    ): Refactoring? {
        dpList ?: return null
        if (dpList.all { it.providerType == DependencyProviderType.CREATE }) {
            return RefactoringImpl(
                MultiRefactoringProcessor(
                    project, dpList.map {
                        logger.trace { it.posCache.getPsi(project) }
                        ReplaceByReflectProcessor(
                            project,
                            psiNewExpression = it.posCache.getPsi(project)!!.parentOfType()!!
                        )
                    },
                    commandName = "LeverageBuiltins"
                )
            )
        }
        return null
    }
}
