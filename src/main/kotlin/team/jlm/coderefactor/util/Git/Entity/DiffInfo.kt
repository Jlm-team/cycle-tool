package team.jlm.coderefactor.util.Git.Entity

data class DiffInfo(
    val classFile: String,
    val className: String,
    val packageName: String,
    val methodInfo: List<MethodInfo>,
    val addLines: List<Pair<Int,Int>>,
    val delLines: List<Pair<Int,Int>>,
    val type: String
)
