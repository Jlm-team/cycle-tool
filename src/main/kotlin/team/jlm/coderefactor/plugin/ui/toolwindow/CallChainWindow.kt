package team.jlm.coderefactor.plugin.ui.toolwindow

import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import team.jlm.refactoring.move.callchain.CallChain
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JPanel
import javax.swing.table.DefaultTableColumnModel
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableColumn

object CallChainWindow {
    fun getWindow(content: HashSet<CallChain>): JPanel {
        val layoutConstraints = GridBagConstraints()
        layoutConstraints.fill = GridBagConstraints.HORIZONTAL
        layoutConstraints.weightx = 1.0
        layoutConstraints.gridx = 0
        layoutConstraints.gridy = 0
        if (content.isNotEmpty()) {
            val data = content.map {
                arrayOf(
                    it.callerContainingClass,
                    it.callerName,
                    it.calleeContainingClass,
                    it.calleeName,
                    it.callTargetContainingClass,
                    it.callTarget
                )
            }.toTypedArray()
            val column = arrayOf("调用者所在类", "调用者", "被调用者所在类", "被调用者", "调用目标类", "调用目标")
            val tableModel = MyTableModel(data, column)

            val columnModel = DefaultTableColumnModel()
            for (i in column.indices) {
                val c = TableColumn(i)
                c.headerValue = column[i]
                columnModel.addColumn(c)
            }
            val contentTable = JBTable(tableModel, columnModel)
            val header = contentTable.tableHeader
            header.preferredSize = Dimension(contentTable.width, 25)

            val panel = JPanel(GridBagLayout())
            panel.add(header, layoutConstraints)
            layoutConstraints.gridy++
            panel.add(contentTable, layoutConstraints)
            val scrollPane = JPanel(BorderLayout())
            scrollPane.add(JBScrollPane(panel), BorderLayout.CENTER)
            return scrollPane
        } else {
            val panel = JPanel(GridBagLayout())
            val label = JBLabel("无内容")
            panel.add(label)
            val scrollPane = JPanel(BorderLayout())
            scrollPane.add(JBScrollPane(panel), BorderLayout.CENTER)
            return scrollPane
        }
    }

    private class MyTableModel(data: Array<Array<String?>>, column: Array<String>) : DefaultTableModel(data, column) {
        override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
            return if (dataVector[rowIndex][columnIndex] == null) "无" else dataVector[rowIndex][columnIndex] as Any
        }

        override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean {
            return false
        }
    }
}