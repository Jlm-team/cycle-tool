package team.jlm.refactoring.replace

import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNewExpression
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.refactoring.RefactoringBundle
import com.intellij.usageView.UsageInfo
import com.intellij.usageView.UsageViewBundle
import com.intellij.usageView.UsageViewDescriptor
import team.jlm.refactoring.BaseRefactoringProcessor
import team.jlm.utils.bundle.messageBundle
import team.jlm.utils.psi.getTargetType

class ReplaceByReflectProcessor(
    project: Project,
    refactoringScope: SearchScope = GlobalSearchScope.projectScope(project),
    prepareSuccessfulCallback: Runnable? = null,
    private val psiNewExpression: PsiNewExpression,
) : BaseRefactoringProcessor(project, refactoringScope, prepareSuccessfulCallback) {
    override fun createUsageViewDescriptor(): UsageViewDescriptor {
        return object : UsageViewDescriptor {
            override fun getElements(): Array<PsiElement> {
                return arrayOf(psiNewExpression)
            }

            override fun getProcessedElementsHeader(): String {
                return messageBundle.getString("replace.byReflect.elements.header")
            }

            override fun getCodeReferencesText(usagesCount: Int, filesCount: Int): String {
                return RefactoringBundle.message(
                    "references.to.be.changed",
                    UsageViewBundle.getReferencesString(usagesCount, filesCount)
                )
            }
        }
    }

    override fun findUsages(): Array<out UsageInfo> {
        return emptyArray()
    }

    override fun performRefactoring(usages: Array<out UsageInfo>) {
        val className = psiNewExpression.classReference!!.qualifiedName
        val listener = refactoringTransaction.getElementListener(psiNewExpression)
        val manager = psiNewExpression.manager
        val factory = JavaPsiFacade.getElementFactory(manager.project)
        val needType = psiNewExpression.getTargetType(myProject!!)
        val target = factory.createExpressionFromText(
            "(${
                if (needType != null) {
                    "(${needType.presentableText}) "
                } else {
                    ""
                }
            }Class.forName(\"${className}\").newInstance())",
            psiNewExpression.context
        )
        psiNewExpression.replace(target)
        listener.elementMoved(target)
        refactoringTransaction.commit()
    }

    override fun getCommandName(): String {
        return messageBundle.getString("replace.byReflect.title")
    }
}