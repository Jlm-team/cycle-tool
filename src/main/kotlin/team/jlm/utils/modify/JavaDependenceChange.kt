package team.jlm.utils.modify

data class JavaDependenceChange(
    val before:ArrayList<String>,
    val after:ArrayList<String>,
    val path:String
)
