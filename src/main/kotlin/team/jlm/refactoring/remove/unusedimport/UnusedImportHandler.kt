package team.jlm.refactoring.remove.unusedimport

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.project.Project
import mu.KotlinLogging
import team.jlm.utils.psi.getAllJavaFilesInProject
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

fun removeUnusedImport(project: Project): ConcurrentHashMap<String, Int> {
    val javaFileList = getAllJavaFilesInProject(project).toTypedArray()
    val process = RemoveImportsProcessor(project, javaFileList)
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
    return process.map
}

