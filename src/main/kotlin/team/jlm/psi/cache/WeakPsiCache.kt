package team.jlm.psi.cache

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import java.lang.ref.WeakReference

class WeakPsiCache(
    psiElement: PsiElement?,
) : INullablePsiCache<PsiElement?> {
    private val weakCache = WeakReference(psiElement)
    override fun getPsi(project: Project): PsiElement? {
        return weakCache.get()
    }
}