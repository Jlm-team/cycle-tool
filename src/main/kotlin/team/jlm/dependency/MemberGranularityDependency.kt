package team.jlm.dependency

import com.intellij.lang.jvm.JvmModifier
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMember
import team.jlm.psi.cache.IPsiCache
import team.jlm.utils.graph.Graph
import team.jlm.utils.psi.findPsiClass

fun analyseMemberGranularityDependency(
    project: Project, classA: String, classB: String,
): Graph<PsiMember>? {
    val psiClass0 = findPsiClass(project, classA)!!
    val psiClass1 = findPsiClass(project, classB)!!
    val result = Graph<PsiMember>()
    var flag = false
    val filter: (PsiClass) -> Boolean = { it === psiClass1 || it === psiClass0 }
    val visitor = v@{
            userClass: PsiClass, providerClass: PsiClass, providerType: DependencyProviderType,
            userType: DependencyUserType, providerCache: IPsiCache<*>, userCache: IPsiCache<*>,
        ->
        if (flag) return@v
        if ((providerType.isMethod || providerType == DependencyProviderType.STATIC_FIELD)
            || (userType.isMethod || userType == DependencyUserType.FIELD_STATIC)
        ) {
            @Suppress("UnstableApiUsage", "DuplicatedCode")
            val userMember = if (userCache === IPsiCache.EMPTY) {
                ClassGranularityPsiMember(userClass)
            } else {
                val member = userCache.getPsi(project) as PsiMember
                if (!member.hasModifier(JvmModifier.STATIC) && member is PsiField) {
                    ClassGranularityPsiMember(userClass)
                } else member
            }

            @Suppress("UnstableApiUsage")
            val providerMember = if (providerCache === IPsiCache.EMPTY) {
                ClassGranularityPsiMember(providerClass)
            } else {
                val member = providerCache.getPsi(project) as PsiMember
                if (!member.hasModifier(JvmModifier.STATIC) && member is PsiField) {
                    ClassGranularityPsiMember(providerClass)
                } else member
            }
            result.addEdge(userMember, providerMember)
        } else {
            flag = true
        }
    }
    DependenciesBuilder.analyzePsiDependencies(psiClass0, filter, visitor)
    DependenciesBuilder.analyzePsiDependencies(psiClass1, filter, visitor)
    return if (flag) null else result
}