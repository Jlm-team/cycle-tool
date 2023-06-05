package team.jlm.coderefactor.plugin.listener

import com.intellij.openapi.vfs.VirtualFileEvent
import com.intellij.openapi.vfs.VirtualFileListener
import com.intellij.openapi.vfs.VirtualFilePropertyEvent

class FileChangeListener : VirtualFileListener {

    companion object {
        @JvmStatic
        var anyFileChanged: Boolean = false
            private set

        @JvmStatic
        fun reAnalyseDependency() {
            anyFileChanged = false
        }
    }

    override fun propertyChanged(event: VirtualFilePropertyEvent) {
        anyFileChanged = true
    }

    override fun contentsChanged(event: VirtualFileEvent) {
        anyFileChanged = true
    }
}