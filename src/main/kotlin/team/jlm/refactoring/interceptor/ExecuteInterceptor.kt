package team.jlm.refactoring.interceptor

import com.intellij.history.LocalHistory
import com.intellij.history.LocalHistoryAction
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.application.ex.ApplicationManagerEx
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.command.UndoConfirmationPolicy
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.wm.impl.status.StatusBarUtil
import com.intellij.psi.PsiDocumentManager
import com.intellij.refactoring.BaseRefactoringProcessor
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.RefactoringHelper
import com.intellij.refactoring.listeners.RefactoringListenerManager
import com.intellij.refactoring.listeners.impl.RefactoringListenerManagerImpl
import com.intellij.refactoring.listeners.impl.RefactoringTransaction
import com.intellij.refactoring.suggested.SuggestedRefactoringProvider
import com.intellij.usageView.UsageInfo
import com.intellij.util.containers.toArray
import net.bytebuddy.implementation.bind.annotation.Argument
import net.bytebuddy.implementation.bind.annotation.RuntimeType
import net.bytebuddy.implementation.bind.annotation.This
import team.jlm.refactoring.IRefactoringProcessor
import team.jlm.refactoring.interceptor.RefactoringProcessorInterceptor.Companion.callPerformRefactoringMethod
import team.jlm.refactoring.interceptor.RefactoringProcessorInterceptor.Companion.canPerformRefactoringInBranchMethod
import team.jlm.refactoring.interceptor.RefactoringProcessorInterceptor.Companion.doRefactoringMethod
import team.jlm.refactoring.interceptor.RefactoringProcessorInterceptor.Companion.getCommandNameMethod
import team.jlm.refactoring.interceptor.RefactoringProcessorInterceptor.Companion.getUndoConfirmationPolicyMethod
import team.jlm.refactoring.interceptor.RefactoringProcessorInterceptor.Companion.myProjectField
import team.jlm.refactoring.multi.ICallFromMulti

class ExecuteInterceptor {
    companion object {
        fun doRefactor(obj: BaseRefactoringProcessor, usageInfoSet: MutableCollection<UsageInfo>) {
            obj.apply {
                val isIRefactoringProcessor = this is IRefactoringProcessor
                val iterator = usageInfoSet.iterator()
                while (iterator.hasNext()) {
                    val usageInfo = iterator.next()
                    val element = usageInfo.element
                    if (element == null || !(RefactoringProcessorInterceptor.isToBeChangedMethod.invoke(
                            this,
                            usageInfo
                        ) as Boolean)
                    ) {
                        iterator.remove()
                    }
                }
                val commandName = getCommandNameMethod.invoke(this) as String
                var action: LocalHistoryAction? = null
                if (!isIRefactoringProcessor || !(this as IRefactoringProcessor).callFromMulti) {
                    action = LocalHistory.getInstance().startAction(commandName)
                }
                val writableUsageInfos: Array<UsageInfo> = usageInfoSet.toArray<UsageInfo>(UsageInfo.EMPTY_ARRAY)
                try {

                    if (!isIRefactoringProcessor || !(this as IRefactoringProcessor).callFromMulti) {
                        PsiDocumentManager.getInstance(myProjectField.get(this) as Project)
                            .commitAllDocuments()
                        val listenerManager = RefactoringListenerManager.getInstance(
                            myProjectField.get(this) as Project
                        ) as RefactoringListenerManagerImpl
                        RefactoringProcessorInterceptor.myTransactionField.set(this, listenerManager.startTransaction())
                    }

                    val preparedData: MutableMap<RefactoringHelper<Any>, Any?> = LinkedHashMap()
                    val prepareHelpersRunnable = Runnable {
                        for (helper in RefactoringHelper.EP_NAME.extensionList) {
                            val operation =
                                ReadAction.compute<Any, RuntimeException> {
                                    helper.prepareOperation(
                                        writableUsageInfos
                                    )
                                }
                            preparedData[helper] = operation
                        }
                    }
                    ProgressManager.getInstance().runProcessWithProgressSynchronously(
                        prepareHelpersRunnable,
                        RefactoringBundle.message("refactoring.prepare.progress"),
                        false, myProjectField.get(this) as Project
                    )
                    val app = ApplicationManagerEx.getApplicationEx()
                    val inBranch = Registry.`is`("run.refactorings.in.model.branch")
                            && canPerformRefactoringInBranchMethod.invoke(this) as Boolean
                    if (inBranch) {
                        callPerformRefactoringMethod.invoke(
                            this,
                            writableUsageInfos,
                            Runnable {
                                RefactoringProcessorInterceptor.performInBranchMethod.invoke(this, writableUsageInfos)
                            })
                    } else if (Registry.`is`("run.refactorings.under.progress")) {
                        app.runWriteActionWithNonCancellableProgressInDispatchThread(
                            commandName, myProjectField.get(this) as Project, null
                        ) { _: ProgressIndicator? ->
                            callPerformRefactoringMethod.invoke(this,
                                writableUsageInfos,
                                Runnable {
                                    RefactoringProcessorInterceptor.performRefactoringMethod.invoke(
                                        this,
                                        writableUsageInfos
                                    )
                                })
                        }
                    } else {
                        app.runWriteAction {
                            callPerformRefactoringMethod.invoke(
                                this,
                                writableUsageInfos,
                                Runnable {
                                    RefactoringProcessorInterceptor.performRefactoringMethod.invoke(
                                        this,
                                        writableUsageInfos
                                    )
                                })
                        }
                    }
                    DumbService.getInstance(myProjectField.get(this) as Project)
                        .completeJustSubmittedTasks()
                    for ((key, value) in preparedData) {
                        key.performOperation(myProjectField.get(this) as Project, value)
                    }

                    if (!isIRefactoringProcessor || !(this as IRefactoringProcessor).callFromMulti) {
                        (RefactoringProcessorInterceptor.myTransactionField.get(this) as RefactoringTransaction).commit()
                    }

                    if (!inBranch) {
                        if (Registry.`is`("run.refactorings.under.progress")) {
                            app.runWriteActionWithNonCancellableProgressInDispatchThread(
                                commandName, myProjectField.get(this) as Project, null
                            ) { _: ProgressIndicator? ->
                                RefactoringProcessorInterceptor.performPsiSpoilingRefactoringMethod.invoke(
                                    this
                                )
                            }
                        } else {
                            app.runWriteAction {
                                RefactoringProcessorInterceptor.performPsiSpoilingRefactoringMethod.invoke(
                                    this
                                )
                            }
                        }
                    }
                } finally {
                    if (!isIRefactoringProcessor || !(this as IRefactoringProcessor).callFromMulti) {
                        action!!.finish()
                    }
                }

                val count = writableUsageInfos.size
                if (count > 0) {
                    StatusBarUtil.setStatusBarInfo(
                        myProjectField.get(this) as Project,
                        RefactoringBundle.message("statusBar.refactoring.result", count)
                    )
                } else {
                    if (!(RefactoringProcessorInterceptor.isPreviewUsagesMethod.invoke(
                            this,
                            writableUsageInfos
                        ) as Boolean)
                    ) {
                        StatusBarUtil.setStatusBarInfo(
                            myProjectField.get(this) as Project,
                            RefactoringBundle.message("statusBar.noUsages")
                        )
                    }
                }
            }
        }

        @JvmStatic
        fun executeInterceptor(@This @RuntimeType obj: Any, @Argument(0) usages: Array<out UsageInfo>) {
            obj as BaseRefactoringProcessor
            val usageInfos: MutableCollection<UsageInfo> = LinkedHashSet(listOf(*usages))
            if (obj is ICallFromMulti && obj.callFromMulti) {
                doRefactor(obj, usageInfos)
            } else {
                CommandProcessor.getInstance().executeCommand(
                    myProjectField.get(obj) as Project, {
                        doRefactoringMethod.invoke(obj, usageInfos)
                        if (RefactoringProcessorInterceptor.isGlobalUndoActionMethod.invoke(obj) as Boolean) CommandProcessor.getInstance()
                            .markCurrentCommandAsGlobal(myProjectField.get(obj) as Project)
                        SuggestedRefactoringProvider.getInstance(myProjectField.get(obj) as Project).reset()
                    }, getCommandNameMethod.invoke(obj) as String,
                    null, getUndoConfirmationPolicyMethod.invoke(obj) as UndoConfirmationPolicy
                )
            }
        }
    }
}
