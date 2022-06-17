package team.jlm.utils

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
import java.util.*
import java.util.stream.Collectors

fun getAllJavaFilesInProject(project: Project): List<PsiJavaFile> {
    val files = ArrayList<PsiJavaFile>()
    ProjectFileIndex.SERVICE.getInstance(project).iterateContent {
        val psiFile = PsiManager.getInstance(project).findFile(it)
        if ((psiFile is PsiJavaFile)
            && !psiFile.isDirectory() &&
            ("JAVA" == psiFile.getFileType().name)
        ) {
            files.add(psiFile)
        }
        true
    }
    return files
}

fun getAllClassesInJavaFile(file: PsiJavaFile): List<PsiClass> {
    val allClasses = ArrayList<PsiClass>()
    val psiClasses: Array<PsiClass> = file.classes
    for (psiClass in psiClasses) {
        allClasses.add(psiClass)
        allClasses.addAll(listOf(*psiClass.innerClasses))
    }
    return allClasses
}

fun getAllClassesInProject(project: Project): List<PsiClass> {
    val files = getAllJavaFilesInProject(project)
    return files.stream().flatMap {
        getAllClassesInJavaFile(it).stream()
    }.collect(Collectors.toList())
}
