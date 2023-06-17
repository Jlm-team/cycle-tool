package team.jlm.coderefactor.plugin.ui

import com.intellij.icons.AllIcons
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableColumnModel
import javax.swing.table.TableColumn

class CollapsiblePanel(
    title: String,
    data: Array<Array<String?>>,
    column: Array<String>,
    needHidden: Boolean,
    hiddenColumnIndex: Int,
    clickAction:((JBTable,MouseEvent?)->Unit)?
) : JPanel(),
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

        val tableModel = SampleTableModel(data, column)

        val columnModel = DefaultTableColumnModel()
        for (i in column.indices) {
            val c = TableColumn(i)
            c.headerValue = column[i]
            columnModel.addColumn(c)
        }
        if (needHidden) {
            columnModel.getColumn(hiddenColumnIndex).width = 0
            columnModel.getColumn(hiddenColumnIndex).cellRenderer = object : DefaultTableCellRenderer() {
                override fun setValue(value: Any?) {

                }
            }
            columnModel.getColumn(hiddenColumnIndex).width = 0
            columnModel.getColumn(hiddenColumnIndex).maxWidth = 0
            columnModel.getColumn(hiddenColumnIndex).minWidth = 0
            columnModel.getColumn(hiddenColumnIndex).resizable = false
        }

        val contentTable = JBTable(tableModel, columnModel)
        contentTable.tableHeader.preferredSize = Dimension(contentTable.width, 20)
        contentTable.autoResizeMode = JTable.AUTO_RESIZE_ALL_COLUMNS

        contentTable.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                clickAction?.let { it(contentTable,e) }
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

}
