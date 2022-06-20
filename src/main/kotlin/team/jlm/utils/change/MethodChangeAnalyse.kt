package team.jlm.utils.change

import com.github.difflib.DiffUtils
import com.github.difflib.patch.DeltaType
import com.github.difflib.patch.DeltaType.*
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import team.jlm.utils.gittools.entity.JavaClassDiff
import team.jlm.utils.gittools.entity.JavaClassInnerDiff
import team.jlm.utils.gittools.entity.JavaClassInnerDiffType
import team.jlm.utils.gittools.entity.JavaClassInnerDiffType.*

fun analyseMethodsModified(
    project: Project, beforeMethods: List<PsiMethod>, afterMethods: List<PsiMethod>,
): List<JavaClassInnerDiff> {
    val result = ArrayList<JavaClassInnerDiff>(beforeMethods.size)
    val methodDiffs = DiffUtils.diff(beforeMethods, afterMethods).deltas
    for (diff in methodDiffs) {
        when (diff.type) {
            CHANGE -> {
                for (i in 0 until diff.source.size()) {
                    val innerDiff = JavaClassInnerDiff(
                        METHOD_MODIFY,
                        diff.source.lines[i].text,
                        diff.target.lines[i].text,
                    )
                    result.add(innerDiff)
                }
            }
            DELETE -> {
                for (i in 0 until diff.source.size()) {
                    val innerDiff = JavaClassInnerDiff(
                        METHOD_DELETE,
                        diff.source.lines[i].text,
                        "",
                    )
                    result.add(innerDiff)
                }
            }
            INSERT -> {
                for (i in 0 until diff.target.size()) {
                    val innerDiff = JavaClassInnerDiff(
                        METHOD_ADD,
                        "",
                        diff.target.lines[i].text,
                    )
                    result.add(innerDiff)
                }
            }
            else -> continue
        }
    }
    return result
}