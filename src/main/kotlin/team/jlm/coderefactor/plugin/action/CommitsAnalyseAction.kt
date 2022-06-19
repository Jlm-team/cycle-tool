package team.jlm.coderefactor.plugin.action

import com.intellij.diff.contents.FileDocumentContentImpl
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diff.SimpleDiffRequest
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task.Backgroundable
import com.intellij.util.SlowOperations
import com.intellij.util.ThrowableRunnable
import git4idea.GitBranch
import git4idea.branch.GitBrancher
import git4idea.changes.GitChangeUtils
import git4idea.history.GitHistoryUtils
import git4idea.repo.GitRepositoryManager
import team.jlm.utils.checkout
import team.jlm.utils.getDiffRequests

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
        val changes = GitChangeUtils.getDiff(
            repo, commits[0].id.asString(), commits[1].id.asString(),
            false
        )
        if (changes != null) {
            for (change in changes) {
                println(change)
                val diffs = change.getDiffRequests(project)
                if (diffs != null) {
                    for (diff in diffs) {
                        println(diff)
                    }
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