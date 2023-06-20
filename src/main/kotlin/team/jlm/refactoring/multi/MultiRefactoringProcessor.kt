package team.jlm.refactoring.multi

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.listeners.RefactoringListenerManager
import com.intellij.refactoring.listeners.impl.RefactoringListenerManagerImpl
import com.intellij.usageView.UsageInfo
import com.intellij.usageView.UsageViewBundle
import com.intellij.usageView.UsageViewDescriptor
import mu.KotlinLogging
import team.jlm.refactoring.BaseRefactoringProcessor

private val logger = KotlinLogging.logger {}

class MultiRefactoringProcessor(
    project: Project,
    private val processors: List<BaseRefactoringProcessor>,
    refactoringScope: SearchScope = GlobalSearchScope.projectScope(project),
    prepareSuccessfulCallback: Runnable? = null,
    private val processedElementsHeader: String = "MultiRefactoring",
    private val commandName: String,
) : BaseRefactoringProcessor(project, refactoringScope, prepareSuccessfulCallback) {

    lateinit var myTransactionWrapper: RefactoringTransactionWrapper

    override fun refreshElements(elements: Array<out PsiElement>) {
        logger.trace { "refreshElements" }
        if (usagesIndexList.size != processors.size) return
        for ((i, processor) in processors.withIndex()) {
            processor.refreshElements(elements.copyOfRange(usagesIndexList[i], usagesIndexList[i + 1]))
        }
    }

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

    override fun findUsages(): Array<out UsageInfo> {
        logger.trace { "findUsages" }
        var count = 0
        usagesIndexList.clear()
        usagesIndexList.add(0)
        return ArrayList<UsageInfo>().apply {
            for (processor in processors) {
                val processorUsages = processor.findUsages()
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
        transaction = listenerManager.startTransaction()
        myTransactionWrapper = RefactoringTransactionWrapper(transaction)
        for (processor in processors) {
            processor.transaction = myTransactionWrapper
            processor.transactionSetter = {}
        }
        for (processor in processors) {
            val elements = processor.createUsageViewDescriptor().elements
            for (i in elements.indices) {
                elements[i] = myTransactionWrapper.findNew(elements[i])
            }
            processor.refreshElements(elements)
            processor.run()
        }
        myTransactionWrapper.finalCommit()
    }

    override fun getCommandName(): String {
        logger.trace { "getCommandName" }
        return commandName
    }
}
