package team.jlm.coderefactor.plugin.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.psi.PsiNewExpression
import com.intellij.psi.util.parentOfType
import mu.KotlinLogging
import team.jlm.refactoring.replace.ReplaceByReflectRefactoring
import team.jlm.utils.psi.psiElementAtMousePointer

private val logger = KotlinLogging.logger {}

class ReplaceByReflectingAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val psiNewExpression = psiNewExpressionAtMousePointer(e) ?: return
        val project = e.project ?: return
        logger.trace { psiNewExpression }
        val refactoring = ReplaceByReflectRefactoring(project, psiNewExpression = psiNewExpression)
        refactoring.run()
    }

    override fun update(e: AnActionEvent) {
        val psiNewExpression = psiNewExpressionAtMousePointer(e)
        e.presentation.isEnabled = (psiNewExpression != null)
                && psiNewExpression.anonymousClass == null
                && psiNewExpression.classReference != null
    }

    private fun psiNewExpressionAtMousePointer(e: AnActionEvent): PsiNewExpression? {
        val psiElement = e.psiElementAtMousePointer ?: return null
        return psiElement.parentOfType(true)
    }
}