package team.jlm.coderefactor.plugin.ui

import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JPanel
import javax.swing.table.DefaultTableColumnModel
import javax.swing.table.TableColumn

object DefaultScrollPaneTable {
    fun getScrollTable(data: Array<Array<String?>>, column: Array<String>): JPanel {
        val layoutConstraints = GridBagConstraints()
        layoutConstraints.fill = GridBagConstraints.HORIZONTAL
        layoutConstraints.weightx = 1.0
        layoutConstraints.gridx = 0
        layoutConstraints.gridy = 0
        if (data.isNotEmpty()) {
            val tableModel = SampleTableModel(data, column)
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
}