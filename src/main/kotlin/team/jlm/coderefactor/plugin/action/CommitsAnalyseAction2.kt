package team.jlm.coderefactor.plugin.action

import com.github.difflib.DiffUtils
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.Change
import com.intellij.psi.PsiField
import com.intellij.psi.PsiStatement
import com.intellij.psi.util.PsiUtil
import com.intellij.psi.util.nextLeaf
import team.jlm.coderefactor.code.PsiGroup
import team.jlm.utils.*

class CommitsAnalyseAction2 : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val gitRepos = project.gitRepositories
        for (repo in gitRepos) {
            println(repo)
        }
        val repo = gitRepos[0]
        runReadAction {
            val commits = repo.commits
//            commits.forEach { println(it) }
            for (i in 0 until 1) {
                println("${commits[i + 1]}, ${commits[i]}")
                val changes = filterOnlyJavaSrc(repo.diff(commits[i + 1], commits[i]))
                for (change in changes) {
//                    println(change)
//                    val content = change.content
//                    val beforeDependencyList = getDependencyList(content.first, project)
//                    val afterDependencyList = getDependencyList(content.second, project)
//                    println("$beforeDependencyList, $afterDependencyList")
//                    val diffResults = DiffUtils.diff(beforeDependencyList, afterDependencyList).deltas
//                    println(diffResults)
                    analyseChange(change, project)
//                    val diffs = analyseJavaFile(project, change)
//                    for (diff in diffs) {
//                        println(diff.classDiffType)
//                        for (inner in diff.classSingleDiff) {
//                            println("\t${inner.classSingleDiffType}")
//                        }
//                    }
//                    analyseChange(change, project)
                }
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
                val group = PsiGroup(beforePsi)
                while (beforePsi != null && !group.textMatches(beforeEleTrim)) {
                    if (group.textLength > beforeEleTrim.length) {
                        break
                    }
                    beforePsi = group.nextLeaf
                    beforePsi?.let { group.add(beforePsi) }
                }
//                println(beforePsi)
                val dependencies = group.dependencyList
                dependencies.forEach { print("$it, ") }
                println(dependencies.size)
            }
        }
        val afterEle = String(result.target.lines.toCharArray())
        val afterEleTrim = afterEle.trim()
        if (afterEleTrim.isNotEmpty()) {
            var afterPsi = afterJavaPsi.findElementAt(afterContent.indexOf(afterEleTrim))
            if (afterPsi != null) {
                val group = PsiGroup(afterPsi)
                while (afterPsi != null && !group.textMatches(afterEleTrim)) {
                    if (group.textLength > afterEleTrim.length) {
                        break
                    }
                    afterPsi = group.nextLeaf
                    afterPsi?.let { group.add(afterPsi) }
                }
//                println(beforePsi)
                val dependencies = group.dependencyList
                dependencies.forEach { print("$it, ") }
                println(dependencies.size)
            }
        }
//        println("${result.source}, ${result.target}")
    }
}
