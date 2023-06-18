package team.jlm.refactoring.makeStatic

import com.intellij.psi.*
import com.intellij.psi.util.InheritanceUtil
import com.intellij.psi.util.PsiTreeUtil

fun findClassRefsInMember(member: PsiTypeParameterListOwner, includeSelf: Boolean): Array<InternalUsageInfo> {
    val containingClass = member.containingClass!!
    val classRefs = ArrayList<InternalUsageInfo>()
    addClassRefs(member, classRefs, containingClass, member, includeSelf)
    return classRefs.toTypedArray()
}

private fun addClassRefs(
    originalMember: PsiTypeParameterListOwner,
    classRefs: ArrayList<in InternalUsageInfo>,
    containingClass: PsiClass,
    element: PsiElement,
    includeSelf: Boolean,
) {
    if (element is PsiReferenceExpression) {
        if (!element.isQualified) { // resolving only "naked" fields and methods
            val resolved = element.resolve()
            if (resolved is PsiMember && !resolved.hasModifierProperty(PsiModifier.STATIC)) {
                if (originalMember.manager.areElementsEquivalent(resolved, originalMember)) {
                    if (includeSelf) {
                        classRefs.add(SelfUsageInfo(element, originalMember))
                    }
                } else {
                    val memberContainingClass = resolved.containingClass
                    if (originalMember !is PsiClass || !isPartOf(
                            memberContainingClass,
                            originalMember
                        )
                    ) {
                        if (isPartOf(memberContainingClass, containingClass)) {
                            classRefs.add(InternalUsageInfo(element, resolved))
                        }
                    }
                }
            }
        }
    } else if (element is PsiThisExpression && element.getParent() !is PsiReceiverParameter) {
        val qualifier = element.qualifier
        val refElement = if (qualifier != null) qualifier.resolve() else PsiTreeUtil.getParentOfType(
            element,
            PsiClass::class.java
        )
        if (refElement is PsiClass && refElement != originalMember && isPartOf(
                refElement as PsiClass?,
                containingClass
            )
        ) {
            val parent = element.getParent()
            if (parent is PsiReferenceExpression && parent.isReferenceTo(originalMember)) {
                if (includeSelf) {
                    classRefs.add(SelfUsageInfo(parent, originalMember))
                }
            } else {
                classRefs.add(InternalUsageInfo(element, refElement))
            }
        }
    } else if (element is PsiSuperExpression) {
        val qualifier = element.qualifier
        val refElement = if (qualifier != null) qualifier.resolve() else PsiTreeUtil.getParentOfType(
            element,
            PsiClass::class.java
        )
        if (refElement is PsiClass) {
            if (isPartOf(refElement as PsiClass?, containingClass)) {
                if (!(originalMember is PsiClass && isPartOf(
                        refElement as PsiClass?,
                        originalMember
                    ))
                ) {
                    classRefs.add(InternalUsageInfo(element, refElement))
                }
            }
        }
    } else if (element is PsiNewExpression) {
        val classReference = element.classReference
        if (classReference != null) {
            val refElement = classReference.resolve()
            if (refElement is PsiClass) {
                val hisClass = refElement.containingClass
                if (hisClass !== originalMember && isPartOf(
                        hisClass,
                        containingClass
                    ) && !refElement.hasModifierProperty(
                        PsiModifier.STATIC
                    )
                ) {
                    classRefs.add(InternalUsageInfo(element, refElement))
                }
            }
        }
    }
    val children = element.children
    for (child in children) {
        addClassRefs(originalMember, classRefs, containingClass, child, includeSelf)
    }
}

private fun isPartOf(elementClass: PsiClass?, containingClass: PsiClass): Boolean {
    var myElementClass = elementClass
    while (myElementClass != null) {
        if (InheritanceUtil.isInheritorOrSelf(containingClass, myElementClass, true)) return true
        if (myElementClass.hasModifierProperty(PsiModifier.STATIC)) return false
        myElementClass = myElementClass.containingClass
    }
    return false
}