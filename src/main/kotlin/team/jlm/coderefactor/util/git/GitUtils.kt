package team.jlm.coderefactor.util.git

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevTree
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.treewalk.AbstractTreeIterator
import org.eclipse.jgit.treewalk.CanonicalTreeParser
import org.eclipse.jgit.treewalk.TreeWalk
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.file.Paths

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

    public fun getDiffBetweenCommit(newCommitId: ObjectId, oldCommitId: ObjectId) {
        val olddTree = prepareTreeParser(oldCommitId)
        val newTree = prepareTreeParser(newCommitId)
        val res = git.diff().setNewTree(newTree).setOldTree(olddTree).call()
        getDiffBetweenFiles(res)
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

    private fun getDiffBetweenFiles(commitDiff: List<DiffEntry>) {
        val out = ByteArrayOutputStream()
        val df = DiffFormatter(out)
        df.setRepository(this.rep)

        try {
            for (diff in commitDiff) {
                val newfilePath = diff.newPath
                val oldfilePath = diff.oldPath

                if (newfilePath.contains("/src/test/java/"))
                    continue
                if (!newfilePath.endsWith(".java") || diff.changeType == DiffEntry.ChangeType.DELETE)
                    continue
                if (diff.changeType == DiffEntry.ChangeType.ADD || diff.changeType == DiffEntry.ChangeType.RENAME)
                    continue
                val fileHeader = df.toFileHeader(diff)
                val addLines = ArrayList<Pair<Int, Int>>()
                val delLines = ArrayList<Pair<Int, Int>>()
                for (edit in fileHeader.toEditList()) {
                    if (edit.lengthA > 0)
                        delLines.add(Pair(edit.beginA, edit.beginB))
                    if (edit.lengthB > 0)
                        addLines.add(Pair(edit.beginB, edit.beginA))
                }
                val newClassContent = getBranchSpecificFileContext(newfilePath)
                val oldClassContent = getBranchSpecificFileContext(oldfilePath)
//TODO:语法分析

            }
        }catch (e:IOException)
        {
            throw e
        }
    }
}