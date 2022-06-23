package team.jlm.utils.change

import com.github.difflib.DiffUtils
import com.github.difflib.patch.Chunk
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vcs.changes.Change
import com.intellij.psi.util.PsiTreeUtil
import com.xyzboom.algorithm.graph.Graph
import team.jlm.coderefactor.code.PsiGroup
import team.jlm.utils.createOrGetJavaPsiFile
import team.jlm.utils.getAllClassesInJavaFile
import team.jlm.utils.getPsiJavaFile
import team.jlm.utils.modify.JavaDependenceChange

/**
 * @description 获取文件变化
 * @param changes MutableCollection<Change>
 * @param project Project
 */
fun analyseChanges(
    changes: MutableCollection<Change>, project: Project,
    beforeCommitId: String, afterCommitId: String,
): Graph<String> {
    val res = Graph<String>()
    for (change in changes) {
        if (change.type != Change.Type.MODIFICATION) {
            continue
        }
        val beforeRevision = change.beforeRevision
        val afterRevision = change.afterRevision
        if (beforeRevision == null || afterRevision == null) {
            continue
        }
        val beforeContent = beforeRevision.content
        val afterContent = afterRevision.content
        if (beforeContent == null || afterContent == null) {
            continue
        }
        val path = change.virtualFile?.path ?: "unknown path"
        val analyseRes = analyseChange(
            beforeContent, afterContent, project, beforeCommitId, afterCommitId, path
        )
        if (analyseRes.adjList.isNotEmpty()) {
            res += analyseRes
        }
    }
    return res
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

