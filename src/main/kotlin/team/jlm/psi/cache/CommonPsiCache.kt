package team.jlm.psi.cache

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement

class CommonPsiCache(
    psiElement: PsiElement,
) : INullablePsiCache<PsiElement?> {
    private val psiFile = psiElement.containingFile
    private val textRange = psiElement.textRange
    override fun getPsi(project: Project): PsiElement? {
        var ele = psiFile.findElementAt(textRange.startOffset) ?: return null
        while (ele.textRange != textRange && ele != psiFile) {
            ele = ele.parent
        }
        return ele
    }
}