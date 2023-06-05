package team.jlm.coderefactor.plugin.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.util.parentOfType
import com.intellij.psi.util.parentOfTypes
import mu.KotlinLogging
import team.jlm.coderefactor.code.IG
import team.jlm.utils.psi.getAllClassesInJavaFile

private val logger = KotlinLogging.logger {}

class CycleDependencyOnOneClassAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val file = e.getData(CommonDataKeys.PSI_FILE) ?: return
        if (file !is PsiJavaFile) return
        val classes = getAllClassesInJavaFile(file, false)
        val ig = IG(classes.toMutableList())
        ig.dependencyMap
    }

    override fun update(e: AnActionEvent) {
        val psiFile = e.getData(CommonDataKeys.PSI_FILE)
        e.presentation.isEnabled = psiFile is PsiJavaFile
    }
}