package team.jlm.coderefactor.plugin.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.xyzboom.algorithm.graph.Graph
import team.jlm.coderefactor.plugin.ui.UMLDialog
import team.jlm.utils.uml.UMLGraphXAdapter
import team.jlm.utils.uml.buildGraph
import team.jlm.utils.uml.myGraphType

class ShowUML : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
        val umlAdapter =UMLGraphXAdapter()
        umlAdapter.graph = buildGraph(graph!!, type!!)
        val umlDialog = UMLDialog(project)
        umlDialog.umlGraph = umlAdapter
    }

    companion object {
        var graph: Graph<String>? = null
        var type: myGraphType? = null
    }

}