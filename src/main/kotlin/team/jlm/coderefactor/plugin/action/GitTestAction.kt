package team.jlm.coderefactor.plugin.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.util.SlowOperations
import team.jlm.utils.commits
import team.jlm.utils.gitRepositories
import team.jlm.utils.gittools.tools.getDiffBetweenCommit

class GitTestAction: AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
        val gitRepos = project!!.gitRepositories

        for (repo in gitRepos) {
            println(repo)
        }
        val repo = gitRepos[0]
        val commits = repo.commits
        getDiffBetweenCommit(commits[0],commits[1],repo,project)
    }
}