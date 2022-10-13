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
import team.jlm.coderefactor.code.PsiGroup
import team.jlm.utils.createOrGetJavaPsiFile
import team.jlm.utils.getAllClassesInJavaFile
import team.jlm.utils.psi.PsiCompareHelper
import team.jlm.utils.psi.createPsiHelpersFromFile
import java.util.concurrent.CompletableFuture

/**
 * @description 获取文件变化
 * @param changes MutableCollection<Change>
 * @param project Project
 */
fun analyseChangesCompletableFuture(
    changes: MutableCollection<Change>, project: Project,
    beforeCommitId: String, afterCommitId: String,
): Graph<String> {
    //d69692-febce6差异分析16线程耗时： 216秒, 227秒
    //d69692-febce6差异分析与change集合数相同线程耗时： 215秒, 234秒
    //从内存占用来看，后者开的线程数量远大于前者，因此固定线程数量方案优先
    val res = Graph<String>()
    val threadNum = 16
    if (changes.size < threadNum) {
        analyseChanges(changes, project, beforeCommitId, afterCommitId, res)
        return res
    }
    val futures = arrayOfNulls<CompletableFuture<*>>(threadNum)
//    val futures = arrayOfNulls<CompletableFuture<*>>(changes.size)
    val changeList = ArrayList(changes)
    val size = changes.size
    val chunkSize = size / threadNum
    var nowFinished = 0f
    for (i in 0 until threadNum) {
//    for (i in changes.indices) {
        val task: () -> Unit =
            {
                runReadAction {
                    for (index in 0 until chunkSize) {
//                        val change = changeList[i]
                        val change = changeList[index + i * chunkSize]
                        prepareAnalyseChange(change, project, beforeCommitId, afterCommitId, res)
                        nowFinished++
                        println(nowFinished / size)
                    }
                }
            }

//        SlowOperations.allowSlowOperations(
//            ThrowableRunnable {
        futures[i] = CompletableFuture.runAsync(task)
//            }
//        )
    }
    CompletableFuture.allOf(*futures).get()
    return res
}

fun analyseChanges(
    changes: MutableCollection<Change>, project: Project,
    beforeCommitId: String, afterCommitId: String,
    outGraph: Graph<String>,
) {
    val size = changes.size
    for ((index, change) in changes.withIndex()) {
        prepareAnalyseChange(change, project, beforeCommitId, afterCommitId, outGraph)
        println(index * 1f / size)
//        if (index * 1f / size > 0.1) {
//            break
//        }
    }
}

private fun prepareAnalyseChange(
    change: Change,
    project: Project,
    beforeCommitId: String,
    afterCommitId: String,
    outGraph: Graph<String>,
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
    analyseChangeFromPsiDiff(psiDiffResults, beforePsiFile, afterPsiFile, outGraph)
}

private fun psiListDiff(
    beforePsiList: List<PsiCompareHelper>,
    afterPsiList: List<PsiCompareHelper>,
) = DiffUtils.diff(beforePsiList, afterPsiList).deltas

fun analyseChangeFromPsiDiff(
    diffResults: List<AbstractDelta<PsiCompareHelper>>,
    beforePsiJavaFile: PsiJavaFile,
    afterPsiJavaFile: PsiJavaFile,
    outGraph: Graph<String>,
) {
    for (diffResult in diffResults) {
        if (diffResult.source != null) {
            dependListFromPsiChangeInfo(diffResult.source, beforePsiJavaFile, outGraph)
        }
        if (diffResult.target != null) {
            dependListFromPsiChangeInfo(diffResult.target, afterPsiJavaFile, outGraph)
        }
    }
}


/**
 * 从语法树差异中分析的依赖项
 */
private fun dependListFromPsiChangeInfo(
    changeChunk: Chunk<PsiCompareHelper>,
    psiJavaFile: PsiJavaFile,
    outGraph: Graph<String>,
) {
    val changePsiList = changeChunk.lines
    if (changePsiList.size <= 0) {
        return
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
    psiGroup.getDependencyGraph(outGraph)
}

