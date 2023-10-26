package team.jlm.psi.cache

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import team.jlm.utils.psi.findPsiClass

class PsiClassCache(
    psiClass: PsiClass,
) : INullablePsiCache<PsiClass?> {
    private val className: String?
    private val commonPsiCache: CommonPsiCache?

    init {
        className = psiClass.qualifiedName
        commonPsiCache = if (className == null) {
            CommonPsiCache(psiClass)
        } else {
            null
        }
    }

    override fun getPsi(project: Project): PsiClass? {
        return if (className == null) {
            commonPsiCache?.getPsi(project) as? PsiClass
        } else {
            findPsiClass(project, className)
        }
    }

}