package team.jlm.coderefactor.plugin.ui.toolwindow

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import team.jlm.coderefactor.plugin.ui.CollapsiblePanel
import team.jlm.refactoring.DeprecatedMethod
import java.awt.*
import java.awt.event.MouseEvent
import javax.swing.*


object DeprecatedMethodWindow {

    fun getWindow(content: HashMap<String, ArrayList<DeprecatedMethod>>): JPanel {
        val layoutConstraints = GridBagConstraints()
        layoutConstraints.fill = GridBagConstraints.HORIZONTAL
        layoutConstraints.weightx = 1.0
        layoutConstraints.gridx = 0
        layoutConstraints.gridy = 0
        val panel = JPanel(GridBagLayout())

        val column = arrayOf("调用类", "调用位置", "类型", "被调用类", "被调用方法", "相对位置", "")
        content.forEach { (k, v) ->
            val data = v.map {
                arrayOf(
                    it.containingClass,
                    it.methodName,
                    it.type,
                    it.deprecatedCallContainingClass,
                    it.deprecatedCallContainingMethod,
                    it.lineNumber,
                    it.fileUrl
                )
            }.toTypedArray()
            val contentPanel =
                CollapsiblePanel(k, data, column, true, column.lastIndex) { contentTable: JBTable, e: MouseEvent? ->
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
            panel.add(contentPanel, layoutConstraints)
            layoutConstraints.gridy++
        }
        val scrollPane = JPanel(BorderLayout())
        scrollPane.add(JBScrollPane(panel), BorderLayout.NORTH)
        return scrollPane
    }


}

