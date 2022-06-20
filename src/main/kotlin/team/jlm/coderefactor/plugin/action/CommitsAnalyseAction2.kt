package team.jlm.coderefactor.plugin.action

import com.github.difflib.DiffUtils
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.Change
import team.jlm.utils.*
import team.jlm.utils.change.analyseJavaFile

class CommitsAnalyseAction2 : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val gitRepos = project.gitRepositories
        for (repo in gitRepos) {
            println(repo)
        }
        val repo = gitRepos[0]
        computeWithModalProgress(project, "Analysing...") {
            val commits = repo.commits
//            commits.forEach { println(it) }
            for (i in 0 until commits.size - 1) {
                val changes = filterOnlyJavaSrc(repo.diff(commits[i + 1], commits[i]))
                for (change in changes) {
                    println(change)
                    analyseJavaFile(project, change)
//                    analyseChange(change, project)
                }
                it?.fraction = 1.0 * i / commits.size
            }
        }.run { }
    }
}
//        SlowOperations.allowSlowOperations(ThrowableRunnable {
//            for (commit in commits) {
//                println(commit)
//                checkout(repo, commit.id.asString(), null, true, false)
//                showClassesInProject(project, commit.id.asString() + ".png")
//            }
//        }).run { }

fun analyseChange(change: Change, project: Project) {
    if (change.type != Change.Type.MODIFICATION) {
        return
    }
    val beforeRevision = change.beforeRevision
    val afterRevision = change.afterRevision
    if (beforeRevision == null || afterRevision == null) {
        return
    }
    val beforeContent = beforeRevision.content
    val afterContent = afterRevision.content
    if (beforeContent == null || afterContent == null) {
        return
    }
    val results = DiffUtils.diff(beforeContent.toList(), afterContent.toList()).deltas
    val beforeJavaPsi = getPsiJavaFile(project, beforeContent)
    val afterJavaPsi = getPsiJavaFile(project, afterContent)
    for (result in results) {
        val beforeEle = String(result.source.lines.toCharArray())
        val beforeEleTrim = beforeEle.trim()
        if (beforeEleTrim.isNotEmpty()) {
            var beforePsi = beforeJavaPsi.findElementAt(beforeContent.indexOf(beforeEleTrim))
            if (beforePsi != null) {
                while (beforePsi != null && !beforePsi.textMatches(beforeEleTrim)) {
                    if (beforePsi.textLength > beforeEleTrim.length) {
                        break
                    }
                    beforePsi = beforePsi.parent
                }
                println(beforePsi)
            }
        }
        val afterEle = String(result.target.lines.toCharArray())
        val afterEleTrim = afterEle.trim()
        if (afterEleTrim.isNotEmpty()) {
            var afterPsi = afterJavaPsi.findElementAt(afterContent.indexOf(afterEleTrim))
            if (afterPsi != null) {
                while (afterPsi != null && !afterPsi.textMatches(afterEleTrim)) {
                    if (afterPsi.textLength > afterEleTrim.length) {
                        break
                    }
                    afterPsi = afterPsi.parent
                }
                println(afterPsi)
            }
            println(afterPsi)
        }
        println("${result.source}, ${result.target}")
    }
}
