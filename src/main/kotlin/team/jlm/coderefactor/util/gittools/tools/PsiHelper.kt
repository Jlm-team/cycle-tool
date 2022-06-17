package team.jlm.coderefactor.util.gittools.tools

import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.impl.PsiFileFactoryImpl
import com.intellij.util.Base64
import team.jlm.utils.getAllClassesInJavaFile
import java.nio.charset.Charset


fun getPsiClass(project: Project, text: String): List<PsiClass> {
    val projectPsiFile = PsiFileFactoryImpl(project)
    val psiFile = projectPsiFile.createFileFromText(JavaLanguage.INSTANCE, text) as PsiJavaFile
    return getAllClassesInJavaFile(psiFile)
}

fun getJavaClassMethods(classes: List<PsiClass>): Map<PsiClass, ArrayList<PsiMethod>> {
    val res = hashMapOf<PsiClass, ArrayList<PsiMethod>>()
    for (i in classes) {
        val methodList = ArrayList<PsiMethod>()
        for (j in i.allMethods) {
            methodList.add(j)
        }
        res.put(i, methodList)
    }
    return res
}

fun md5Encoder(str: String): String {
    return Base64.encode(str.toByteArray())

}

fun isSameMethods(m1: PsiMethod, m2: PsiMethod): Boolean {
    if(md5Encoder(m1.text) == md5Encoder(m2.text))
        return true
    return false
}