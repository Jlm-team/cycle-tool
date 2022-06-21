package team.jlm.utils.gittools.tools

import com.github.difflib.DiffUtils
import com.github.difflib.patch.DeltaType.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.Change
import com.intellij.vcs.log.TimedVcsCommit
import git4idea.repo.GitRepository
import team.jlm.utils.diff
import team.jlm.utils.filterOnlyJavaSrc
import team.jlm.utils.gittools.entity.JavaFile
import team.jlm.utils.gittools.entity.JavaText

fun getDiffBetweenCommit(
    oldCommit: TimedVcsCommit,
    newCommit: TimedVcsCommit,
    repo: GitRepository,
    project: Project
): ArrayList<JavaFile> {

    val res = ArrayList<JavaFile>()
    val diff = filterOnlyJavaSrc(repo.diff(oldCommit, newCommit))

    for (i in diff) {
        if (i.type == Change.Type.MODIFICATION) {

            val newJavaFileText = i.afterRevision?.content
            val oldJavaFileText = i.beforeRevision?.content

            val filteredNewText = JavaText(replaceNote(newJavaFileText), project)
            val filteredOldText = JavaText(replaceNote(oldJavaFileText), project)

            val diffBetweenText = DiffUtils.diff(filteredOldText.Lines, filteredNewText.Lines)

            for (diffPart in diffBetweenText.deltas) {
                when (diffPart.type!!) {
                    CHANGE -> {
                        filteredOldText.sloveElement(getTextBlock(diffPart.source.lines),CHANGE)
                        filteredNewText.sloveElement(getTextBlock(diffPart.target.lines),CHANGE)
                    }
                    DELETE -> {
                        filteredOldText.sloveElement(getTextBlock(diffPart.source.lines),DELETE)
                    }
                    INSERT -> {
                        filteredNewText.sloveElement(getTextBlock(diffPart.target.lines),INSERT)
                    }
                    EQUAL -> {
                        continue
                    }
                }
            }
            res.add(
                JavaFile(
                    filteredOldText,
                    filteredNewText,
                    i.afterRevision!!.file.path.replace(project.basePath!!, "")
                )
            )
        }

    }
    return res
}