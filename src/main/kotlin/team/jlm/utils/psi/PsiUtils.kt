package team.jlm.utils.psi

import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.psi.impl.PsiFileFactoryImpl
import com.intellij.psi.impl.source.resolve.FileContextUtil
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.parentOfType
import com.intellij.testFramework.LightVirtualFile
import mu.KotlinLogging
import java.util.stream.Collectors


private val logger = KotlinLogging.logger { }

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

fun getAllClassesInProject(project: Project): MutableList<PsiClass> {
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

fun findPsiClass(project: Project, className: String): PsiClass? {
    val javaPsiFacade = JavaPsiFacade.getInstance(project)
    return javaPsiFacade.findClass(className, GlobalSearchScope.projectScope(project))
}

fun PsiElement.getOuterClass(strict: Boolean = false): PsiClass? {
    val result = PsiTreeUtil.getParentOfType(this, PsiClass::class.java, strict)
        ?: return null
    if (result is PsiAnonymousClass) {
        return null
    }
    if (result.getOuterClass(true) != null) return null
    return result
}

/**
 * 获取PsiExpression的目标类型
 * ```java
 * Parent parent = new Child();
 * ```
 * 上述代码中，new表达式的目标类型是`Parent`
 */
fun PsiExpression.getTargetType(project: Project): PsiType? {
    val context = this.context ?: return null
    return when (context) {
        is PsiReturnStatement -> {
            val psiMethod = context.parentOfType<PsiMethod>() ?: return null
            psiMethod.returnType
        }

        is PsiField -> context.type
        is PsiVariable -> context.type
        is PsiExpression, is PsiExpressionList -> type
        is PsiExpressionStatement, is PsiIfStatement, is PsiWhileStatement,
        is PsiSwitchStatement, is PsiCaseLabelElementList, is PsiCaseLabelElement,
        is PsiSynchronizedStatement,
        -> PsiType.getJavaLangObject(manager, GlobalSearchScope.projectScope(project))

        is PsiThrowStatement -> PsiType.getTypeByName(
            java.lang.Throwable::class.java.name,
            project,
            GlobalSearchScope.projectScope(project)
        )

        is PsiForeachStatement -> PsiType.getTypeByName(
            java.lang.Iterable::class.java.name,
            project,
            GlobalSearchScope.projectScope(project)
        )

        is PsiNameValuePair, is PsiAnnotationMethod -> null

        else -> {
            logger.trace { context }
            null
        }
    }
}

val AnActionEvent.psiElementAtMousePointer: PsiElement?
    get() {
        val caret = getData(CommonDataKeys.CARET) ?: return null
        val offset = caret.offset
        val psiFile = getData(CommonDataKeys.PSI_FILE) ?: return null
        return psiFile.findElementAt(offset)
    }