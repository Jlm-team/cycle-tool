package team.jlm.utils

import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vcs.changes.Change
import com.intellij.vcs.log.TimedVcsCommit
import git4idea.GitCommit
import git4idea.GitUtil
import git4idea.GitUtil.updateAndRefreshChangedVfs
import git4idea.changes.GitChangeUtils
import git4idea.commands.Git
import git4idea.history.GitHistoryUtils
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryManager
import java.util.*

val Project.gitRepositories: List<GitRepository>
    get() {
        val gitManager = GitRepositoryManager.getInstance(this)
        return gitManager.repositories
    }

@Suppress("UnstableApiUsage")
@get:kotlin.jvm.Throws(VcsException::class)
val GitRepository.commits: List<TimedVcsCommit>
    get() {
        val commits: MutableList<TimedVcsCommit> = ArrayList(1024)
        GitHistoryUtils.loadTimedCommits(
            project, root,
            commits::add
        )
        return commits
    }


fun GitRepository.diff(
    old: TimedVcsCommit,
    new: TimedVcsCommit,
    detectRenames: Boolean = false
): MutableCollection<Change> {
    return GitChangeUtils.getDiff(
        this, old.id.asString(), new.id.asString(), detectRenames
    ) ?: mutableListOf()
}

fun filterOnlyJavaSrc(changes: MutableCollection<Change>): MutableCollection<Change> {
    changes.removeIf {
        val virtualFile = it.virtualFile ?: return@removeIf true
        val path = virtualFile.path
        return@removeIf path.contains("src/test") || !path.endsWith(".java")
    }
    return changes
}

fun checkout(
    repository: GitRepository,
    reference: String,
    newBranch: String?,
    force: Boolean,
    detach: Boolean
): Boolean {
    val result = Git.getInstance().checkout(repository, reference, newBranch, force, detach)
    if (result.success()) {
        val head = GitUtil.getHead(repository)
        updateAndRefreshChangedVfs(repository, head)
        return true
    }
    return false
}
