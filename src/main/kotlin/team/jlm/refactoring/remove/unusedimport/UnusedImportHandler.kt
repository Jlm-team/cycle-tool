package team.jlm.refactoring.remove.unusedimport

import com.intellij.codeInsight.actions.OptimizeImportsProcessor
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.project.Project
import team.jlm.utils.psi.getAllJavaFilesInProject


fun removeUnusedImport(project: Project) {
    val javaFileList = getAllJavaFilesInProject(project).toTypedArray()
    val process = OptimizeImportsProcessor(project, javaFileList, null)
    process.run()
    val message = buildString {
        append("已移除未使用Import")
    }
    val notification = Notification(
        "team.jlm.plugin.notification",
        message,
        NotificationType.INFORMATION
    )
    Notifications.Bus.notify(notification)
}

