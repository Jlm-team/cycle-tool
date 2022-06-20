package team.jlm.utils.change

import com.github.difflib.DiffUtils
import com.intellij.openapi.application.Application
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import team.jlm.utils.gittools.entity.JavaClassDiff

internal fun analyseClassesModified(
    project: Project, beforeClasses: List<PsiClass>, afterClasses: List<PsiClass>,
): List<JavaClassDiff> {
    val result = ArrayList<JavaClassDiff>(beforeClasses.size)
    for (i in beforeClasses.indices) {
        val thisClassDiff = JavaClassDiff()
        val beforeClass = beforeClasses[i]
        val afterClass = afterClasses[i]
        val beforeMethods = beforeClass.methods
        val afterMethods = afterClass.methods
        val methodsDiffResult = analyseMethodsModified(project, beforeMethods.toList(), afterMethods.toList())
        thisClassDiff.classSingleDiff.addAll(methodsDiffResult)
        result.add(thisClassDiff)
    }
    return result
}