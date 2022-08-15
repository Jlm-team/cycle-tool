package team.jlm.coderefactor.plugin.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.runReadAction
import git4idea.history.GitHistoryUtils
import team.jlm.utils.*
import team.jlm.utils.change.analyseChanges

class CommitsAnalyseAction : AnAction() {
    @Suppress("UnstableApiUsage")
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val gitRepos = project.gitRepositories
        for (repo in gitRepos) {
            println(repo)
        }
        val repo = gitRepos[0]
        computeWithModalProgress(project, "Analysing...") {
            runReadAction {
                val timedVcsCommits = repo.timedVcsCommits
                val commits = repo.commits
                commits.removeIf {
                    val affectedPaths = it.affectedPaths
                    affectedPaths.removeIf { it1 ->
                        !it1.name.endsWith(".java")
                    }
                    return@removeIf affectedPaths.size > 10 || affectedPaths.size == 0
                }
                for (afterCommit in commits) {
                    val afterIndex = timedVcsCommits.indexOfFirst {
                        it.id == afterCommit.id
                    }
                    if (afterIndex < 1) {
                        continue
                    }
                    val beforeCommitId = timedVcsCommits[afterIndex - 1].id.asString()
                    val afterCommitId = afterCommit.id.asString()
                    println("analyse: $beforeCommitId, $afterCommitId")
                    val changes = filterOnlyJavaSrc(
                        repo.diff(
                            timedVcsCommits[afterIndex - 1], timedVcsCommits[afterIndex]
                        )
                    )
                    val dg = analyseChanges(
                        changes, e, afterCommitId, beforeCommitId
                    )
                    println(dg)
                    clearPsiMapAccordingToCommit(beforeCommitId)
                }
//                for (i in 0 until timedVcsCommits.size - 1) {
//                    println("${timedVcsCommits[i + 1]}, ${timedVcsCommits[i]}")
//                    val changes = filterOnlyJavaSrc(repo.diff(timedVcsCommits[i + 1], timedVcsCommits[i]))
//                    val dg = analyseChanges(
//                        changes, project, commits[i + 1].id.asString(), commits[i].id.asString()
//                    )
//                    println(dg)
//                    clearPsiMapAccordingToCommit(commits[i].id.asString())
//                }
            }.run { }
        }
    }
}
//        SlowOperations.allowSlowOperations(ThrowableRunnable {
//            for (commit in commits) {
//                println(commit)
//                checkout(repo, commit.id.asString(), null, true, false)
//                showClassesInProject(project, commit.id.asString() + ".png")
//            }
//        }).run { }

