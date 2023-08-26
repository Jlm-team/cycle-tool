package team.jlm.psi.cache

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement

/**
 * 低内存存储psi
 */
interface IPsiCache<out T : PsiElement> : INullablePsiCache<T> {
    override fun getPsi(project: Project): T
}