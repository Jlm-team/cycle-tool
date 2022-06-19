package team.jlm.utils.gittools.entity

import com.intellij.psi.PsiMethod
import team.jlm.annotation.NoArg

data class DiffInfo(
    val classPath: String,
    val className: String,
    val packageName: String,
    val methodInfo: ArrayList<PsiMethod>
)