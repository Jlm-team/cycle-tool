package team.jlm.coderefactor.plugin.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.ui.popup.JBPopupFactory
import team.jlm.utils.graph.Graph
import team.jlm.utils.graph.saveAsDependencyGraph
import team.jlm.coderefactor.plugin.service.CommitsAnalyseCacheService
import team.jlm.utils.*
import team.jlm.utils.change.analyseChangesCompletableFuture

import mu.KotlinLogging
import team.jlm.utils.psi.clearPsiMapAccordingToCommit

private val logger = KotlinLogging.logger{}

class CommitsAnalyseAction : AnAction() {
    @Suppress("UnstableApiUsage")
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val service = project.service<CommitsAnalyseCacheService>()
        logger.debug { "analysed: " }
        for (s in service.state.analysedCommits!!) {
            logger.debug { "$s, " }
        }
        logger.debug {}
        val gitRepos = project.gitRepositories
        for (repo in gitRepos) {
            logger.debug { repo }
        }
        if (gitRepos.isEmpty()) {
            JBPopupFactory.getInstance().createPopupChooserBuilder(listOf("确认"))
                .setTitle("您的项目中没有git仓库").createPopup().showInFocusCenter()
            return
        }
        val repo = gitRepos[0]
        computeWithModalProgress(project, "Analysing...") {
//            runReadAction {
            val timedVcsCommits = repo.timedVcsCommits
            var commits = repo.commits
            commits.removeIf {
                val affectedPaths = it.affectedPaths
                affectedPaths.removeIf { it1 ->
                    !it1.name.endsWith(".java")
                }
                return@removeIf affectedPaths.size > 10 || affectedPaths.size <= 1
            }
            if (commits.size > 100) {
                commits = commits.subList(commits.size - 100, commits.size)
            }
            logger.debug { "number commits to analyse: ${commits.size}" }
            for (afterCommit in commits) {
                val afterIndex = timedVcsCommits.indexOfFirst {
                    it.id == afterCommit.id
                }
                if (afterIndex < 1) {
                    continue
                }
                val beforeCommitId = timedVcsCommits[afterIndex - 1].id.asString()
                val afterCommitId = afterCommit.id.asString()
                logger.debug { "analyse: $beforeCommitId, $afterCommitId" }
                val changes = filterOnlyJavaSrc(
                    repo.diff(
                        timedVcsCommits[afterIndex - 1], timedVcsCommits[afterIndex]
                    )
                )
                val start = System.currentTimeMillis()
                var dg = Graph<String>()
                runReadAction {
                    dg = analyseChangesCompletableFuture(
                        changes, project, beforeCommitId, afterCommitId
                    )
                    logger.debug { dg }
                }
                logger.debug { "time change dp analyse used: ${(System.currentTimeMillis() - start) / 1000}" }
                clearPsiMapAccordingToCommit(beforeCommitId)
                service.state.analysedCommits?.add(beforeCommitId)
                fun last6Str(s: String) =
                    s.substring(s.length - 6, s.length)
                project.basePath?.let { it1 ->
                    dg.saveAsDependencyGraph(
                        "gitCommits/${last6Str(beforeCommitId)}-${last6Str(afterCommitId)}", it1
                    )
                }
//                break
            }
//                for (i in 0 until timedVcsCommits.size - 1) {
//                    logger.debug{ "${timedVcsCommits[i + 1]}, ${timedVcsCommits[i]}")
//                    val changes = filterOnlyJavaSrc(repo.diff(timedVcsCommits[i + 1], timedVcsCommits[i]))
//                    val dg = analyseChanges(
//                        changes, project, commits[i + 1].id.asString(), commits[i].id.asString()
//                    )
//                    logger.debug{ dg)
//                    clearPsiMapAccordingToCommit(commits[i].id.asString())
//                }
//            }.run { }
        }
    }
}
//        SlowOperations.allowSlowOperations(ThrowableRunnable {
//            for (commit in commits) {
//                logger.debug{ commit)
//                checkout(repo, commit.id.asString(), null, true, false)
//                showClassesInProject(project, commit.id.asString() + ".png")
//            }
//        }).run { }

