package team.jlm.utils.file

fun getTempPath(): String = "F:\\Temp"

fun getFileSeparator(): String {
    return System.getProperty("file.separator")
}

fun getSavePath(className: String, pathSuffix: String): String {
    val fileSeparator = getFileSeparator()
    return getTempPath() + fileSeparator + pathSuffix + fileSeparator +
            className.replace(".", fileSeparator) + ".json"

}
