package team.jlm.coderefactor.plugin.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
class DependencyToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = DependencyToolWindow.getWindow()
        val contentFactory = ContentFactory.SERVICE.getInstance()
        val content = contentFactory.createContent(panel,"重构",false)
        toolWindow.contentManager.addContent(content)
    }
}