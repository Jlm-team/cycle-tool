package team.jlm.utils.file

import com.intellij.ide.projectView.actions.MarkRootActionBase
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.util.Comparing
import com.intellij.openapi.vfs.VirtualFile
import java.io.File

fun getFileSeparator(): String {
    return System.getProperty("file.separator")
}

fun getPluginFoldrPath(projectBase: String): String {
    return "$projectBase${getFileSeparator()}.hya"
}

fun pluginBaseFoldrExist(projectBasePath: String): Boolean {
    val file = File("$projectBasePath${getFileSeparator()}.hya")
    if (!file.exists() && !file.isDirectory) {
        file.mkdir()
        return false
    }
    return true
}

fun tryCreatePluginBaseFolder(project: Project) {
    project.basePath?.let { pluginBaseFoldrExist(it) }
}

fun getSavePath(className: String?, pathSuffix: String, projectBasePath: String, fileName: String?): String {
    val fileSeparator = getFileSeparator()
    return if (className != null) {
        projectBasePath + fileSeparator + ".hya" + fileSeparator + pathSuffix + fileSeparator +
                className.replace(".", fileSeparator) + ".json"
    } else {
        "$projectBasePath$fileSeparator.hya$fileSeparator$pathSuffix$fileName"
    }

}

fun excludePluginBaseFolder(module: Module, files: Array<VirtualFile>, folderPath: String) {
    val model = ModuleRootManager.getInstance(module).modifiableModel
    for (file in files) {
        val entry = MarkRootActionBase.findContentEntry(model, file)
        if (entry != null) {
            val sourceFolders = entry.sourceFolders
            for (sourceFolder in sourceFolders) {
                if (Comparing.equal(sourceFolder.file, file)) {
                    entry.removeSourceFolder(sourceFolder)
                    break
                }
            }
            entry.addExcludeFolder(folderPath)
        }
    }
    ApplicationManager.getApplication().runWriteAction {
        model.commit()
        module.project.save()
    }
}