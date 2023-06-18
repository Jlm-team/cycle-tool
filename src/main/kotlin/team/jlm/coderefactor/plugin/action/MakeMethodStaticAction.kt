package team.jlm.coderefactor.plugin.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.psi.PsiMethod
import org.jetbrains.annotations.TestOnly
import team.jlm.refactoring.BaseRefactoring
import team.jlm.refactoring.makeStatic.MakeMethodStaticProcessor

@TestOnly
class MakeMethodStaticAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val rf = object : BaseRefactoring<MakeMethodStaticProcessor>(
            MakeMethodStaticProcessor(e.project!!, e.getData(CommonDataKeys.PSI_ELEMENT) as PsiMethod)
                .apply { setPreviewUsages(true) }
        ) {}
        rf.run()
    }

    override fun update(e: AnActionEvent) {
        val psiMethod = e.getData(CommonDataKeys.PSI_ELEMENT)
        e.presentation.isEnabled = psiMethod is PsiMethod
    }
}