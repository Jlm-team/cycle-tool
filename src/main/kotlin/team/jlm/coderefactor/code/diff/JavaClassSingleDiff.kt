package team.jlm.coderefactor.code.diff

import com.intellij.psi.PsiClass

/**
 * 这个类代表类的变化的细节部分
 */
class JavaClassSingleDiff(
    val classSingleDiffType: JavaClassSingleDiffType,
    val before: String,
    val after: String
) {

}