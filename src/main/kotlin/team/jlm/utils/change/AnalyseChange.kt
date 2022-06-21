package team.jlm.utils.change

import com.github.difflib.DiffUtils
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.Change
import team.jlm.coderefactor.code.PsiGroup
import team.jlm.utils.getPsiJavaFile

/**
 * @description 获取文件变化
 * @param changes MutableCollection<Change>
 * @param project Project
 */
fun analyseChanges(changes: MutableCollection<Change>, project: Project) {

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
        analyseChange(beforeContent, afterContent, project)
    }
}

fun analyseChange(
    beforeContent: String, afterContent: String, project: Project,
): Pair<ArrayList<String>, ArrayList<String>> {
    val diffResults = DiffUtils.diff(beforeContent.toList(), afterContent.toList()).deltas
    val beforeList = ArrayList<String>()
    val afterList = ArrayList<String>()
    for (diffResult in diffResults) {
        beforeList.addAll(
            dependListFromChangeInfo(String(diffResult.source.lines.toCharArray()), project, beforeContent)
        )
        afterList.addAll(
            dependListFromChangeInfo(String(diffResult.target.lines.toCharArray()), project, beforeContent)
        )
    }
    return Pair(beforeList, afterList)
}

/**
 * 从文本差异中分析到的依赖项
 */
private fun dependListFromChangeInfo(
    changeStr: String,
    project: Project,
    content: String,
): ArrayList<String> {
    val result = ArrayList<String>()
    val psiJavaFile = getPsiJavaFile(project, content)
    val eleTrim = changeStr.trim()
    if (eleTrim.isNotEmpty()) {
        var psiElement = psiJavaFile.findElementAt(content.indexOf(eleTrim))
        if (psiElement != null) {
            val psiGroup = PsiGroup(psiElement)
            while (psiElement != null && !psiGroup.textMatches(eleTrim)) {
                if (psiGroup.textLength > eleTrim.length) {
                    break
                }
                psiElement = psiGroup.nextLeaf
                psiElement?.let { psiGroup.add(psiElement) }
                result.addAll(psiGroup.dependencyList)
            }
        }
    }
    return result
}

