package team.jlm.psi.cache

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement

/**
 * 低内存存储psi
 */
interface IPsiCache<out T : PsiElement> {
    fun getPsi(project: Project): T

    companion object {
        val EMPTY = object : IPsiCache<PsiElement> {
            override fun getPsi(project: Project): PsiElement {
                throw NullPointerException("Can not get psi from EMPTY!")
            }
        }
    }
}