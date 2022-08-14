package team.jlm.coderefactor.plugin.action

import com.intellij.openapi.project.Project
import com.intellij.util.ui.StartupUiUtil
import com.xyzboom.algorithm.graph.Graph
import team.jlm.coderefactor.plugin.ui.UMLDialog
import team.jlm.utils.uml.UMLGraphXAdapter
import team.jlm.utils.uml.buildGraph
import team.jlm.utils.uml.myGraphType
import java.awt.Dimension
import java.awt.Window

fun showDialog(graph: Graph<String>, type: myGraphType, project: Project) {
    val umlAdapter = UMLGraphXAdapter()
    umlAdapter.graph = buildGraph(graph, type)
    umlAdapter.init(StartupUiUtil.isUnderDarcula())
    UMLDialog.umlGraph =umlAdapter
    val umlDialog = UMLDialog(project)
    umlDialog.show()
}