package team.jlm.coderefactor.plugin.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import team.jlm.refactoring.DeprecatedMethod
import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableColumnModel
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableColumn


object DeprecatedMethodWindow {

    fun getWindow(content: HashMap<String, ArrayList<DeprecatedMethod>>): JPanel {
        val layoutConstraints = GridBagConstraints()
        layoutConstraints.fill = GridBagConstraints.HORIZONTAL
        layoutConstraints.weightx = 1.0
        layoutConstraints.gridx = 0
        layoutConstraints.gridy = 0
        val panel = JPanel(GridBagLayout())
        content.forEach { (k, v) ->
            val contentPanel = CollapsiblePanel(k, v)
            panel.add(contentPanel, layoutConstraints)
            layoutConstraints.gridy++
        }
        val scrollPane = JPanel(BorderLayout())
        scrollPane.add(JBScrollPane(panel), BorderLayout.CENTER)
        return scrollPane
    }
}

class CollapsiblePanel(title: String, content: ArrayList<DeprecatedMethod>) : JPanel(),
    ActionListener {
    private var collapsed: Boolean = true
    private val titlePanel: JPanel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0))
    private val collapsedJButton = JButton(AllIcons.General.ArrowRight)
    private val contentPanel = JPanel(GridBagLayout())

    init {
        titlePanel.border = JBUI.Borders.empty(6)
        collapsedJButton.preferredSize = Dimension(16, 16)
        collapsedJButton.addActionListener(this)
        val titleLabel = JLabel(title)
        titleLabel.border = BorderFactory.createEmptyBorder(0, 10, 0, 0)
        titlePanel.add(collapsedJButton)
        titlePanel.add(titleLabel)

        val column = arrayOf("调用类", "调用位置", "类型", "被调用类", "被调用方法", "相对位置", "")
        val data = content.map {
            arrayOf(
                it.containingClass, it.methodName,
                it.type, it.deprecatedCallContainingClass, it.deprecatedCallContainingMethod, it.lineNumber, it.fileUrl
            )
        }.toTypedArray()
        val tableModel = MyTableModel(data, column)

        val columnModel = DefaultTableColumnModel()
        for (i in column.indices) {
            val c = TableColumn(i)
            c.headerValue = column[i]
            columnModel.addColumn(c)
        }
        columnModel.getColumn(column.lastIndex).width = 0
        columnModel.getColumn(column.lastIndex).cellRenderer = object : DefaultTableCellRenderer() {
            override fun setValue(value: Any?) {

            }
        }
        columnModel.getColumn(column.lastIndex).width = 0
        columnModel.getColumn(column.lastIndex).maxWidth = 0
        columnModel.getColumn(column.lastIndex).minWidth = 0
        columnModel.getColumn(column.lastIndex).resizable = false
        val contentTable = JBTable(tableModel, columnModel)
        contentTable.tableHeader.preferredSize = Dimension(contentTable.width, 20)
        contentTable.autoResizeMode = JTable.AUTO_RESIZE_ALL_COLUMNS

        contentTable.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                if (e != null && e.clickCount == 2) {
                    val row = contentTable.selectedRow
                    if (row >= 0) {
                        val fileUrl = contentTable.getValueAt(row, column.lastIndex) as String
                        val pos = contentTable.getValueAt(row, column.lastIndex - 1) as String
                        val projects = ProjectManager.getInstance().openProjects
                        for (project in projects) {
                            try {
                                val file = VirtualFileManager.getInstance().findFileByUrl(fileUrl)
                                val descriptor = OpenFileDescriptor(project, file!!, pos.toInt() - 1, 0)
                                FileEditorManager.getInstance(project).openEditor(descriptor, true)
                            } catch (e: Exception) {
                                continue
                            }
                            break
                        }
                    }
                }
            }
        })

        val layoutConstraints = GridBagConstraints()
        layoutConstraints.fill = GridBagConstraints.HORIZONTAL
        layoutConstraints.gridy = 0
        layoutConstraints.weightx = 1.0
        layoutConstraints.gridx = 0
        val header = contentTable.tableHeader
        header.preferredSize = Dimension(contentTable.width, 25)
        contentPanel.add(header, layoutConstraints)
        layoutConstraints.gridy++
        contentPanel.add(contentTable, layoutConstraints)
        layout = BorderLayout()
        add(titlePanel, BorderLayout.NORTH)
        add(contentPanel, BorderLayout.CENTER)
        setCollapsed(false)
    }

    private fun setCollapsed(collapsed: Boolean) {
        this.collapsed = collapsed
        this.collapsedJButton.icon = if (this.collapsed) AllIcons.General.ArrowRight else AllIcons.General.ArrowDown
        this.contentPanel.isVisible = !this.collapsed
        revalidate()
        repaint()
    }

    override fun actionPerformed(e: ActionEvent?) {
        setCollapsed(!this.collapsed)
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
