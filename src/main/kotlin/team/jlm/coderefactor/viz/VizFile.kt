package team.jlm.coderefactor.viz

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiPackage

class VizFile(private var file: PsiJavaFile) {
    init {
        val elements = file.getOnDemandImports(true, true)
//        PsiManager
    }
}