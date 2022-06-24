package team.jlm.coderefactor.plugin.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.runReadAction
import team.jlm.utils.*
import team.jlm.utils.change.analyseChanges

class CommitsAnalyseAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val gitRepos = project.gitRepositories
        for (repo in gitRepos) {
            println(repo)
        }
        val repo = gitRepos[0]
        runReadAction {
            val commits = repo.commits
            for (i in 0 until commits.size - 1) {
                println("${commits[i + 1]}, ${commits[i]}")
                val changes = filterOnlyJavaSrc(repo.diff(commits[i + 1], commits[i]))
                val dg = analyseChanges(
                    changes, project, commits[i + 1].id.asString(), commits[i].id.asString()
                )
                print(dg)
                clearPsiMapAccordingToCommit(commits[i].id.asString())
            }
        }.run { }
    }
}
//        SlowOperations.allowSlowOperations(ThrowableRunnable {
//            for (commit in commits) {
//                println(commit)
//                checkout(repo, commit.id.asString(), null, true, false)
//                showClassesInProject(project, commit.id.asString() + ".png")
//            }
//        }).run { }

