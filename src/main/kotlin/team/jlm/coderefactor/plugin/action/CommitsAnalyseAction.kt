package team.jlm.coderefactor.plugin.action

import com.github.difflib.DiffUtils
import com.intellij.diff.util.DiffUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.psi.impl.PsiFileFactoryImpl
import com.intellij.psi.search.searches.ReferenceSearcher
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.CachedValue
import com.intellij.util.SlowOperations
import com.intellij.util.ThrowableRunnable
import git4idea.GitCommit
import org.jetbrains.uast.UElement
import org.jetbrains.uast.test.env.findUElementByTextFromPsi
import team.jlm.utils.*

class CommitsAnalyseAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val gitRepos = project.gitRepositories
        for (repo in gitRepos) {
            println(repo)
        }
        val repo = gitRepos[0]
        SlowOperations.allowSlowOperations(ThrowableRunnable {
            val commits = repo.commits
//            commits.forEach { println(it) }
            val changes = filterOnlyJavaSrc(repo.diff(commits[1], commits[0]))
            for (change in changes) {
                println(change)
                val beforeRevision = change.beforeRevision
                val afterRevision = change.afterRevision
                if (beforeRevision == null || afterRevision == null) {
                    continue
                }
                val beforeContent = beforeRevision.content
                val afterContent = afterRevision.content
                if (beforeContent == null || afterContent == null) {
                    return@ThrowableRunnable
                }
                val results = DiffUtils.diffInline(beforeContent, afterContent).deltas
                val beforeJavaPsi = getPsiJavaFile(project, beforeContent)
                val afterJavaPsi = getPsiJavaFile(project, afterContent)
                for (result in results) {
                    for (beforeEle in result.source.lines) {
                        val beforePsi = beforeJavaPsi.findElementAt(beforeContent.indexOf(beforeEle.trim()))
                        println(beforePsi)
                    }
                    for (afterEle in result.target.lines) {
                        val afterPsi = afterJavaPsi.findElementAt(afterContent.indexOf(afterEle.trim()))
                        println(afterPsi)
                    }
                    println("${result.source}, ${result.target}")
                }
            }
        }).run { }
//        SlowOperations.allowSlowOperations(ThrowableRunnable {
//            for (commit in commits) {
//                println(commit)
//                checkout(repo, commit.id.asString(), null, true, false)
//                showClassesInProject(project, commit.id.asString() + ".png")
//            }
//        }).run { }
    }
}