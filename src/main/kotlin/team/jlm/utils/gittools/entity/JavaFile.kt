package team.jlm.utils.gittools.entity


data class JavaFile(
    val original: JavaText,
    val modify: JavaText,
    val filePath: String
)