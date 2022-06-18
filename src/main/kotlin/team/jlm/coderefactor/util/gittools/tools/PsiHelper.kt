package team.jlm.coderefactor.util.gittools.tools

import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.impl.PsiFileFactoryImpl
import com.intellij.util.Base64
import team.jlm.coderefactor.util.gittools.entity.DiffInfo
import team.jlm.utils.getAllClassesInJavaFile
import java.util.regex.Matcher
import java.util.regex.Pattern


fun getPsiJavaFile(project: Project, text: String): PsiJavaFile {
    val projectPsiFile = PsiFileFactoryImpl(project)
    val psiFile = projectPsiFile.createFileFromText(JavaLanguage.INSTANCE, text) as PsiJavaFile
    return psiFile
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

fun getDiffInfo(newFile: PsiJavaFile, oldFile: PsiJavaFile, newPath: String): ArrayList<DiffInfo> {
    val res = ArrayList<DiffInfo>()
    val classPackage = newFile.packageName
    val newMethods = getJavaClassMethods(getAllClassesInJavaFile(newFile))
    val oldMethods = getJavaClassMethods(getAllClassesInJavaFile(oldFile))
    for ((k, v) in newMethods) {
        if (oldMethods.contains(k)) { //判断新提交中的类是否为新增
            val newMethod = ArrayList<PsiMethod>()
            for (m in v) { //若不是，则遍历方法

                for (oldm in oldMethods.get(k)!!) {
                    if (isSameMethods(m, oldm)) //函数名称，参数列表相同
                    {
                        if (isEqualMethods(m, oldm)) //函数MD5摘要相同，未改变
                            break
                        else
                            newMethod.add(m)
                    }
                }
                //未找到，说明新增/修改
                newMethod.add(m)
            }
            res.add(DiffInfo(newPath, k.name.toString(), classPackage, newMethod))
        } else { //旧版本不存在，为新增类
            res.add(DiffInfo(newPath, k.name.toString(), classPackage, v))
        }
    }
    return res
}

fun md5Encoder(str: String): String {
    val md5str = replaceBlank(str)
    return Base64.encode(md5str.toByteArray())
}

fun isSameMethods(m1: PsiMethod, m2: PsiMethod): Boolean {
    if (m1.name != m2.name) {
        return false
    }
    val parame = m2.parameterList.parameters
    for (i in m1.parameterList.parameters) {
        if (!parame.contains(i))
            return false
    }
    return true
}

fun replaceBlank(str: String): String {
    var dest = ""
    val p: Pattern = Pattern.compile("\\s*|\t|\r|\n")
    val m: Matcher = p.matcher(str)
    dest = m.replaceAll("")
    return dest
}

fun isEqualMethods(m1: PsiMethod, m2: PsiMethod): Boolean {
    if (md5Encoder(m1.text) == md5Encoder(m2.text))
        return true
    return false
}