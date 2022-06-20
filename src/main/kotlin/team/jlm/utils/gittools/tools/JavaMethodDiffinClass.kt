package team.jlm.utils.gittools.tools


import com.intellij.psi.PsiMethod
import com.intellij.util.Base64


fun getMethodDiff(oldMethod: PsiMethod, newMethod: PsiMethod) {

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




