package team.jlm.coderefactor.plugin.service

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.annotations.Property

@State(name = "CommitsAnalyseCacheService", storages = [Storage(StoragePathMacros.WORKSPACE_FILE)])
@Storage(StoragePathMacros.WORKSPACE_FILE)
class CommitsAnalyseCacheService :
    SimplePersistentStateComponent<CommitsAnalyseCacheService.State>(State(HashSet())) {
    data class State @JvmOverloads
    constructor(@Property @JvmField val analysedCommits: HashSet<String>? = HashSet()) : BaseState()

    companion object {
        @JvmStatic
        fun getInstance(project: Project): CommitsAnalyseCacheService {
            return project.service()
        }
    }
}