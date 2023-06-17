package team.jlm.coderefactor.plugin.ui.toolwindow

import team.jlm.coderefactor.plugin.ui.DefaultScrollPaneTable
import team.jlm.utils.graph.GNode
import javax.swing.JPanel

object DependencyWindow {
    fun getWindow(content: List<ArrayList<GNode<String>>>): JPanel {
        val data = ArrayList<Array<String?>>(content.size)
        content.forEach {
            data.add(arrayOf(it[0].data, it[1].data))
        }
        val column = arrayOf("依赖点1", "依赖点2")
        return DefaultScrollPaneTable.getScrollTable(data.toTypedArray(), column)
    }
}