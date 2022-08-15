package team.jlm.coderefactor.plugin.listener

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener
import team.jlm.utils.file.tryCreatePluginBaseFolder

class ProjectOpenCloseListener : ProjectManagerListener {
    override fun projectOpened(project: Project) {
        tryCreatePluginBaseFolder(project)
    }
}