package team.jlm.utils.change

import com.github.difflib.DiffUtils
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.Change
import team.jlm.coderefactor.code.PsiGroup
import team.jlm.utils.getPsiJavaFile
import team.jlm.utils.modify.JavaDependenceChange

/**
 * @description 获取文件变化
 * @param changes MutableCollection<Change>
 * @param project Project
 */
fun analyseChanges(changes: MutableCollection<Change>, project: Project):ArrayList<JavaDependenceChange> {

    val res = ArrayList<JavaDependenceChange>(64)
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
        val path = change.virtualFile!!.path.replace(project.basePath!!,"")
        val analyseRes = analyseChange(beforeContent, afterContent, project, path)
        if(analyseRes == null)
            continue
        else
            res.add(analyseRes)
    }
    return res
}

fun analyseChange(
    beforeContent: String, afterContent: String, project: Project,path:String
): JavaDependenceChange?{
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
    if(beforeList.isEmpty() and afterList.isEmpty())
        return null
    return JavaDependenceChange(beforeList,afterList,path)
}

/**
 * 从文本差异中分析到的依赖项
 */
private fun dependListFromChangeInfo(
    changeStr: String,
    project: Project,
    content: String,
): HashSet<String> {
    val result = HashSet<String>()
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

