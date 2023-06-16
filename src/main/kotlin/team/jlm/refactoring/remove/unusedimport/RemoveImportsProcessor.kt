package team.jlm.refactoring.remove.unusedimport

import com.google.common.collect.HashMultiset
import com.google.common.collect.Multiset
import com.intellij.codeInsight.CodeInsightBundle
import com.intellij.codeInsight.actions.AbstractLayoutCodeProcessor
import com.intellij.codeInsight.daemon.impl.ShowAutoImportPass
import com.intellij.java.JavaBundle
import com.intellij.lang.ImportOptimizer.CollectingInfoRunnable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.EmptyRunnable
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.util.NlsContexts.HintText
import com.intellij.openapi.util.NlsContexts.ProgressText
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import com.intellij.psi.impl.source.codeStyle.CoreCodeStyleUtil
import com.intellij.util.IncorrectOperationException
import com.intellij.util.SlowOperations
import com.intellij.util.SmartList
import mu.KotlinLogging
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.FutureTask

private val logger = KotlinLogging.logger {}

@Suppress("UnstableApiUsage")
class RemoveImportsProcessor(
    private val project: Project,
    javaPsiFiles: Array<out PsiFile>,
) : AbstractLayoutCodeProcessor(
    project, javaPsiFiles, getProgressText(), getCommandName(),
    null, false
) {
    private val myOptimizerNotifications: MutableList<NotificationInfo> = SmartList()
    val map = ConcurrentHashMap<String, Int>(javaPsiFiles.size)
    override fun prepareTask(file: PsiFile, processChangedTextOnly: Boolean): FutureTask<Boolean> {
        ApplicationManager.getApplication().assertReadAccessAllowed()
        if (DumbService.isDumb(file.project)) {
            return emptyTask()
        }
        val runnables = collectOptimizers(project, file, map)

        if (runnables.isEmpty()) {
            return emptyTask()
        }
        val hints =
            if (ApplicationManager.getApplication().isDispatchThread) emptyList() else ShowAutoImportPass.getImportHints(
                file
            )
        return FutureTask<Boolean>({
            ApplicationManager.getApplication().assertIsDispatchThread()
            CoreCodeStyleUtil.setSequentialProcessingAllowed(false)
            try {
                for (runnable in runnables) {
                    runnable.run()
                    myOptimizerNotifications.add(getNotificationInfo(runnable))
                }
                putNotificationInfoIntoCollector()
                ShowAutoImportPass.fixAllImportsSilently(file, hints)
            } finally {
                CoreCodeStyleUtil.setSequentialProcessingAllowed(true)
            }
        }, true)
    }

    private fun putNotificationInfoIntoCollector() {
        val collector = infoCollector ?: return
        var atLeastOneOptimizerChangedSomething = false
        for (info in myOptimizerNotifications) {
            atLeastOneOptimizerChangedSomething = atLeastOneOptimizerChangedSomething or info.isSomethingChanged
            if (info.message != null) {
                collector.optimizeImportsNotification = info.message
                return
            }
        }
        val hint =
            if (atLeastOneOptimizerChangedSomething) CodeInsightBundle.message("hint.text.imports.optimized") else null
        collector.optimizeImportsNotification = hint
    }

    internal class NotificationInfo private constructor(
        val isSomethingChanged: Boolean,
        val message: @HintText String?,
    ) {

        constructor(message: @HintText String) : this(true, message) {}

        companion object {
            val NOTHING_CHANGED_NOTIFICATION = NotificationInfo(false, null)
            val SOMETHING_CHANGED_WITHOUT_MESSAGE_NOTIFICATION = NotificationInfo(true, null)
        }
    }

    companion object {
        private fun emptyTask(): FutureTask<Boolean> {
            return FutureTask(EmptyRunnable.INSTANCE, true)
        }

        private fun collectOptimizers(project: Project, file: PsiFile, map: MutableMap<String, Int>): List<Runnable> {
            val runnables: MutableList<Runnable> = ArrayList()
            val files = file.viewProvider.allFiles
            for (psiFile in files) {
                runnables.add(processFile(project, psiFile, map))
            }
            return runnables
        }

        private fun getNotificationInfo(runnable: Runnable): NotificationInfo {
            if (runnable is CollectingInfoRunnable) {
                val optimizerMessage = runnable.userNotificationInfo
                return if (optimizerMessage == null) NotificationInfo.NOTHING_CHANGED_NOTIFICATION else NotificationInfo(
                    optimizerMessage
                )
            }
            return if (runnable === EmptyRunnable.getInstance()) {
                NotificationInfo.NOTHING_CHANGED_NOTIFICATION
            } else NotificationInfo.SOMETHING_CHANGED_WITHOUT_MESSAGE_NOTIFICATION
        }

        private fun getProgressText(): @ProgressText String {
            return CodeInsightBundle.message("progress.text.optimizing.imports")
        }

        private fun getCommandName(): @NlsContexts.Command String {
            return CodeInsightBundle.message("process.optimize.imports")
        }

        private fun processFile(project: Project, file: PsiFile, map: MutableMap<String, Int>): Runnable {
            if (file !is PsiJavaFile) {
                return EmptyRunnable.getInstance()
            }
            val newImportList = JavaCodeStyleManager.getInstance(project).prepareOptimizeImportsResult(
                file
            ) ?: return EmptyRunnable.getInstance()
            return object : CollectingInfoRunnable {
                private var myImportsAdded = 0
                private var myImportsRemoved = 0
                override fun run() {
                    SlowOperations.allowSlowOperations<RuntimeException> { doRun() }
                }

                private fun doRun() {
                    try {
                        val manager = PsiDocumentManager.getInstance(file.getProject())
                        val document = manager.getDocument(file)
                        if (document != null) {
                            manager.commitDocument(document)
                        }
                        val oldImportList = file.importList!!
                        val oldImports: Multiset<PsiElement?> = HashMultiset.create()
                        for (statement in oldImportList.importStatements) {
                            statement.resolve()?.let { oldImports.add(it) }
                        }
                        val oldStaticImports: Multiset<PsiElement?> = HashMultiset.create()
                        for (statement in oldImportList.importStaticStatements) {
                            statement.resolve()?.let { oldStaticImports.add(it) }
                        }
                        oldImportList.replace(newImportList)
                        for (statement in newImportList.importStatements) {
                            if (statement.resolve()?.let { oldImports.remove(it) } == true) {
                                myImportsAdded++
                            }
                        }
                        myImportsRemoved += oldImports.size
                        for (statement in newImportList.importStaticStatements) {
                            if (!oldStaticImports.remove(statement.resolve()!!)) {
                                myImportsAdded++
                            }
                        }
                        myImportsRemoved += oldStaticImports.size
                        map["${file.packageName}/${file.name}"] = myImportsRemoved
                    } catch (e: IncorrectOperationException) {
                        logger.error(e) { }
                    }
                }

                override fun getUserNotificationInfo(): String {
                    if (myImportsRemoved == 0) {
                        return JavaBundle.message("hint.text.rearranged.imports")
                    }
                    var notification = JavaBundle.message(
                        "hint.text.removed.imports",
                        myImportsRemoved,
                        if (myImportsRemoved == 1) 0 else 1
                    )
                    if (myImportsAdded > 0) {
                        notification += JavaBundle.message(
                            "hint.text.added.imports",
                            myImportsAdded,
                            if (myImportsAdded == 1) 0 else 1
                        )
                    }
                    return notification
                }
            }
        }
    }
}