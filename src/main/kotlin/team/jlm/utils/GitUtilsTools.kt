package team.jlm.utils

import git4idea.GitUtil
import git4idea.GitUtil.updateAndRefreshChangedVfs
import git4idea.commands.Git
import git4idea.repo.GitRepository

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