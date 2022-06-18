package team.jlm.coderefactor.util.gittools.entity

import com.intellij.psi.PsiMethod
import team.jlm.annotation.NoArg

@NoArg
data class DiffInfo(
    var classPath: String,
    var className: String,
    var packageName: String,
    var methodInfo: ArrayList<PsiMethod>
)