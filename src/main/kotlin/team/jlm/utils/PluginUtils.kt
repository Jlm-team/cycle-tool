package team.jlm.utils

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.NlsContexts.DialogTitle
import com.intellij.openapi.wm.WindowManager
import com.intellij.util.ThrowableConvertor
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

@Throws(Exception::class)
fun <T> computeWithModalProgress(
    project: Project?,
    title: @DialogTitle String,
    computable: ThrowableConvertor<in ProgressIndicator?, T, out Exception>
): T {
    return ProgressManager.getInstance().run(object : Task.WithResult<T, Exception?>(project, title, true) {
        @Throws(Exception::class)
        override fun compute(indicator: ProgressIndicator): T {
            return computable.convert(indicator)
        }
    })
}
