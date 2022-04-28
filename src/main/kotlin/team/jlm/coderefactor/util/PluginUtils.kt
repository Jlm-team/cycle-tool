package team.jlm.coderefactor.util

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.wm.WindowManager
import java.awt.Window

fun getActiveProject(): Project? {
    val projects: Array<Project> = ProjectManager.getInstance().openProjects
    var activeProject: Project? = null
    for (project in projects) {
        val window: Window? = WindowManager.getInstance().suggestParentWindow(project)
        if (window != null && window.isActive) {
            activeProject = project
        }
    }
    return activeProject
}
