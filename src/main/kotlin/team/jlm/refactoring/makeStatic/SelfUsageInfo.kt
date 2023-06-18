package team.jlm.refactoring.makeStatic

import com.intellij.model.ModelBranch
import com.intellij.psi.PsiElement
import com.intellij.usageView.UsageInfo

class SelfUsageInfo(element: PsiElement, referencedElement: PsiElement) :
    InternalUsageInfo(element, referencedElement) {
    override fun obtainBranchCopy(branch: ModelBranch): UsageInfo {
        return SelfUsageInfo(
            branch.obtainPsiCopy(element!!),
            branch.obtainPsiCopy(referencedElement)
        )
    }
}