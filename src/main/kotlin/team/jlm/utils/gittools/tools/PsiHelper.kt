package team.jlm.utils.gittools.tools

import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.impl.PsiFileFactoryImpl
import com.intellij.util.Base64
import team.jlm.utils.gittools.entity.DiffInfo
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

//fun getDiffInfo(newFile: PsiJavaFile, oldFile: PsiJavaFile, newPath: String): ArrayList<DiffInfo> {
//    val res = ArrayList<DiffInfo>()
//    val classPackage = newFile.packageName
//    val newMethods = getJavaClassMethods(getAllClassesInJavaFile(newFile))
//    val oldMethods = getJavaClassMethods(getAllClassesInJavaFile(oldFile))
//    for ((k, v) in newMethods) {
//        if (isContainsClassinSameFile(k, oldMethods.keys)) { //判断新提交中的类是否为新增
//            val newMethod = ArrayList<PsiMethod>()
//            for (m in v) { //若不是，则遍历方法
//                var nextMethodFlag = false
//                var findMethod = false
//                val oldMethodsList = getContainsinMap(oldMethods, k)!!
//                val listIndex = oldMethodsList.size
//                for (i in 0..listIndex - 1) {
//                    val oldm = oldMethodsList[i]
//                    if (isSameMethods(m, oldm)) //函数名称，参数列表相同
//                    {
//                        if (isEqualMethods(m, oldm)) //函数MD5摘要相同，未改变 若源经过混淆，无法比较
//                        {
//                            nextMethodFlag = true
//                            break
//                        } else //到此，说明函数内部改变
//                        {
//                            for (j in i + 1..listIndex - 1) //继续遍历剩下的元素
//                            {
//                                val oldm_ = oldMethodsList[j]
//                                if (isSameMethods(m, oldm_)) {
//                                    if (isEqualMethods(m, oldm_)) {
//                                        nextMethodFlag = true //如果依然找到了相同的，则说明if语句找到的为同一文件同名函数的不同实现
//                                        break
//                                    }
//                                }
//                            }
//                            if (!nextMethodFlag) {
//                                findMethod = true
//                                newMethod.add(m)
//                            }
//                        }
//
//                    }
//                }
//                //未找到，说明新增/修改
//                if (nextMethodFlag)
//                    continue
//                if(!findMethod) //若在旧版本中没有找到
//                    newMethod.add(m)
//            }
//            res.add(DiffInfo(newPath, k.name.toString(), classPackage, newMethod))
//        } else { //旧版本不存在，为新增类
//            res.add(DiffInfo(newPath, k.name.toString(), classPackage, v))
//        }
//    }
//    return res
//}

fun getContainsinMap(map: Map<PsiClass, ArrayList<PsiMethod>>, k: PsiClass): ArrayList<PsiMethod>? {
    for (i in map.keys) {
        if (i.name == k.name)
            return map.get(i)
    }
    return null
}

/**
 * @Description: 在同一文件中，只要类名相同，就说明此类并非新加入的类
 * @param c PsiClass
 * @param cl Set<PsiClass>
 * @return Boolean
 */
fun isContainsClassinSameFile(c: PsiClass, cl: Set<PsiClass>): Boolean {
    for (i in cl) {
        if (i.name == c.name)
            return true
    }
    return false
}

fun md5Encoder(str: String): String {
    val md5str = replaceBlank(str)
    return Base64.encode(md5str.toByteArray())
}

fun isSameMethods(m1: PsiMethod, m2: PsiMethod): Boolean {
    if (m1.name != m2.name) { //函数名不同，不同
        return false
    }
    if (m1.returnType.toString() != m2.returnType.toString()) //返回值不同，不同
        return false

    if (m1.parameterList.parameters.isEmpty() && m2.parameterList.parameters.isEmpty())
        return true

    if (m1.modifierList.text == m2.modifierList.text)
        return true

    val parame = m2.parameterList.parameters //参数不同，不同
    for (i in m1.parameterList.parameters) {
        for (j in parame) {
            if (i.name == j.name && i.type.toString() == j.type.toString()) {
                return true
            }
        }
    }

    return false
}

fun replaceBlank(str: String): String {
    var dest = ""
    val p: Pattern = Pattern.compile("\\s*|\t|\r|\n")
    val m: Matcher = p.matcher(str)
    dest = m.replaceAll("")
    return dest
}

fun isEqualMethods(m1: PsiMethod, m2: PsiMethod): Boolean {
    if (m1.body == null || m2.body == null) {
        if (m1.body == null && m2.body == null)
            return true
        return false
    }
    if (md5Encoder(m1.body!!.text) == md5Encoder(m2.body!!.text))
        return true
    return false
}