package team.jlm.coderefactor.plugin.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.readAction
import com.xyzboom.algorithm.graph.Graph
import com.xyzboom.algorithm.graph.Tarjan
import team.jlm.coderefactor.code.IG
import team.jlm.utils.getAllClassesInProject

class CycleDependencyAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val classes = e.project?.let { getAllClassesInProject(it) } ?: return
        classes.removeIf {
            val path = it.containingFile.originalFile.containingDirectory.toString()
            it.containingClass != null
                    || path.contains("test", true) || path.contains("docs", false)
                    || path.contains("examples", true)
        }
        val ig = IG(classes)
        val tarjan = Tarjan(ig)
        val result = tarjan.result
        for (row in result) {
            if (row.size != 2) {
                ig.delNode(row[0].data)
            }
        }
        println()
    }
}
