package team.jlm.refactoring.move.staticA2B

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import com.intellij.refactoring.move.MoveCallback
import com.intellij.refactoring.move.moveMembers.MoveMembersOptions
import com.intellij.refactoring.move.moveMembers.MoveMembersProcessor
import com.intellij.usageView.UsageInfo
import com.intellij.usageView.UsageViewDescriptor
import team.jlm.refactoring.IRefactoringProcessor

class MoveStaticMembersProcessor(
    project: Project,
    options: MoveMembersOptions,
    moveCallback: MoveCallback? = null,
    prepareSuccessfulCallback: Runnable? = null,
) : MoveMembersProcessor(project, moveCallback, options), IRefactoringProcessor {
    override var myPrepareSuccessfulSwingThreadCallback: Runnable? = prepareSuccessfulCallback
    override fun createUsageViewDescriptor(): UsageViewDescriptor {
        return super.createUsageViewDescriptor(emptyArray())
    }

    override fun execute(usages: Array<out UsageInfo>) {
        super.execute(usages)
    }

    override fun findUsages(): Array<UsageInfo> {
        return super.findUsages()
    }

    override fun getCommandName(): String {
        return super.getCommandName()
    }

    override fun isPreviewUsages(): Boolean {
        return super.isPreviewUsages()
    }

    override fun preprocessUsages(refUsage: Ref<Array<out UsageInfo>>): Boolean {
        return super<MoveMembersProcessor>.preprocessUsages(refUsage)
    }

    override fun isPreviewUsages(usages: Array<out UsageInfo>): Boolean {
        return super.isPreviewUsages(usages)
    }

    override fun refreshElements(elements: Array<out PsiElement>) {
        super.refreshElements(elements)
    }

    override var callFromMulti: Boolean = false
}