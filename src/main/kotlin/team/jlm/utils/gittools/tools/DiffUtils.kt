package team.jlm.utils.gittools.tools

import git4idea.GitCommit
import git4idea.repo.GitRepository
import team.jlm.utils.diff
import team.jlm.utils.filterOnlyJavaSrc

fun getDiffBetweenCommit(oldCommit:GitCommit,newCommit: GitCommit,repo: GitRepository){
    val diff = filterOnlyJavaSrc(repo.diff(oldCommit,newCommit))
    for(i in diff)
    {

    }
}