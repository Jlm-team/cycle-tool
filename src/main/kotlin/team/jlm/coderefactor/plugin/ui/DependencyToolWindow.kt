package team.jlm.coderefactor.plugin.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.impl.ActionButtonWithText
import com.intellij.refactoring.Refactoring
import com.xyzboom.algorithm.graph.GEdge
import team.jlm.coderefactor.plugin.action.CycleDependencyAction
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.JButton
import javax.swing.JPanel

class DependencyToolWindow {

    fun getWindow(): JPanel {
        return getWindow(ArrayList())
    }

    fun getWindow(values: List<Pair<GEdge<String>, Refactoring>>): JPanel {
        var flag = false
        if (values.isEmpty()) {
            flag = true
        }
        val panel = JPanel(BorderLayout())
        val table = DependencyTable(values)
        val buttons = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0))
        val doneButton = JButton("确定")
        doneButton.addActionListener {
            val refactors = table.selectedRows.map { s -> table.refactorsMap[s] }
            refactors.forEach { it.second.run() }
        }
        val selectAllButton = JButton("全选")
        selectAllButton.addActionListener { table.selectAll() }
        val presentation = Presentation("刷新")
        presentation.icon = AllIcons.Actions.Refresh
        val refreshButton = ActionButtonWithText(
            CycleDependencyAction(),
            presentation,
            ActionPlaces.TOOLWINDOW_TOOLBAR_BAR,
            Dimension(32, 32)
        )
        if (flag) {
            doneButton.isEnabled = false
            selectAllButton.isEnabled = false
        }
        buttons.add(doneButton)
        buttons.add(selectAllButton)
        buttons.add(refreshButton)
        panel.add(buttons, BorderLayout.NORTH)
        panel.add(table, BorderLayout.CENTER)
        return panel
    }
}