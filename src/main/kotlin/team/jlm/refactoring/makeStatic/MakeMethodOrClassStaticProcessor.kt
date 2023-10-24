package team.jlm.refactoring.makeStatic

import com.intellij.java.refactoring.JavaRefactoringBundle
import com.intellij.lang.findUsages.DescriptiveNameUtil
import com.intellij.model.PsiElementUsageInfo
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.search.searches.OverridingMethodsSearch
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.listeners.RefactoringEventData
import com.intellij.refactoring.makeStatic.*
import com.intellij.refactoring.util.CommonRefactoringUtil
import com.intellij.refactoring.util.RefactoringUIUtil
import com.intellij.refactoring.util.RefactoringUtil
import com.intellij.usageView.UsageInfo
import com.intellij.usageView.UsageViewDescriptor
import com.intellij.util.IncorrectOperationException
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.containers.MultiMap
import mu.KotlinLogging
import team.jlm.refactoring.BaseRefactoringProcessor

private val logger = KotlinLogging.logger {}

@Suppress("UnstableApiUsage")
abstract class MakeMethodOrClassStaticProcessor<T : PsiTypeParameterListOwner>(
    project: Project,
    protected var member: T,
) : BaseRefactoringProcessor(project) {

    override fun createUsageViewDescriptor(): UsageViewDescriptor {
        return MakeMethodOrClassStaticViewDescriptor(member)
    }

    override fun getRefactoringId(): String? {
        return "refactoring.makeStatic"
    }

    override fun getBeforeData(): RefactoringEventData? {
        val data = RefactoringEventData()
        data.addElement(member)
        return data
    }

    override fun getAfterData(usages: Array<UsageInfo>): RefactoringEventData? {
        val data = RefactoringEventData()
        data.addElement(member)
        return data
    }

    protected fun findUsages(member: T, containingClass: PsiClass): Array<UsageInfo> {
        val result = ArrayList<UsageInfo>()
        ContainerUtil.addAll(
            result, *(findClassRefsInMember(member, true, containingClass) as Array<out UsageInfo>)
        )
        findExternalUsages(member, result)
        if (member is PsiMethod) {
            val overridingMethods =
                OverridingMethodsSearch.search((member as PsiMethod), member.useScope, false).toArray(
                    PsiMethod.EMPTY_ARRAY
                )
            for (overridingMethod in overridingMethods) {
                if (overridingMethod !== member) {
                    result.add(OverridingMethodUsageInfo(overridingMethod))
                }
            }
        }
        return result.toArray(UsageInfo.EMPTY_ARRAY)
    }

    override fun findUsages(): Array<UsageInfo> {
        return findUsages(member, member.containingClass!!)
    }

    protected abstract fun findExternalUsages(member: T, result: ArrayList<UsageInfo>)

    protected fun findExternalReferences(method: PsiMethod, result: ArrayList<UsageInfo>) {
        for (ref in ReferencesSearch.search(method)) {
            val element = ref.element
            var qualifier: PsiElement? = null
            if (element is PsiReferenceExpression) {
                qualifier = element.qualifierExpression
                if (qualifier is PsiThisExpression) qualifier = null
            }
            if (!PsiTreeUtil.isAncestor(member, element, true) || qualifier != null) {
                result.add(PsiElementUsageInfo(element))
            }
            processExternalReference(element, method, result)
        }
    }

    protected open fun processExternalReference(
        element: PsiElement,
        method: PsiMethod?,
        result: ArrayList<out UsageInfo>,
    ) {
    }

    protected fun setupTypeParameterList(member: T) {
        val list = member.typeParameterList!!
        val newList = RefactoringUtil.createTypeParameterListWithUsedTypeParameters(member)
        if (newList != null) {
            list.replace(newList)
        }
    }

    override fun getCommandName(): String {
        return JavaRefactoringBundle.message("make.static.command", DescriptiveNameUtil.getDescriptiveName(member))
    }

    override fun performRefactoring(usages: Array<out UsageInfo>) {
        @Suppress("UNCHECKED_CAST")
        val copyMember = member.copy() as T
        val manager = copyMember.manager
        val factory = JavaPsiFacade.getElementFactory(manager.project)
        try {
            for (usage in findUsages(copyMember, member.containingClass!!)) {
                when (usage) {
                    is SelfUsageInfo -> changeSelfUsage(copyMember, usage)
                    is InternalUsageInfo -> changeInternalUsage(copyMember, usage, factory)
                    else -> changeExternalUsage(copyMember, usage, factory)
                }
            }
            changeSelf(copyMember, member.containingClass!!, factory, usages)
            refactoringTransaction.getElementListener(member).elementMoved(member.replace(copyMember))
        } catch (ex: IncorrectOperationException) {
            logger.error(ex) {}
        }
    }

    protected open fun makeClassParameterFinal(usages: Array<out UsageInfo>): Boolean {
        for (usage in usages) {
            if (usage is InternalUsageInfo) {
                val referencedElement = usage.referencedElement
                if (referencedElement !is PsiField) {
                    if (usage.isInsideAnonymous) {
                        return true
                    }
                }
            }
        }
        return false
    }

    protected abstract fun changeSelf(
        member: T,
        containingClass: PsiClass,
        factory: PsiElementFactory,
        usages: Array<out UsageInfo>,
    )

    protected abstract fun changeSelfUsage(member: T, usageInfo: SelfUsageInfo)

    protected abstract fun changeInternalUsage(member: T, usage: InternalUsageInfo, factory: PsiElementFactory)

    protected abstract fun changeExternalUsage(member: T, usage: UsageInfo, factory: PsiElementFactory)

    companion object {
        private fun filterOverriding(
            usages: Array<UsageInfo>,
            suggestToMakeStatic: MutableSet<in UsageInfo>,
        ): Array<UsageInfo> {
            val result = ArrayList<UsageInfo>()
            for (usage in usages) {
                if (usage is ChainedCallUsageInfo) {
                    suggestToMakeStatic.add(usage)
                } else if (usage !is OverridingMethodUsageInfo) {
                    result.add(usage)
                }
            }
            return result.toArray(UsageInfo.EMPTY_ARRAY)
        }

        private fun filterInternalUsages(usages: Array<UsageInfo>): Array<UsageInfo> {
            val result = ArrayList<UsageInfo>()
            for (usage in usages) {
                if (usage !is InternalUsageInfo) {
                    result.add(usage)
                }
            }
            return result.toArray(UsageInfo.EMPTY_ARRAY)
        }

        private fun createInaccessibleFieldsConflictDescription(
            inaccessible: ArrayList<PsiField>, container: PsiElement,
            conflicts: MultiMap<PsiElement, String>,
        ) {
            for (field in inaccessible) {
                conflicts.putValue(
                    field, JavaRefactoringBundle.message(
                        "field.0.is.not.accessible", CommonRefactoringUtil.htmlEmphasize(field.name),
                        RefactoringUIUtil.getDescription(container, true)
                    )
                )
            }
        }

        @JvmStatic
        protected fun makeFieldParameterFinal(field: PsiField, usages: Array<UsageInfo>): Boolean {
            for (usage in usages) {
                if (usage is InternalUsageInfo) {
                    val referencedElement = usage.referencedElement
                    if (referencedElement is PsiField && field == referencedElement) {
                        if (usage.isInsideAnonymous) {
                            return true
                        }
                    }
                }
            }
            return false
        }
    }
}
