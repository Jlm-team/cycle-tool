package team.jlm.utils.change

import com.github.difflib.DiffUtils
import com.github.difflib.patch.AbstractDelta
import com.github.difflib.patch.Chunk
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vcs.changes.Change
import com.intellij.psi.PsiJavaFile
import com.intellij.refactoring.suggested.endOffset
import com.intellij.refactoring.suggested.startOffset
import com.xyzboom.algorithm.graph.Graph
import com.xyzboom.algorithm.graph.saveAsDependencyGraph
import kotlinx.coroutines.runBlocking
import team.jlm.coderefactor.code.PsiGroup
import team.jlm.utils.createOrGetJavaPsiFile
import team.jlm.utils.file.getFileSeparator
import team.jlm.utils.getAllClassesInJavaFile
import team.jlm.utils.psi.PsiCompareHelper
import team.jlm.utils.psi.createPsiHelpersFromFile
import java.util.concurrent.CompletableFuture
import kotlin.concurrent.thread

/**
 * @description 获取文件变化
 * @param changes MutableCollection<Change>
 * @param project Project
 */
fun analyseChangesCompletableFuture(
    changes: MutableCollection<Change>, project: Project,
    beforeCommitId: String, afterCommitId: String,
): Graph<String> {
    val res = Graph<String>()
    val threadNum = 16
    if (changes.size < threadNum)
        return analyseChanges(changes, project, beforeCommitId, afterCommitId)
    val futures = arrayOfNulls<CompletableFuture<*>>(threadNum)
    val changeList = ArrayList(changes)
    val size = changes.size
    val chunkSize = size / threadNum
    var nowFinished = 0f
    for (i in 0 until threadNum) {
        val task: () -> Unit =
            {
                runReadAction {
                    for (index in 0 until chunkSize) {
                        val change = changeList[index + i * chunkSize];
                        prepareAnalyseChange(change, project, beforeCommitId, afterCommitId)
                        nowFinished++
                    }
                }
            }

//        SlowOperations.allowSlowOperations(
//            ThrowableRunnable {
        futures[i] = CompletableFuture.runAsync(task)

//            }
//        )
    }
    thread {
        while (true) {
            println(nowFinished / size)
            Thread.sleep(3500)
            if (nowFinished / size > 0.9) {
                break
            }
        }
    }
    CompletableFuture.allOf(*futures).get()
    return res
}

fun analyseChanges(
    changes: MutableCollection<Change>, project: Project,
    beforeCommitId: String, afterCommitId: String,
): Graph<String> {
    val res = Graph<String>()
    val size = changes.size
    for ((index, change) in changes.withIndex()) {
        prepareAnalyseChange(change, project, beforeCommitId, afterCommitId)
        println(index * 1f / size)
//        if (index * 1f / size > 0.1) {
//            break
//        }
    }
    return res
}

/**
 * @description 获取文件变化
 * @param changes MutableCollection<Change>
 * @param project Project
 */
fun analyseChangesCoroutine(
    changes: MutableCollection<Change>, project: Project,
    beforeCommitId: String, afterCommitId: String,
): Graph<String> {
    val res = Graph<String>()
    val size = changes.size
    var nowFinished = 0f
    thread {
        while (true) {
            println(nowFinished / size)
            Thread.sleep(3500)
            if (nowFinished >= size) {
                break
            }
        }
    }
    runBlocking {
        for ((index, change) in changes.withIndex()) {
            val task: suspend () -> Unit =
                {
                    prepareAnalyseChange(change, project, beforeCommitId, afterCommitId)
                    nowFinished++
                }

            task()
        }
    }

    return res
}

private fun prepareAnalyseChange(
    change: Change,
    project: Project,
    beforeCommitId: String,
    afterCommitId: String,
) {
    if (change.type != Change.Type.MODIFICATION) {
        return
    }
    val beforeRevision = change.beforeRevision
    val afterRevision = change.afterRevision
    if (beforeRevision == null || afterRevision == null) {
        return
    }
    val beforeContent = beforeRevision.content
    val afterContent = afterRevision.content
    if (beforeContent == null || afterContent == null) {
        return
    }
    val path = change.virtualFile?.path ?: "unknown path"
    val beforePsiFile = createOrGetJavaPsiFile(project, beforeContent, beforeCommitId, path)
    val beforePsiList = createPsiHelpersFromFile(beforePsiFile)
    val afterPsiFile = createOrGetJavaPsiFile(project, afterContent, afterCommitId, path)
    val afterPsiList = createPsiHelpersFromFile(afterPsiFile)
//    val strDiffResults = strDiff(beforeContent, afterContent)
    val psiDiffResults = psiListDiff(beforePsiList, afterPsiList)
//    val analyseRes = analyseChange(
//        beforeContent, afterContent, project, beforeCommitId, afterCommitId, path
//    )
    val analyseRes = analyseChangeFromPsiDiff(psiDiffResults, beforePsiFile, afterPsiFile)
    if (analyseRes.adjList.isNotEmpty()) {
//        print(analyseRes)
//            res += analyseRes
        project.basePath?.let {
            analyseRes.saveAsDependencyGraph(
                "${project.name}${getFileSeparator()}" +
                        "${beforeCommitId.subSequence(0, 6)}__${afterCommitId.subSequence(0, 6)}", it
            )
        }
    }
}

private fun psiListDiff(
    beforePsiList: List<PsiCompareHelper>,
    afterPsiList: List<PsiCompareHelper>,
) = DiffUtils.diff(beforePsiList, afterPsiList).deltas

private fun strDiff(
    beforeContent: String,
    afterContent: String,
) = DiffUtils.diff(beforeContent.toList(), afterContent.toList()).deltas

fun analyseChangeFromPsiDiff(
    diffResults: List<AbstractDelta<PsiCompareHelper>>,
    beforePsiJavaFile: PsiJavaFile,
    afterPsiJavaFile: PsiJavaFile,
): Graph<String> {
    val result = Graph<String>()
    for (diffResult in diffResults) {
        if (diffResult.source != null) {
            val beforeDependGraph =
                dependListFromPsiChangeInfo(diffResult.source, beforePsiJavaFile)
            if (beforeDependGraph != null && beforeDependGraph.adjList.isNotEmpty()) {
                result += beforeDependGraph
            }
        }
        if (diffResult.target != null) {
            val afterDependGraph =
                dependListFromPsiChangeInfo(diffResult.target, afterPsiJavaFile)
            if (afterDependGraph != null && afterDependGraph.adjList.isNotEmpty()) {
                result += afterDependGraph
            }
        }
    }
    return result
}


fun analyseChange(
    beforeContent: String, afterContent: String,
    project: Project, beforeCommitId: String, afterCommitId: String,
    path: String,
): Graph<String> {
    val diffResults = DiffUtils.diff(beforeContent.toList(), afterContent.toList()).deltas
    val result = Graph<String>()
    for (diffResult in diffResults) {
        val beforeDependGraph =
            dependListFromChangeInfo(diffResult.source, project, beforeContent, beforeCommitId, path)
        if (beforeDependGraph != null && beforeDependGraph.adjList.isNotEmpty()) {
            result += beforeDependGraph
        }
        val afterDependGraph =
            dependListFromChangeInfo(diffResult.target, project, afterContent, afterCommitId, path)
        if (afterDependGraph != null && afterDependGraph.adjList.isNotEmpty()) {
            result += afterDependGraph
        }
    }
    return result
}

private fun dependListFromPsiChangeInfo(
    changeChunk: Chunk<PsiCompareHelper>,
    psiJavaFile: PsiJavaFile,
): Graph<String>? {
    val changePsiList = changeChunk.lines
    if (changePsiList.size <= 0) {
        return null
    }
    val changeStartIndex = changePsiList[0].element.startOffset
    val changeEndIndex = changePsiList[changePsiList.size - 1].element.endOffset
    val classes = getAllClassesInJavaFile(psiJavaFile, false)
    val classNameAndTextRange: Map<String, TextRange> =
        classes.associate {
            (it.qualifiedName ?: "") to it.textRange
        }
    val psiGroup =
        PsiGroup(changePsiList[0].element, TextRange(changeStartIndex, changeEndIndex), classNameAndTextRange)
    return psiGroup.dependencyGraph
}

/**
 * 从文本差异中分析到的依赖项
 */
private fun dependListFromChangeInfo(
    changeChunk: Chunk<Char>,
    project: Project,
    content: String,
    commitId: String,
    path: String,
): Graph<String>? {
    val changeStr = String(changeChunk.lines.toCharArray())
    val eleTrim = changeStr.trim()
    if (eleTrim.isEmpty()) return null
    val psiJavaFile = createOrGetJavaPsiFile(project, content, commitId, path)
    val classes = getAllClassesInJavaFile(psiJavaFile, false)
    val classNameAndTextRange: Map<String, TextRange> =
        classes.associate {
            (it.qualifiedName ?: "") to it.textRange
        }
    val eleIndex = changeChunk.position + changeStr.indexOf(eleTrim)
    val psiElement = psiJavaFile.findElementAt(eleIndex)
    if (psiElement != null) {
        val psiGroup = PsiGroup(psiElement, TextRange(eleIndex, eleTrim.length + eleIndex), classNameAndTextRange)
        return psiGroup.dependencyGraph
    }
    return null
}

