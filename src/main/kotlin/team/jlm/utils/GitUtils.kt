package team.jlm.utils

import com.intellij.diff.DiffContentFactory
import com.intellij.diff.actions.impl.MutableDiffRequestChain
import com.intellij.diff.chains.DiffRequestProducer
import com.intellij.diff.chains.DiffRequestProducerException
import com.intellij.diff.requests.DiffRequest
import com.intellij.openapi.diff.DiffBundle
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vcs.changes.Change
import git4idea.GitCommit
import git4idea.GitUtil
import git4idea.GitUtil.updateAndRefreshChangedVfs
import git4idea.changes.GitChangeUtils
import git4idea.commands.Git
import git4idea.history.GitHistoryUtils
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryManager

val Project.gitRepositories: List<GitRepository>
    get() {
        val gitManager = GitRepositoryManager.getInstance(this)
        return gitManager.repositories
    }

val GitRepository.commits: List<GitCommit>
    get() =
        GitHistoryUtils.history(project, root)

fun GitRepository.diff(old: GitCommit, new: GitCommit, detectRenames: Boolean = false): MutableCollection<Change> {
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

fun Change.getDiffRequests(project: Project): List<DiffRequest>? {
    val afterRevision = this.afterRevision
    val beforeRevision = this.beforeRevision
    if (afterRevision == null || beforeRevision == null) {
        return null
    }
    val beforeStr = beforeRevision.content
    val afterStr = afterRevision.content
    if (beforeStr == null || afterStr == null) {
        return null
    }
    val beforeVFile = beforeRevision.file.virtualFile
    val afterVFile = afterRevision.file.virtualFile
    if (beforeVFile == null || afterVFile == null) {
        return null
    }
    val contentFactory = DiffContentFactory.getInstance()

    val beforeContent = contentFactory.create(project, beforeVFile)
    val afterContent = contentFactory.create(project, afterVFile)
    val chain = MutableDiffRequestChain(beforeContent, afterContent)

    val allProducers: List<DiffRequestProducer?> = chain.requests
    val index = chain.index

    if (allProducers.isEmpty()) return emptyList()
    val producers = listOf(allProducers[index])
    fun collectRequests(
        allProducers: List<DiffRequestProducer?>,
        index: Int,
        indicator: ProgressIndicator
    ): List<DiffRequest> {
        if (allProducers.isEmpty()) return emptyList()
        val producersLocal = listOf(allProducers[index])
        val requestsL: MutableList<DiffRequest> = java.util.ArrayList()

        val contextL = UserDataHolderBase()

        for (producer in producersLocal) {
            try {
                if (producer != null) {
                    requestsL.add(producer.process(contextL, indicator))
                }
            } catch (ignored: DiffRequestProducerException) {
            }
        }

        return requestsL
    }
    return computeWithModalProgress(
        project,
        DiffBundle.message("progress.title.loading.requests")
    ) { indicator: ProgressIndicator? ->
        return@computeWithModalProgress collectRequests(producers, index, indicator!!)
    }
}




