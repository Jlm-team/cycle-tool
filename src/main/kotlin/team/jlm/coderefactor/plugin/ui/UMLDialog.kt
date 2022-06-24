package team.jlm.coderefactor.plugin.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import team.jlm.utils.uml.UMLGraphXAdapter
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class UMLDialog(project: Project?) : DialogWrapper(project) {

    var umlGraph: UMLGraphXAdapter? = null

    init {
        title = "依赖图"
        init()
    }


    override fun createCenterPanel(): JComponent {
        if (umlGraph == null) {
            val dialogPanel = JPanel(BorderLayout())
            val label = JLabel("暂无UML图可以显示")
            label.preferredSize = Dimension(100, 100)
            label.icon = AllIcons.General.Error
            dialogPanel.add(label, BorderLayout.CENTER)
            return dialogPanel
        }
        else{
            return umlGraph!!
        }
    }

}