package team.jlm.coderefactor.plugin.action

import com.intellij.ide.customize.CustomizeUIThemeStepPanel.ThemeInfo
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.fileEditor.impl.EditorWindow
import com.intellij.util.ui.StartupUiUtil
import com.xyzboom.algorithm.graph.GNode
import com.xyzboom.algorithm.graph.Graph
import team.jlm.utils.uml.UMLGraphXAdapter
import team.jlm.utils.uml.buildGraph
import team.jlm.utils.uml.myGraphType


class TestUML : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {

        val project = e.project
        val graph = Graph<String>()
        val nodes = ArrayList<GNode<String>>()
        for (i in 0..5) {
            nodes.add(graph.addNode(('A'.code + i.toChar().code).toChar().toString()))
        }
        graph.addEdge(nodes[0], nodes[1]) //A -> B
        graph.addEdge(nodes[0], nodes[2]) //A -> C
        graph.addEdge(nodes[1], nodes[3]) //B -> D
        graph.addEdge(nodes[1], nodes[4]) //B -> E
        graph.addEdge(nodes[2], nodes[3]) //C -> D
        graph.addEdge(nodes[3], nodes[4]) //D -> E
        graph.addEdge(nodes[3], nodes[5]) //D -> F
        graph.addEdge(nodes[1], nodes[0])

        val umlAdapter = UMLGraphXAdapter()
        umlAdapter.graph = buildGraph(graph, myGraphType.DirectedMultigraph)
        umlAdapter.init()
        val editorWindow = e.getData(EditorWindow.DATA_KEY)
        StartupUiUtil.isUnderDarcula()

        editorWindow!!.manager.addTopComponent(editorWindow.editors[0].editors[0], umlAdapter)

    }


}
