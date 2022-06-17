package team.jlm.coderefactor.util.gittools

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.jetbrains.rd.util.Callable
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
import team.jlm.coderefactor.util.gittools.tools.getJavaClassMethods
import team.jlm.coderefactor.util.gittools.tools.getPsiClass
import team.jlm.coderefactor.util.gittools.tools.isSameMethods
import java.io.ByteArrayOutputStream
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

    fun getDiffBetweenCommit(newCommitId: ObjectId, oldCommitId: ObjectId, project: Project) {
        val olddTree = prepareTreeParser(oldCommitId)
        val newTree = prepareTreeParser(newCommitId)
        val res = git.diff().setNewTree(newTree).setOldTree(olddTree).call()
        getDiffBetweenFiles(res, project)
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
    ): ArrayList<HashMap<String, HashMap<PsiClass, ArrayList<PsiMethod>>>> {
        var FilePath = ""
        val res = hashMapOf<PsiClass, ArrayList<PsiMethod>>()
        val returnRes = ArrayList<HashMap<String, HashMap<PsiClass, ArrayList<PsiMethod>>>>()
        try {
            for (diff in commitDiff) {
                val newfilePath = diff.newPath
                FilePath = newfilePath
                val oldfilePath = diff.oldPath

                if (newfilePath.contains("/src/test/java/"))//测试文件，跳过
                    continue
                if (!newfilePath.endsWith(".java") || diff.changeType == DiffEntry.ChangeType.DELETE) //删除或者非Java，跳过
                    continue
                if (diff.changeType == DiffEntry.ChangeType.ADD || diff.changeType == DiffEntry.ChangeType.RENAME) //添加文件或者重命名，跳过
                    continue
                val newClassContent = getBranchSpecificFileContext(newfilePath)
                val oldClassContent = getBranchSpecificFileContext(oldfilePath)
                val newMethods = getJavaClassMethods(getPsiClass(project, newClassContent))
                val oldMethods = getJavaClassMethods(getPsiClass(project, oldClassContent))
                for ((k, v) in newMethods) {
                    if (oldMethods.contains(k)) { //判断新提交中的类是否为新增
                        val newMethod = ArrayList<PsiMethod>()
                        for (m in v) { //若不是，则遍历方法

                            for (oldm in oldMethods.get(k)!!) {
                                if (isSameMethods(m, oldm)) //找到相同的函数，说明未改变
                                    break
                            }
                            //未找到，说明新增
                            newMethod.add(m)
                        }
                        res.put(k, newMethod)
                    } else { //旧版本不存在，为新增类
                        res.put(k, v)
                    }

                }
                returnRes.add(hashMapOf(Pair(FilePath, res)))
            }
        } catch (e: IOException) {
            throw e
        }
        return returnRes
    }

    private fun batchDiffBetweenFiles(
        commitDiff: List<DiffEntry>,
        project: Project,
        threadNum: Int
    ):  ArrayList<HashMap<String, HashMap<PsiClass, ArrayList<PsiMethod>>>>{
        if (commitDiff.size <= threadNum)
            return getDiffBetweenFiles(commitDiff, project)
        else {
            val res =  ArrayList<HashMap<String, HashMap<PsiClass, ArrayList<PsiMethod>>>>()
            val taskNum: Int = (commitDiff.size / threadNum) + 1
            val taskList = ArrayList<ArrayList<DiffEntry>>()
            val executorService = Executors.newFixedThreadPool(threadNum)
            val tasks = ArrayList<Callable<ArrayList<HashMap<String, HashMap<PsiClass, ArrayList<PsiMethod>>>>>>()
            for (i in 0..taskNum) {
                if (i < taskNum - 1)
                    taskList.add(commitDiff.subList(i * taskNum, i * taskNum + threadNum) as ArrayList<DiffEntry>)
                else
                    taskList.add(commitDiff.subList(i * taskNum, commitDiff.size) as ArrayList<DiffEntry>)
            }
            for (i in taskList) {
                val task =
                    Callable< ArrayList<HashMap<String, HashMap<PsiClass, ArrayList<PsiMethod>>>>>(fun(): ArrayList<HashMap<String, HashMap<PsiClass, ArrayList<PsiMethod>>>> {
                        return getDiffBetweenFiles(
                            i,
                            project
                        )
                    })
                tasks.add(task)
            }
            try{
                val resFuture = executorService.invokeAll(tasks)
                for(i in resFuture){
                    res.add(i.get() as HashMap<String, HashMap<PsiClass, ArrayList<PsiMethod>>> )
                }
            }catch (e:Exception){
                throw e
            }finally {
                executorService.shutdown()
            }
            return res
        }
    }
}
