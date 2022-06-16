package team.jlm.coderefactor.util.git.entity

import team.jlm.coderefactor.util.git.entity.MethodInfo

data class DiffInfo(
    val classFile: String,
    val className: String,
    val packageName: String,
    val methodInfo: List<MethodInfo>,
    val addLines: List<Pair<Int,Int>>,
    val delLines: List<Pair<Int,Int>>,
    val type: String
)
