package team.jlm.coderefactor.plugin.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import com.xyzboom.algorithm.graph.Graph
import com.xyzboom.algorithm.graph.Tarjan
import guru.nidi.graphviz.engine.Format
import guru.nidi.graphviz.engine.Graphviz
import team.jlm.coderefactor.code.IG
import team.jlm.coderefactor.plugin.ui.ImagePanel
import team.jlm.utils.file.pluginCacheFolderName
import team.jlm.utils.getAllClassesInProject
import java.io.File

class ShowClassesAction : AnAction() {
    companion object {
        init {

        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project

        showClassesInProject(project)
    }

}

fun showClassesInProject(project: Project?, name: String = "all_classes.png") {
    val classes = project?.let { getAllClassesInProject(it) }
//    if (classes != null) {
//        for (clazz in classes) {
//            println(clazz.name)
//        }
//    }
    val ig = classes?.let { IG(it) }
    val t = Tarjan(ig as Graph<String>)
    val result = t.result
    for (row in result) {
        if (row.size == 1) {
            ig.delNode(row[0].data)
        } else {
            for (col in row) {
                print("${col.data} ,")
            }
            println()
        }
    }
    val g = ig.toGraphvizGraph()
    val r = g.let { Graphviz.fromGraph(it).render(Format.PNG) }
    r.toFile(
        File(
            (project.basePath ?: System.getProperty("user.dir")) +
                    "/${pluginCacheFolderName}/${name}"
        )
    )
    val img = r.toImage()
    ImagePanel.img = img
    VirtualFileManager.getInstance().asyncRefresh {  }
}