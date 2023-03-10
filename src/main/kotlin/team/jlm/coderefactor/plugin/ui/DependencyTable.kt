package team.jlm.coderefactor.plugin.ui

import com.intellij.icons.AllIcons
import com.intellij.refactoring.Refactoring
import com.intellij.ui.components.JBLabel
import com.intellij.ui.table.JBTable
import com.xyzboom.algorithm.graph.GEdge
import java.awt.Component
import java.awt.Dimension
import java.util.Vector
import javax.swing.JTable
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel

class DependencyTable : JBTable {

    val refactorsMap = ArrayList<Pair<GEdge<String>, Refactoring>>()

    private constructor(tableModel: DefaultTableModel) : super(tableModel) {
        this.tableHeader.isVisible = false
        val headerRenderer = DefaultTableCellRenderer()
        headerRenderer.preferredSize = Dimension(0, 0)
        this.tableHeader.defaultRenderer = headerRenderer
        this.autoscrolls = true
        this.tableHeader.reorderingAllowed = false
    }

    constructor(edges: List<Pair<GEdge<String>, Refactoring>>) : this(initValues(edges)) {
        refactorsMap.addAll(edges)
    }


    override fun isCellEditable(row: Int, column: Int): Boolean {
        return false
    }

    companion object {
        @JvmStatic
        private fun initValues(edges: List<Pair<GEdge<String>, Refactoring>>): DefaultTableModel {
            val values = Vector<Vector<String>>(edges.size)
            edges.forEach { (k, _) ->
                values.add(Vector(listOf(k.nodeFrom.data, "", k.nodeTo.data)))
            }
            return DefaultTableModel(values, Vector(listOf("", "")))
        }
    }


}

class DependencyTableCellRenderer : DefaultTableCellRenderer() {
    init {
        this.horizontalAlignment = CENTER
    }

    override fun getTableCellRendererComponent(
        table: JTable?,
        value: Any?,
        isSelected: Boolean,
        hasFocus: Boolean,
        row: Int,
        column: Int,
    ): Component {
        return if (column == 1) {
            val label = JBLabel(dIcon)
            label.isOpaque = false
            label
        } else super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
    }

    companion object {
        private val dIcon = AllIcons.Actions.Diff
    }

}
