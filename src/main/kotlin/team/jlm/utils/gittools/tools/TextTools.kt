package team.jlm.utils.gittools.tools

import org.apache.commons.lang.StringUtils


fun splitString(str: String?): List<String>? {
    if (str !== null) {
        return str.split(Regex("\\R"))
    }
    return null
}

fun replaceNote(text: String?): ArrayList<String> {
    val sb = StringBuilder()
    val lines = splitString(text)
    if (lines == null) return ArrayList()
    for (line in lines) {
        if (line.trim().matches(Regex("\\s*"))) {
            // 空行
            continue
        }
        if (line.matches(Regex(".*//.*"))) {
            if (StringUtils.isEmpty(line.substring(0, line.indexOf("//")).trim())) {
                // 属于直接注释
                continue
            }
            // 包含双斜杠注释
            if (line.matches(Regex(".*\".*//(?! ).*"))) {
                // 反斜杠在字符串里面如: String url = "http://www.baidu.com"
                sb.append(line).append("\n")
            } else if (line.indexOf(";") > 0 || line.indexOf("(") > 0 || line.indexOf("{") > 0) {
                // 内容后面如：int status = 0; // 状态
                sb.append(line.substring(0, line.indexOf("//"))).append("\n")
            }
        } else {
            // 不包含 双斜杠注释
            sb.append(line).append("\n")
        }
    }
    // 过滤到有提示注释
    val newStr = sb.toString().replace(Regex("/\\*{1,2}[\\s\\S]*?\\*/"), "").replace(Regex("(?m)^\\s*$(\\n|\\r\\n)"), " ")
    val res = ArrayList<String>()
    val strlist =splitString(newStr)!!
    val size = strlist.size
    var cflag = false
    for (i in 0..size-1) {
        if (strlist[i] == "")
            continue
        else {
            if(cflag)
            {
                cflag=false
                continue
            }
            if(strlist[i].endsWith(",") or strlist[i].endsWith("."))
            {
                res.add(strlist[i].replaceFirst(Regex("^\\s+"), "")+strlist[i+1].replaceFirst(Regex("^\\s+"), ""))
                cflag = true
            }
            else
                res.add(strlist[i].replaceFirst(Regex("^\\s+"), ""))
        }
    }
    return res
}