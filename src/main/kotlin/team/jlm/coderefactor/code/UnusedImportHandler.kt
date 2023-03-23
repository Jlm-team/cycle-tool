package team.jlm.coderefactor.code

import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiImportStatement
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.search.GlobalSearchScope

fun removeUnusedImport(project: Project, psiFile: PsiJavaFile): Int {
    val imports = psiFile.importList ?: return 0
    val unused = ArrayList<PsiImportStatement>(8)
    val scope = GlobalSearchScope.fileScope(psiFile)
    imports.importStatements.forEach {
        val psiClass = it.qualifiedName?.let { name -> JavaPsiFacade.getInstance(project).findClass(name, scope) }
        if (psiClass == null)
            unused.add(it)
    }
    unused.forEach(PsiElement::delete)
    return unused.size
}