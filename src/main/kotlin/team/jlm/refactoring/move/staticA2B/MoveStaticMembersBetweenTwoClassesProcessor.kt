package team.jlm.refactoring.move.staticA2B

import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.move.moveMembers.MoveMemberHandler
import com.intellij.refactoring.move.moveMembers.MoveMembersOptions
import com.intellij.refactoring.move.moveMembers.MoveMembersProcessor.MoveMembersUsageInfo
import com.intellij.usageView.UsageInfo
import com.intellij.usageView.UsageViewBundle
import com.intellij.usageView.UsageViewDescriptor
import com.intellij.usageView.UsageViewUtil
import com.intellij.util.VisibilityUtil
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.containers.toArray
import mu.KotlinLogging
import team.jlm.dependency.DependenciesBuilder
import team.jlm.psi.cache.INullablePsiCache
import team.jlm.refactoring.BaseRefactoringProcessor
import team.jlm.utils.psi.findPsiClass

private val logger = KotlinLogging.logger {}

/**
 * Move static members between two classes processor
 *
 * @property project 项目对象
 * @property members0 类A中的成员列表
 * @property targetClassName0 类A的类全名
 * @property members1 类B中的成员列表
 * @property targetClassName1 类B的类全名
 * @constructor
 *
 * @param refactoringScope
 * @param prepareSuccessfulCallback
 */
class MoveStaticMembersBetweenTwoClassesProcessor @JvmOverloads constructor(
    private val project: Project,
    refactoringScope: SearchScope = GlobalSearchScope.projectScope(project),
    prepareSuccessfulCallback: Runnable? = null,
    private val members0: Array<PsiJvmMember>,
    private val targetClassName0: String,
    private val members1: Array<PsiJvmMember>,
    private val targetClassName1: String,
) : BaseRefactoringProcessor(project, refactoringScope, prepareSuccessfulCallback) {

    override fun refreshElements(elements: Array<out PsiElement>) {
        for (i in elements.indices) {
            if (i < members0.size) {
                members0[i] = elements[i] as PsiJvmMember
            } else {
                members1[i - members0.size] = elements[i] as PsiJvmMember
            }
        }
    }

    override fun createUsageViewDescriptor(): UsageViewDescriptor {
        return object : UsageViewDescriptor {
            override fun getElements() =
                Array<PsiElement>(members0.size + members1.size) {
                    if (it < members0.size) members0[it]
                    else members1[it - members0.size]
                }

            override fun getProcessedElementsHeader(): String {
                return RefactoringBundle.message("move.members.elements.header")
            }

            override fun getCodeReferencesText(usagesCount: Int, filesCount: Int): String {
                return RefactoringBundle.message(
                    "references.to.be.changed",
                    UsageViewBundle.getReferencesString(usagesCount, filesCount)
                )
            }
        }
    }

    override fun findUsages(): Array<UsageInfo> {
        val usagesList = ArrayList<UsageInfo>()
        val membersArray = arrayOf(members0 to targetClassName0, members1 to targetClassName1)
        for ((members, targetClassName) in membersArray) {
            for (member in members) {
                for (psiReference in ReferencesSearch.search(member)) {
                    val ref = psiReference.element
                    val handler = MoveMemberHandler.EP_NAME.forLanguage(ref.language)
                    var usage: MoveMembersUsageInfo? = null
                    val targetClass = findPsiClass(project, targetClassName)
                    if (handler != null && targetClass != null) {
                        usage = handler.getUsage(
                            member, psiReference,
                            hashSetOf(*members) as Set<PsiMember>,
                            targetClass
                        )
                    }
                    if (usage != null) {
                        usagesList.add(usage)
                    } else {
                        if (!isInMovedElement(ref, members)) {
                            usagesList.add(MoveMembersUsageInfo(member, ref, null, ref, psiReference))
                        }
                    }
                }
            }
        }
        var usageInfos = usagesList.toArray(UsageInfo.EMPTY_ARRAY)
        usageInfos = UsageViewUtil.removeDuplicatedUsages(usageInfos)
        return usageInfos
    }

    private fun isInMovedElement(element: PsiElement, members: Array<PsiJvmMember>): Boolean {
        for (member in members) {
            if (PsiTreeUtil.isAncestor(member, element, false)) return true
        }
        return false
    }

    override fun performRefactoring(usages: Array<out UsageInfo>) {
        val membersArray = arrayOf(members0 to targetClassName0, members1 to targetClassName1)
        for ((members, className) in membersArray) {
            val targetClass = findPsiClass(project, className) ?: continue
            val movedMembers = performMove(
                targetClass,
                hashSetOf(*members),
                ContainerUtil.map(usages, MoveMembersUsageInfo::class.java::cast),
                DefaultMoveA2BMemberOptions(members, className)
            )
            afterAllMovements(movedMembers)
        }
    }

    private fun performMove(
        targetClass: PsiClass,
        membersToMove: Set<PsiMember>,
        usages: List<MoveMembersUsageInfo>,
        options: MoveMembersOptions,
    ): Map<PsiMember, SmartPsiElementPointer<PsiMember>> {
        beforeAllMovements(membersToMove)
        // collect anchors to place moved members at
        val anchors: MutableMap<PsiMember, SmartPsiElementPointer<PsiElement>?> = HashMap()
        val anchorsInSourceClass: MutableMap<PsiMember, PsiMember> = HashMap()
        for (member in membersToMove) {
            val handler = MoveMemberHandler.EP_NAME.forLanguage(member.language)
            if (handler != null) {
                val anchor = handler.getAnchor(member, targetClass, membersToMove)
                if (anchor is PsiMember && membersToMove.contains(anchor)) {
                    anchorsInSourceClass[member] = anchor
                } else {
                    anchors[member] =
                        if (anchor == null) null else SmartPointerManager.getInstance(myProject)
                            .createSmartPsiElementPointer(anchor)
                }
            }
        }

        // correct references to moved members from the outside
        val otherUsages = ArrayList<MoveMembersUsageInfo>()
        for (usage in usages) {
            if (!usage.reference.isValid) continue
            usage.element ?: continue
            val handler = MoveMemberHandler.EP_NAME.forLanguage(usage.element!!.language)
            if (handler.changeExternalUsage(options, usage)) continue
            otherUsages.add(usage)
        }

        // correct references inside moved members and outer references to Inner Classes
        val movedMembers: MutableMap<PsiMember, SmartPsiElementPointer<PsiMember>> = HashMap()
        for (member in membersToMove) {
            val refsToBeRebind = ArrayList<PsiReference>()
            val iterator = otherUsages.iterator()
            while (iterator.hasNext()) {
                val info = iterator.next()
                if (member == info.member) {
                    val ref = info.getReference()
                    if (ref != null) {
                        refsToBeRebind.add(ref)
                    }
                    iterator.remove()
                }
            }
            transaction.getElementListener(member) // initialize the listener while PSI is valid
            val handler = MoveMemberHandler.EP_NAME.forLanguage(member.language)
            if (handler != null) {
                val anchor: SmartPsiElementPointer<out PsiElement>? = if (anchorsInSourceClass.containsKey(member)) {
                    val memberInSourceClass = anchorsInSourceClass[member]
                    //anchor should be already moved as myMembersToMove contains members in order they appear in source class
                    if (memberInSourceClass != null) movedMembers[memberInSourceClass] else null
                } else {
                    anchors[member]
                }
                val newMember = handler.doMove(options, member, anchor?.element, targetClass)
                movedMembers[member] = SmartPointerManager.createPointer(newMember)
                fixModifierList(member, newMember, usages, targetClass)
                for (reference in refsToBeRebind) {
                    try {
                        reference.bindToElement(newMember)
                    } catch (e: Throwable) {
                        logger.error(e) {}
                    }
                }
            }
        }

        // qualifier info must be decoded after members are moved
        val handler = MoveMemberHandler.EP_NAME.forLanguage(targetClass.language)
        handler?.decodeContextInfo(targetClass)
        return movedMembers
    }

    private fun beforeAllMovements(membersToMove: Set<PsiMember>) {
        val result = HashMap<PsiElement, PsiElement>()
        for (psiElement in membersToMove) {
            DependenciesBuilder.analyzePsiDependencies(
                psiElement, { it === psiElement.containingClass }
            ) {
                    _, _, info
                ->
                if (info.providerCache === INullablePsiCache.EMPTY) return@analyzePsiDependencies
                val providerPsi = info.providerCache.getPsi(project)
                if (providerPsi is PsiModifierListOwner) {
                    val modifierList = providerPsi.modifierList ?: return@analyzePsiDependencies
                    val oldPsi = modifierList.copy()
                    VisibilityUtil.setVisibility(modifierList, defaultVisibility)
                    result[oldPsi] = modifierList
                }
            }
        }
        for ((old, new) in result) {
            transaction.getElementListener(old).elementMoved(new)
        }
    }

    private fun afterAllMovements(movedMembers: Map<PsiMember, SmartPsiElementPointer<PsiMember>>) {
        for ((oldMember, value) in movedMembers) {
            val newMember = value.element
            if (newMember != null) {
                transaction.getElementListener(oldMember).elementMoved(newMember)
            }
        }
    }

    private fun fixModifierList(
        member: PsiMember, newMember: PsiMember, usages: List<MoveMembersUsageInfo>,
        targetClass: PsiClass,
    ) {
        val modifierList = newMember.modifierList
        if (modifierList != null && targetClass.isInterface) {
            modifierList.setModifierProperty(PsiModifier.PUBLIC, false)
            modifierList.setModifierProperty(PsiModifier.PROTECTED, false)
            modifierList.setModifierProperty(PsiModifier.PRIVATE, false)
            if (newMember is PsiClass) {
                modifierList.setModifierProperty(PsiModifier.STATIC, false)
            }
            return
        }
        val filtered: MutableList<UsageInfo> = ArrayList()
        for (usage in usages) {
            if (member === usage.member) {
                filtered.add(usage)
            }
        }
        val infos = filtered.toArray<UsageInfo>(UsageInfo.EMPTY_ARRAY)
        VisibilityUtil.fixVisibility(UsageViewUtil.toElements(infos), newMember, PsiModifier.PUBLIC)
    }

    override fun getCommandName(): String {
        return RefactoringBundle.message("move.members.title")
    }

    companion object {
        @JvmStatic
        val defaultVisibility = PsiModifier.PUBLIC
    }
}