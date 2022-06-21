package team.jlm.utils.change

import com.github.difflib.DiffUtils
import com.github.difflib.patch.DeltaType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.Change
import team.jlm.utils.getAllClassesInJavaFile
import team.jlm.utils.getPsiJavaFile
import team.jlm.utils.modify.JavaClassDiff
import java.lang.Integer.max

fun analyseJavaFile(project: Project, change: Change): List<JavaClassDiff> {
    return when (change.type) {
        Change.Type.NEW -> analyseFileAdded(project, change)
        Change.Type.DELETED -> analyseFileDeleted(project, change)
        Change.Type.MOVED -> analyseFileMoved(project, change)
        Change.Type.MODIFICATION -> analyseFileModified(project, change)
    }
}

private fun analyseFileModified(project: Project, change: Change): List<JavaClassDiff> {
    val beforeRevision = change.beforeRevision
    val afterRevision = change.afterRevision
    if (beforeRevision == null || afterRevision == null) {
        return emptyList()
    }
    val beforeContent = beforeRevision.content
    val afterContent = afterRevision.content
    if (beforeContent == null || afterContent == null) {
        return emptyList()
    }
    val beforeJavaPsi = getPsiJavaFile(project, beforeContent)
    val afterJavaPsi = getPsiJavaFile(project, afterContent)
    val beforeClasses = getAllClassesInJavaFile(beforeJavaPsi)
    val afterClasses = getAllClassesInJavaFile(afterJavaPsi)
    val classDiffResults = DiffUtils.diff(beforeClasses, afterClasses)
    val result = ArrayList<JavaClassDiff>(max(beforeClasses.size, afterClasses.size))
    for (diffResult in classDiffResults.deltas) {
        val classDiff = when (diffResult.type) {
            DeltaType.CHANGE -> analyseClassesModified(project, diffResult.source.lines, diffResult.target.lines)
            else -> emptyList()
        }
        result.addAll(classDiff)
    }
    return result
}

private fun analyseFileDeleted(project: Project, change: Change): List<JavaClassDiff> {
    //TODO
    return emptyList()
}

private fun analyseFileMoved(project: Project, change: Change): List<JavaClassDiff> {
    //TODO
    return emptyList()
}

private fun analyseFileAdded(project: Project, change: Change): List<JavaClassDiff> {
    //TODO
    return emptyList()
}
