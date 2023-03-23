package team.jlm.coderefactor.code

import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiImportStatement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.IncorrectOperationException
import team.jlm.utils.getAllJavaFilesInProject

fun removeUnusedImport(project: Project): Int {
    val javaFileList = getAllJavaFilesInProject(project)
    var resolve = 0
    var unResolve = 0
    javaFileList.forEach { psiFile ->
        val imports = psiFile.importList ?: return@forEach
        val unused = ArrayList<PsiImportStatement>(8)
        val scope = GlobalSearchScope.fileScope(psiFile)
        imports.importStatements.forEach {
            val psiClass = it.qualifiedName?.let { name -> JavaPsiFacade.getInstance(project).findClass(name, scope) }
            if (psiClass == null)
                unused.add(it)
        }
        unused.forEach {
            try {
                it.delete()
                resolve++
            } catch (e: IncorrectOperationException) {
                unResolve++
            }
        }
    }
    return resolve
}