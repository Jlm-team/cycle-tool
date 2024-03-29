package team.jlm.coderefactor.plugin.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.psi.PsiMethod
import org.jetbrains.annotations.TestOnly
import team.jlm.refactoring.BaseRefactoring
import team.jlm.refactoring.IRefactoringProcessor
import team.jlm.refactoring.makeStatic.createMakeMethodStaticProcess

@TestOnly
class MakeMethodStaticAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val rf = object : BaseRefactoring<IRefactoringProcessor>(
            createMakeMethodStaticProcess(
                e.project!!,
                e.getData(CommonDataKeys.PSI_ELEMENT) as PsiMethod
            )
        ) {}
        rf.run()
    }

    override fun update(e: AnActionEvent) {
        val psiMethod = e.getData(CommonDataKeys.PSI_ELEMENT)
        e.presentation.isEnabled = psiMethod is PsiMethod
    }
}