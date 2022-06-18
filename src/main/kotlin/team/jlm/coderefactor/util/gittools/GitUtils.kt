package team.jlm.coderefactor.util.gittools

import com.intellij.openapi.project.Project
import com.jetbrains.rd.util.Callable
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevTree
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.treewalk.AbstractTreeIterator
import org.eclipse.jgit.treewalk.CanonicalTreeParser
import org.eclipse.jgit.treewalk.TreeWalk
import team.jlm.coderefactor.util.gittools.entity.DiffInfo
import team.jlm.coderefactor.util.gittools.tools.getDiffInfo
import team.jlm.coderefactor.util.gittools.tools.getPsiJavaFile
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.file.Paths
import java.util.concurrent.Executors

class GitUtils(rpoDir: String) {
    private val localGitRpoDir: String = rpoDir
    private lateinit var git: Git
    private lateinit var rep: Repository

    init {
        try {
            this.rep =
                FileRepositoryBuilder().setGitDir(Paths.get(this.localGitRpoDir, ".git").toFile()).build()
            this.git = Git(this.rep)
        } catch (e: IOException) {
            if (e is FileNotFoundException) {
                initGit()
            } else
                throw e
        }
    }

    private fun initGit() {
        this.git = Git.init().setDirectory(File(localGitRpoDir)).call()
    }

    fun checkOut(branchName: String) {
        this.git.checkout().setName(branchName).call()
    }

    fun getBranchsName(): List<String> {
        val res = ArrayList<String>()
        for (i in this.git.branchList().call()) {
            res.add(i.name)
        }
        return res
    }

    fun getCommits(): List<ObjectId> {
        val res = ArrayList<ObjectId>()
        for (i in this.git.log().call()) {
            res.add(i.id)
        }
        return res
    }

    private fun prepareTreeParser(commitid: ObjectId): AbstractTreeIterator {
        val treeParser = CanonicalTreeParser()
        try {
            val rep = this.rep
            val walk = RevWalk(rep)
            val commit = walk.parseCommit(commitid)
            val tree = walk.parseTree(commit.tree.id)
            val reader = rep.newObjectReader()
            treeParser.reset(reader, tree.id)
            walk.dispose()
        } catch (e: Exception) {
            throw e
        }
        return treeParser
    }

    fun getDiffBetweenCommit(newCommitId: ObjectId, oldCommitId: ObjectId, project: Project, threadNum: Int) {
        val olddTree = prepareTreeParser(oldCommitId)
        val newTree = prepareTreeParser(newCommitId)
        val res = git.diff().setNewTree(newTree).setOldTree(olddTree).call()
        batchDiffBetweenFiles(res, project, threadNum)
    }

    private fun getFileContent(path: String, tree: RevTree, walk: RevWalk): String {
        val treeWalk = TreeWalk.forPath(rep, path, tree)
        val blodid = treeWalk.getObjectId(0)
        val loader = rep.open(blodid)
        val bytes = loader.getBytes()
        walk.dispose()
        return String(bytes)
    }

    private fun getBranchSpecificFileContext(path: String): String {
        val branchRef = rep.exactRef("/refs/heads/" + rep.branch)
        val obid = branchRef.objectId
        val walk = RevWalk(rep)
        val tree = walk.parseTree(obid)
        return getFileContent(path, tree, walk)
    }


    private fun getDiffBetweenFiles(
        commitDiff: List<DiffEntry>,
        project: Project
    ): ArrayList<DiffInfo> {

        var res = ArrayList<DiffInfo>()
        try {
            for (diff in commitDiff) {
                val newfilePath = diff.newPath
                val oldfilePath = diff.oldPath

                if (newfilePath.contains("/src/test/java/"))//测试文件，跳过
                    continue
                if (!newfilePath.endsWith(".java") || diff.changeType == DiffEntry.ChangeType.DELETE) //删除或者非Java，跳过
                    continue
                if (diff.changeType == DiffEntry.ChangeType.ADD || diff.changeType == DiffEntry.ChangeType.RENAME) //添加文件或者重命名，跳过
                    continue
                val newClassContent = getBranchSpecificFileContext(newfilePath)
                val oldClassContent = getBranchSpecificFileContext(oldfilePath)
                val newPsiJavaFile = getPsiJavaFile(project, newClassContent)
                val oldPsiJavaFile = getPsiJavaFile(project, oldClassContent)
                res = getDiffInfo(newPsiJavaFile, oldPsiJavaFile, newfilePath)
            }
        } catch (e: IOException) {
            throw e
        }
        return res
    }

    private fun batchDiffBetweenFiles(
        commitDiff: List<DiffEntry>,
        project: Project,
        threadNum: Int
    ): ArrayList<DiffInfo> {
        if (commitDiff.size <= threadNum)
            return getDiffBetweenFiles(commitDiff, project)
        else {
            val res = ArrayList<DiffInfo>()
            val taskNum: Int = (commitDiff.size / threadNum) + 1
            val taskList = ArrayList<ArrayList<DiffEntry>>()
            val executorService = Executors.newFixedThreadPool(threadNum)
            val tasks = ArrayList<Callable<ArrayList<DiffInfo>>>()
            for (i in 0..threadNum-1) {
                val fromIndex = i * taskNum;
                var toIndex = 0

                if ((i + 1) * taskNum < commitDiff.size)
                    toIndex = (i + 1) * taskNum
                else
                    toIndex = commitDiff.size

                taskList.add(ArrayList(commitDiff.subList(fromIndex, toIndex)))
            }
            for (i in taskList) {
                val task =
                    Callable<ArrayList<DiffInfo>>(fun(): ArrayList<DiffInfo> {
                        return getDiffBetweenFiles(
                            i,
                            project
                        )
                    })
                tasks.add(task)
            }
            try {
                val resFuture = executorService.invokeAll(tasks)
                for (i in resFuture) {
                    res.addAll(i.get())
                }
            } catch (e: Exception) {
                throw e
            } finally {
                executorService.shutdown()
            }
            return res
        }
    }
}
