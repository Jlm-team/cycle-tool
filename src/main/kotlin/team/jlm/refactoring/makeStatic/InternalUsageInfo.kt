package team.jlm.refactoring.makeStatic

import com.intellij.model.BranchableUsageInfo
import com.intellij.model.ModelBranch
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.util.PsiUtil
import com.intellij.refactoring.util.RefactoringUtil
import com.intellij.usageView.UsageInfo
import java.util.*

@Suppress("UnstableApiUsage")
open class InternalUsageInfo(
    element: PsiElement, val referencedElement: PsiElement,
) :
    UsageInfo(element), BranchableUsageInfo {
    private var myIsInsideAnonymous: Boolean? = null

    init {
        isInsideAnonymous
    }

    val isInsideAnonymous: Boolean
        get() {
            if (myIsInsideAnonymous == null) {
                myIsInsideAnonymous = java.lang.Boolean.valueOf(RefactoringUtil.isInsideAnonymousOrLocal(element, null))
            }
            return myIsInsideAnonymous!!
        }
    val isWriting: Boolean
        get() = (referencedElement is PsiField &&
                element is PsiReferenceExpression && PsiUtil.isAccessedForWriting(((element as PsiReferenceExpression?)!!)))

    override fun obtainBranchCopy(branch: ModelBranch): UsageInfo {
        return InternalUsageInfo(
            branch.obtainPsiCopy<PsiElement>(element!!),
            branch.obtainPsiCopy(referencedElement)
        )
    }
}