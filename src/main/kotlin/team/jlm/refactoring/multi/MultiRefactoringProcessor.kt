package team.jlm.refactoring.multi

import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.command.UndoConfirmationPolicy
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.refactoring.BaseRefactoringProcessor
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.listeners.RefactoringListenerManager
import com.intellij.refactoring.listeners.impl.RefactoringListenerManagerImpl
import com.intellij.usageView.UsageInfo
import com.intellij.usageView.UsageViewBundle
import com.intellij.usageView.UsageViewDescriptor
import mu.KotlinLogging
import team.jlm.refactoring.IRefactoringProcessor
import team.jlm.refactoring.interceptor.RefactoringProcessorInterceptor
import team.jlm.refactoring.interceptor.RefactoringProcessorInterceptor.Companion.findUsagesMethod
import team.jlm.refactoring.interceptor.RefactoringProcessorInterceptor.Companion.getCommandNameMethod
import team.jlm.refactoring.interceptor.RefactoringProcessorInterceptor.Companion.getUndoConfirmationPolicyMethod
import team.jlm.refactoring.interceptor.RefactoringProcessorInterceptor.Companion.myProjectField
import team.jlm.refactoring.interceptor.RefactoringProcessorInterceptor.Companion.refreshElementsMethod

private val logger = KotlinLogging.logger {}

class MultiRefactoringProcessor(
    project: Project,
    private val processors: List<IRefactoringProcessor>,
    refactoringScope: SearchScope = GlobalSearchScope.projectScope(project),
    prepareSuccessfulCallback: Runnable? = null,
    private val processedElementsHeader: String = "MultiRefactoring",
    private val commandName: String,
) : BaseRefactoringProcessor(project, refactoringScope, prepareSuccessfulCallback),
    IRefactoringProcessor {
    override var callFromMulti: Boolean = false
    override fun isPreviewUsages(): Boolean {
        return super.isPreviewUsages()
    }

    override fun isPreviewUsages(usages: Array<out UsageInfo>): Boolean {
        return super.isPreviewUsages(usages)
    }

    lateinit var myTransactionWrapper: RefactoringTransactionWrapper

    override fun refreshElements(elements: Array<out PsiElement>) {
        logger.trace { "refreshElements" }
        if (usagesIndexList.size != processors.size) return
        for ((i, processor) in processors.withIndex()) {
            processor.refreshElements(elements.copyOfRange(usagesIndexList[i], usagesIndexList[i + 1]))
        }
    }

    override var myPrepareSuccessfulSwingThreadCallback: Runnable? = prepareSuccessfulCallback

    override fun createUsageViewDescriptor(): UsageViewDescriptor {
        logger.trace { "createUsageViewDescriptor" }
        return object : UsageViewDescriptor {
            override fun getElements(): Array<PsiElement> {
                return ArrayList<PsiElement>().apply {
                    for (processor in processors) {
                        addAll(processor.createUsageViewDescriptor().elements)
                    }
                }.toTypedArray()
            }

            override fun getProcessedElementsHeader(): String {
                return this@MultiRefactoringProcessor.processedElementsHeader
            }

            override fun getCodeReferencesText(usagesCount: Int, filesCount: Int): String {
                return RefactoringBundle.message(
                    "references.to.be.changed",
                    UsageViewBundle.getReferencesString(usagesCount, filesCount)
                )
            }
        }
    }

    private val usagesIndexList = ArrayList<Int>(processors.size + 1)
    override fun createUsageViewDescriptor(usages: Array<out UsageInfo>): UsageViewDescriptor {
        return processors[0].createUsageViewDescriptor()
    }

    override fun findUsages(): Array<out UsageInfo> {
        logger.trace { "findUsages" }
        var count = 0
        usagesIndexList.clear()
        usagesIndexList.add(0)
        return ArrayList<UsageInfo>().apply {
            for (processor in processors) {
                val processorUsages = findUsagesMethod.invoke(processor) as Array<UsageInfo>
                addAll(processorUsages)
                count += processorUsages.size
                usagesIndexList.add(count)
            }
        }.toTypedArray()
    }

    override fun performRefactoring(usages: Array<out UsageInfo>) {
        logger.trace { "performRefactoring" }
    }

    override fun preprocessUsages(): Boolean {
        logger.trace { "preprocessUsages" }
        return processors.all { it.preprocessUsages() }
    }

    override fun execute(usages: Array<out UsageInfo>) {
        logger.trace { "execute" }
        val listenerManager = RefactoringListenerManager.getInstance(myProject) as RefactoringListenerManagerImpl
        if (!callFromMulti) {
            PsiDocumentManager.getInstance(RefactoringProcessorInterceptor.myProjectField.get(this) as Project)
                .commitAllDocuments()
            val refactoringTransaction = listenerManager.startTransaction()
            myTransactionWrapper = RefactoringTransactionWrapper(refactoringTransaction)
            for (processor in processors) {
                RefactoringProcessorInterceptor.myTransactionField.set(processor, myTransactionWrapper)
                processor.callFromMulti = true
            }
            CommandProcessor.getInstance().executeCommand(
                myProjectField.get(this) as Project, {
                    executeProcessors()
                }, getCommandNameMethod.invoke(this) as String,
                null, getUndoConfirmationPolicyMethod.invoke(this) as UndoConfirmationPolicy
            )
            myTransactionWrapper.finalCommit()
        } else {
            executeProcessors()
        }
    }

    private fun executeProcessors() {
        for (processor in processors) {
            val elements = processor.createUsageViewDescriptor().elements
            if (!callFromMulti) {
                for (i in elements.indices) {
                    elements[i] = myTransactionWrapper.findNew(elements[i])
                }
            }
            refreshElementsMethod.invoke(processor, elements)
            processor.run()
        }
    }

    override fun getCommandName(): String {
        logger.trace { "getCommandName" }
        return commandName
    }

    override fun preprocessUsages(refUsage: Ref<Array<out UsageInfo>>): Boolean {
        return super<BaseRefactoringProcessor>.preprocessUsages(refUsage)
    }
}
