package team.jlm.psi.cache

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement

interface INullablePsiCache<out T : PsiElement?> {
    fun getPsi(project: Project): T

    companion object {
        val EMPTY = object : INullablePsiCache<PsiElement?> {
            override fun getPsi(project: Project): PsiElement? {
                return null
            }
        }
    }
}