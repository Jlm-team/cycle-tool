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
fun analyseChange(changes: MutableCollection<Change>, project: Project) {

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
        getChange(beforeContent, afterContent, project)

    }
}

/**
 *
 * @param beforeContent String
 * @param afterContent String
 * @param project Project
 * @return Pair<PsiGroup, PsiGroup>
 */
fun getChange(beforeContent: String, afterContent: String, project: Project): Pair<PsiGroup, PsiGroup> {

    val results = DiffUtils.diff(beforeContent.toList(), afterContent.toList()).deltas
    val beforeJavaPsi = getPsiJavaFile(project, beforeContent)
    val afterJavaPsi = getPsiJavaFile(project, afterContent)
    var beforeGroup = PsiGroup()
    var afterGroup = PsiGroup()

    for (result in results) {
        val beforeEle = String(result.source.lines.toCharArray())
        val beforeEleTrim = beforeEle.trim()
        if (beforeEleTrim.isNotEmpty()) {
            var beforePsi = beforeJavaPsi.findElementAt(beforeContent.indexOf(beforeEleTrim))
            if (beforePsi != null) {
                beforeGroup = PsiGroup(beforePsi)
                while (beforePsi != null && !beforeGroup.textMatches(beforeEleTrim)) {
                    if (beforeGroup.textLength > beforeEleTrim.length) {
                        break
                    }
                    beforePsi = beforeGroup.nextLeaf
                    beforePsi?.let { beforeGroup.add(beforePsi) }
                }
            }
        }
        val afterEle = String(result.target.lines.toCharArray())
        val afterEleTrim = afterEle.trim()
        if (afterEleTrim.isNotEmpty()) {
            var afterPsi = afterJavaPsi.findElementAt(afterContent.indexOf(afterEleTrim))
            if (afterPsi != null) {
                afterGroup = PsiGroup(afterPsi)
                while (afterPsi != null && !afterGroup.textMatches(afterEleTrim)) {
                    if (afterGroup.textLength > afterEleTrim.length) {
                        break
                    }
                    afterPsi = afterGroup.nextLeaf
                    afterPsi?.let { afterGroup.add(afterPsi) }
                }

            }
        }

    }
    return Pair(beforeGroup, afterGroup)
}

