package team.jlm.refactoring

import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.listeners.RefactoringListenerManager
import com.intellij.refactoring.listeners.impl.RefactoringListenerManagerImpl
import com.intellij.refactoring.suggested.SuggestedRefactoringProvider
import com.intellij.usageView.UsageInfo
import com.intellij.usageView.UsageViewBundle
import com.intellij.usageView.UsageViewDescriptor

class MultiRefactoringProcessor(
    project: Project,
    private val processors: List<BaseRefactoringProcessor>,
    refactoringScope: SearchScope = GlobalSearchScope.projectScope(project),
    prepareSuccessfulCallback: Runnable? = null,
    private val processedElementsHeader: String = "MultiRefactoring",
    private val commandName: String,
) : BaseRefactoringProcessor(project, refactoringScope, prepareSuccessfulCallback) {
    override fun createUsageViewDescriptor(): UsageViewDescriptor {
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
        processors.forEachIndexed { index, processor ->
            processor.performRefactoring(usages.copyOfRange(usagesIndexList[index], usagesIndexList[index + 1]))
        }
    }

    override fun preprocessUsages(): Boolean {
        return processors.all { it.preprocessUsages() }
    }

    override fun execute(usages: Array<out UsageInfo>) {

        CommandProcessor.getInstance().executeCommand(myProject, {
            val usageInfos: MutableCollection<UsageInfo> =
                LinkedHashSet(listOf(*usages))
            PsiDocumentManager.getInstance(myProject!!).commitAllDocuments()
            // WARN 此处增加了事务的粒度，可能带来某些异常，但是此功能是必需的
            val listenerManager = RefactoringListenerManager.getInstance(myProject) as RefactoringListenerManagerImpl
            transaction = listenerManager.startTransaction()
            for (processor in processors) {
                processor.transaction = transaction
            }
            doRefactoring(usageInfos)
            transaction.commit()
            if (isGlobalUndoAction()) CommandProcessor.getInstance()
                .markCurrentCommandAsGlobal(myProject)
            SuggestedRefactoringProvider.getInstance(myProject!!).reset()
        }, getCommandName(), null, getUndoConfirmationPolicy())
    }

    override fun getCommandName(): String {
        return commandName
    }
}
