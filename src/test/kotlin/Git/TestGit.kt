package Git

import team.jlm.coderefactor.util.git.GitUtils

fun main(){
   val  gittools: GitUtils = GitUtils("E:\\code\\Cassandra")
    val res = gittools.getCommits()
    for (i in res) {
        println(i)
    }
    gittools.getDiffBetweenCommit(res[1],res[2])
//    val diff = gittools.getDiffBetweenCommit(res.get(1),res.get(2))
//    val out = ByteArrayOutputStream()
//    val df = DiffFormatter(out)
//    df.setRepository(gittools.git.repository)
//    val hander =df.toFileHeader(diff[92])
    //PASS
}