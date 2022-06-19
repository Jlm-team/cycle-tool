package team.jlm.utils.gittools.tools

import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.impl.PsiFileFactoryImpl
import com.intellij.util.Base64
import java.util.regex.Matcher
import java.util.regex.Pattern





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
