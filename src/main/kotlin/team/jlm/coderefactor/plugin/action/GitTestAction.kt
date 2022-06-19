package team.jlm.coderefactor.plugin.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import team.jlm.coderefactor.util.gittools.GitUtils

class GitTestAction: AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
        val git = GitUtils("E:\\code\\TestJgit")
        val commit = git.getCommits()
        project?.let { git.getDiffBetweenCommit(commit[0],commit[1], it,4) }
    }
}