package team.jlm.utils.gittools.tools


import com.intellij.psi.PsiMethod
import com.intellij.util.Base64
import org.apache.commons.lang.StringUtils

fun getMethodDiff(oldMethod: PsiMethod, newMethod: PsiMethod) {

}

fun md5Encoder(str: String): String {
    val md5str = replaceNote(str)
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

fun splitString(str: String): List<String> {
    val lines = str.split("\\R")
    return lines
}

fun replaceNote(text: String): String {
    val sb = StringBuilder()
    val lines = splitString(text)
    for (line in lines) {
        if (line.trim().matches(Regex("\\s*"))) {
            // 空行
            continue;
        }
        if (line.matches(Regex(".*//.*"))) {
            if (StringUtils.isEmpty(line.substring(0, line.indexOf("//")).trim())) {
                // 属于直接注释
                continue;
            }
            // 包含双斜杠注释
            if (line.matches(Regex(".*\".*//(?! ).*"))) {
                // 反斜杠在字符串里面如: String url = "http://www.baidu.com"
                sb.append(line).append("\n");
            } else if (line.indexOf(";") > 0 || line.indexOf("(") > 0 || line.indexOf("{") > 0) {
                // 内容后面如：int status = 0; // 状态
                sb.append(line.substring(0, line.indexOf("//"))).append("\n");
            }
        } else {
            // 不包含 双斜杠注释
            sb.append(line).append("\n");
        }
    }
    val str = sb.toString();

    // 过滤到有提示注释
    val regex = "/\\*{1,2}[\\s\\S]*?\\*/";
    val newStr = str.replace(Regex(regex), "")
    // replaceAll过滤之后会产生空行 -> 在清理一次空行
    val sb2 = StringBuffer();
    val lines2 = newStr.split("\\n");
    for (line in lines2) {
        if (line.trim().matches(Regex("\\s*"))) {
            continue;
        }
        sb2.append(line).append("\n");
    }
    return sb2.toString();
}