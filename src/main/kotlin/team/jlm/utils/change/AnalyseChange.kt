package team.jlm.utils.change

import com.github.difflib.DiffUtils
import com.github.difflib.patch.Chunk
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vcs.changes.Change
import com.xyzboom.algorithm.graph.Graph
import com.xyzboom.algorithm.graph.saveAsDependencyGraph
import team.jlm.coderefactor.code.PsiGroup
import team.jlm.utils.createOrGetJavaPsiFile
import team.jlm.utils.file.getFileSeparator
import team.jlm.utils.getAllClassesInJavaFile
import java.util.concurrent.CompletableFuture

/**
 * @description 获取文件变化
 * @param changes MutableCollection<Change>
 * @param project Project
 */
fun analyseChanges(
    changes: MutableCollection<Change>, event: AnActionEvent,
    beforeCommitId: String, afterCommitId: String,
): Graph<String> {
    val res = Graph<String>()
    val futures = arrayOfNulls<CompletableFuture<*>>(changes.size)
    for ((index, change) in changes.withIndex()) {
        val task: () -> Unit =
            {
                runReadAction {
                    prepareAnalyseChange(change, event, beforeCommitId, afterCommitId)
//                    println("end of change $change")
                }
            }

//        SlowOperations.allowSlowOperations(
//            ThrowableRunnable {
        futures[index] = CompletableFuture.runAsync(task)

//            }
//        )
    }
    CompletableFuture.allOf(*futures)
    return res
}

private fun prepareAnalyseChange(
    change: Change,
    event: AnActionEvent,
    beforeCommitId: String,
    afterCommitId: String,
) {
    val project = event.project ?: return
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
    val analyseRes = project.let {
        analyseChange(
            beforeContent, afterContent, it, beforeCommitId, afterCommitId, path
        )
    }
    if (analyseRes.adjList.isNotEmpty()) {
//            res += analyseRes
            project.basePath?.let {
                analyseRes.saveAsDependencyGraph(
                    "${project.name}${getFileSeparator()}" +
                            "${beforeCommitId.subSequence(0, 6)}__${afterCommitId.subSequence(0, 6)}",
                    project.basePath!!,event
                )
            }
        }
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

