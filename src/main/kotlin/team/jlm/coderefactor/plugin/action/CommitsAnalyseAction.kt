package team.jlm.coderefactor.plugin.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task.Backgroundable
import com.intellij.util.SlowOperations
import com.intellij.util.ThrowableRunnable
import git4idea.GitBranch
import git4idea.branch.GitBrancher
import git4idea.history.GitHistoryUtils
import git4idea.repo.GitRepositoryManager
import team.jlm.utils.checkout

class CommitsAnalyseAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
        val grManager = project?.let { GitRepositoryManager.getInstance(it) }
        val gitRepos = grManager?.repositories ?: return
        for (repo in gitRepos) {
            println(repo)
        }
        val repo = gitRepos[0]
        val repoRoot = repo.root
        val commits = GitHistoryUtils.history(project, repoRoot)
        commits.sortBy { -it.commitTime }
        commits.forEach { println(it) }
        SlowOperations.allowSlowOperations(ThrowableRunnable {
            for (commit in commits) {
                println(commit)
                checkout(repo, commit.id.asString(), null, true, false)
                showClassesInProject(project, commit.id.asString() + ".png")
            }
        }).run { }


    }


}