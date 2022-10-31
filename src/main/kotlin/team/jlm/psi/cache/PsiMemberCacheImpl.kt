package team.jlm.psi.cache

import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiJvmMember
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil

class PsiMemberCacheImpl(
    private val startIndexInClass: Int,
    private val containingClassName: String,
    private val memberClass: Class<PsiJvmMember>,
) : IPsiCache<PsiJvmMember> {
    override fun getPsi(project: Project): PsiJvmMember {
        val javaPsiFacade = JavaPsiFacade.getInstance(project)
        val psiClass = javaPsiFacade.findClass(containingClassName, GlobalSearchScope.projectScope(project))!!
        return PsiTreeUtil.getParentOfType(
            psiClass.findElementAt(startIndexInClass),
            memberClass
        ) as PsiJvmMember
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PsiMemberCacheImpl) return false

        if (startIndexInClass != other.startIndexInClass) return false
        if (containingClassName != other.containingClassName) return false
        if (memberClass != other.memberClass) return false

        return true
    }

    override fun hashCode(): Int {
        var result = startIndexInClass
        result = 31 * result + containingClassName.hashCode()
        result = 31 * result + memberClass.hashCode()
        return result
    }
}