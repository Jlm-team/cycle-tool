package team.jlm.dependency

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMember
import team.jlm.psi.cache.INullablePsiCache
import team.jlm.utils.graph.Graph
import team.jlm.utils.psi.findPsiClass

@Suppress("DuplicatedCode")
fun analyseMemberGranularityDependency(
    project: Project, classA: String, classB: String,
): Graph<PsiMember>? {
    val psiClass0 = findPsiClass(project, classA)!!
    val psiClass1 = findPsiClass(project, classB)!!
    val result = Graph<PsiMember>()
    var noDependencyFound = true
    val filter: (PsiClass) -> Boolean = { it === psiClass1 || it === psiClass0 }
    val visitor: (PsiClass, PsiClass, DependencyInfo) -> Unit = v@{
            userClass: PsiClass, providerClass: PsiClass, info: DependencyInfo,
        ->
        if (info.userType.isMember || info.providerType.isMember) {
            noDependencyFound = false
            val userMember = if (info.userCache === INullablePsiCache.EMPTY) {
                NoMethodGranularityPsiMember(userClass)
            } else {
                val psiEle = info.userCache.getPsi(project)
                psiEle as? PsiMember ?: NoMethodGranularityPsiMember(providerClass)
            }

            val providerMember = if (info.providerCache === INullablePsiCache.EMPTY) {
                NoMethodGranularityPsiMember(providerClass)
            } else {
                val psiEle = info.providerCache.getPsi(project)
                psiEle as? PsiMember ?: NoMethodGranularityPsiMember(providerClass)
            }
            result.addEdge(userMember, providerMember)
        }
    }
    DependenciesBuilder.analyzePsiDependencies(psiClass0, filter, visitor)
    DependenciesBuilder.analyzePsiDependencies(psiClass1, filter, visitor)
    return if (noDependencyFound) null else result
}