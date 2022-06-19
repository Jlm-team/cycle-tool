package team.jlm.coderefactor.plugin.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.util.SlowOperations
import com.intellij.util.ThrowableRunnable
import team.jlm.utils.*

class CommitsAnalyseAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val gitRepos = project.gitRepositories
        for (repo in gitRepos) {
            println(repo)
        }
        val repo = gitRepos[0]
        val commits = repo.commits
        commits.sortedBy { -it.commitTime }
        commits.forEach { println(it) }
        val changes = filterOnlyJavaSrc(repo.diff(commits[1], commits[0]))
        for (change in changes) {
            println(change)
            val diffs = change.getDiffRequests(project)
            if (diffs != null) {
                for (diff in diffs) {
                    println(diff)
                }
            }
        }
        SlowOperations.allowSlowOperations(ThrowableRunnable {
            for (commit in commits) {
                println(commit)
                checkout(repo, commit.id.asString(), null, true, false)
                showClassesInProject(project, commit.id.asString() + ".png")
            }
        }).run { }
    }
}