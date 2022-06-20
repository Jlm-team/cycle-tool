package team.jlm.utils.gittools.tools

import com.github.difflib.DiffUtils
import com.github.difflib.patch.DeltaType.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.Change
import com.intellij.psi.PsiElement
import com.intellij.vcs.log.TimedVcsCommit
import git4idea.repo.GitRepository
import team.jlm.utils.diff
import team.jlm.utils.filterOnlyJavaSrc
import team.jlm.utils.gittools.entity.JavaText

fun getDiffBetweenCommit(oldCommit: TimedVcsCommit, newCommit: TimedVcsCommit, repo: GitRepository, project: Project) {
    val diff = filterOnlyJavaSrc(repo.diff(oldCommit, newCommit))
    for (i in diff) {
        if (i.type == Change.Type.MODIFICATION) {

            val newJavaFileText = i.afterRevision?.content
            val oldJavaFileText = i.beforeRevision?.content

            val filteredNewText = JavaText(replaceNote(newJavaFileText),project)
            val filteredOldText = JavaText(replaceNote(oldJavaFileText),project)

            val diffBetweenText = DiffUtils.diff(filteredOldText.Lines,filteredNewText.Lines)

            for(diffPart in diffBetweenText.deltas){
                when(diffPart.type){
                    CHANGE -> {
                        for(d in diffPart.source.lines){
                          filteredOldText.sloveElement(d,CHANGE)
                        }
                        for(t in diffPart.target.lines){
                            filteredNewText.sloveElement(t,CHANGE)
                        }
                    }
                    DELETE -> {
                        for(d in diffPart.source.lines){
                            filteredOldText.sloveElement(d,DELETE)
                        }
                    }
                    INSERT -> {
                        for(t in diffPart.target.lines){
                            filteredNewText.sloveElement(t,INSERT)
                        }
                    }
                    EQUAL -> {
                        continue
                    }
                }
            }

        }

    }
}