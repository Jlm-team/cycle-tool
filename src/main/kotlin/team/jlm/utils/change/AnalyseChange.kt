package team.jlm.utils.change

import com.github.difflib.DiffUtils
import com.github.difflib.patch.Chunk
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vcs.changes.Change
import com.xyzboom.algorithm.graph.Graph
import team.jlm.coderefactor.code.PsiGroup
import team.jlm.utils.getAllClassesInJavaFile
import team.jlm.utils.getPsiJavaFile
import team.jlm.utils.modify.JavaDependenceChange

/**
 * @description 获取文件变化
 * @param changes MutableCollection<Change>
 * @param project Project
 */
fun analyseChanges(changes: MutableCollection<Change>, project: Project): Graph<String> {

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
        val path = change.virtualFile!!.path.replace(project.basePath!!, "")
        val analyseRes = analyseChange(beforeContent, afterContent, project, path)
        res += analyseRes
    }
    return res
}

fun analyseChange(
    beforeContent: String, afterContent: String, project: Project, path: String,
): Graph<String> {
    val diffResults = DiffUtils.diff(beforeContent.toList(), afterContent.toList()).deltas
    val result = Graph<String>()
    for (diffResult in diffResults) {
        result +=
            dependListFromChangeInfo(diffResult.source, project, beforeContent)
        result +=
            dependListFromChangeInfo(diffResult.target, project, afterContent)
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
): Graph<String> {
    val result = Graph<String>()
    val psiJavaFile = getPsiJavaFile(project, content)
    val classes = getAllClassesInJavaFile(psiJavaFile, false)
    val classNameAndTextRange: Map<String, TextRange> =
        classes.associate {
            (it.qualifiedName ?: "") to it.textRange
        }
    val changeStr = String(changeChunk.lines.toCharArray())
    val eleTrim = changeStr.trim()
    val eleIndex = changeChunk.position + changeStr.indexOf(eleTrim)
    if (eleTrim.isNotEmpty()) {
        val psiElement = psiJavaFile.findElementAt(eleIndex)
        if (psiElement != null) {
            val psiGroup = PsiGroup(psiElement, TextRange(eleIndex, eleTrim.length + eleIndex), classNameAndTextRange)
            result += psiGroup.dependencyGraph
        }
    }
    return result
}

