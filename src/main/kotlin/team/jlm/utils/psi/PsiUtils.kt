package team.jlm.utils

import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.PsiFileFactoryImpl
import com.intellij.psi.impl.source.resolve.FileContextUtil
import com.intellij.testFramework.LightVirtualFile
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

fun getAllClassesInJavaFile(file: PsiJavaFile, containsInnerClasses: Boolean = true): List<PsiClass> {
    val allClasses = ArrayList<PsiClass>()
    val psiClasses: Array<PsiClass> = file.classes
    for (psiClass in psiClasses) {
        allClasses.add(psiClass)
        if (containsInnerClasses) {
            allClasses.addAll(listOf(*psiClass.innerClasses))
        }
    }
    return allClasses
}

fun getPsiJavaFile(project: Project, text: String): PsiJavaFile {
    val projectPsiFile = PsiFileFactoryImpl(project)
    val psiFile = projectPsiFile.createFileFromText(JavaLanguage.INSTANCE, text) as PsiJavaFile
    return psiFile
}

fun getAllClassesInProject(project: Project): List<PsiClass> {
    val files = getAllJavaFilesInProject(project)
    return files.stream().flatMap {
        getAllClassesInJavaFile(it).stream()
    }.collect(Collectors.toList())
}

fun toVirtualFile(file: PsiFile): VirtualFile? {
    var vfile = file.virtualFile
    if (vfile == null) {
        vfile = file.originalFile.virtualFile
        if (vfile == null) {
            vfile = file.viewProvider.virtualFile
        }
    } else if (vfile is LightVirtualFile) {
        val containingFile = file.containingFile
        if (containingFile != null && containingFile !== file) {
            val originalFile: PsiFile = containingFile.originalFile
            val owningFile = originalFile.getUserData(FileContextUtil.INJECTED_IN_ELEMENT)
            if (owningFile != null) {
                vfile = owningFile.virtualFile
            }
        }
    }
    return vfile
}

private val psiMap = HashMap<String, HashMap<String, PsiJavaFile>>()
fun createOrGetJavaPsiFile(
    project: Project, text: String, commitId: String, path: String,
): PsiJavaFile {
    var commitMap = psiMap[commitId]
    if (commitMap == null) {
        commitMap = HashMap()
        psiMap[commitId] = commitMap
    }
    var wanted = commitMap[path]
    if (wanted != null) {
        return wanted
    }
    wanted = getPsiJavaFile(project, text)
    commitMap[path] = wanted
    return wanted
}

fun clearPsiMapAccordingToCommit(commitId: String) {
    psiMap.remove(commitId)
}


