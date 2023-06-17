package team.jlm.refactoring

import com.intellij.codeInsight.actions.VcsFacade
import com.intellij.find.findUsages.PsiElement2UsageTargetAdapter
import com.intellij.history.LocalHistory
import com.intellij.ide.DataManager
import com.intellij.lang.Language
import com.intellij.model.ModelBranch
import com.intellij.model.ModelPatch
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.application.ex.ApplicationManagerEx
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.command.UndoConfirmationPolicy
import com.intellij.openapi.command.undo.BasicUndoableAction
import com.intellij.openapi.command.undo.UndoManager
import com.intellij.openapi.command.undo.UndoableAction
import com.intellij.openapi.fileEditor.impl.NonProjectFileWritingAccessProvider
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.UnloadedModuleDescription
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.IndexNotReadyException
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Factory
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.wm.impl.status.StatusBarUtil
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.psi.util.PsiUtilCore
import com.intellij.refactoring.BaseRefactoringProcessor
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.RefactoringHelper
import com.intellij.refactoring.listeners.RefactoringEventData
import com.intellij.refactoring.listeners.RefactoringEventListener
import com.intellij.refactoring.listeners.RefactoringListenerManager
import com.intellij.refactoring.listeners.impl.RefactoringListenerManagerImpl
import com.intellij.refactoring.listeners.impl.RefactoringTransaction
import com.intellij.refactoring.suggested.SuggestedRefactoringProvider
import com.intellij.refactoring.ui.ConflictsDialog
import com.intellij.refactoring.util.CommonRefactoringUtil
import com.intellij.usageView.UsageInfo
import com.intellij.usageView.UsageViewBundle
import com.intellij.usageView.UsageViewDescriptor
import com.intellij.usageView.UsageViewUtil
import com.intellij.usages.*
import com.intellij.usages.impl.UnknownUsagesInUnloadedModules
import com.intellij.usages.impl.UsageViewImpl
import com.intellij.usages.rules.PsiElementUsage
import com.intellij.util.Processor
import com.intellij.util.SlowOperations
import com.intellij.util.containers.MultiMap
import com.intellij.util.containers.toArray
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet
import mu.KotlinLogging
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.NonNls
import java.awt.event.ActionEvent
import java.util.*
import javax.swing.AbstractAction

private val logger = KotlinLogging.logger {}

@Suppress("UnstableApiUsage")
abstract class BaseRefactoringProcessor @JvmOverloads constructor(
    project: Project,
    refactoringScope: SearchScope = GlobalSearchScope.projectScope(project),
    prepareSuccessfulCallback: Runnable? = null,
) {
    private var PREVIEW_IN_TESTS = true

    protected var myProject: Project? = project
    protected var myRefactoringScope: SearchScope = refactoringScope

    private lateinit var myTransaction: RefactoringTransaction
    private var myIsPreviewUsages = false
    var myPrepareSuccessfulSwingThreadCallback: Runnable? = prepareSuccessfulCallback
    private var myUsageView: UsageView? = null

    internal abstract fun createUsageViewDescriptor(usages: Array<out UsageInfo>): UsageViewDescriptor

    /**
     * Is called inside atomic action.
     */
    abstract fun findUsages(): Array<out UsageInfo>

    /**
     * is called when usage search is re-run.
     *
     * @param elements - refreshed elements that are returned by UsageViewDescriptor.getElements()
     */
    protected open fun refreshElements(elements: Array<out PsiElement>) {}

    /**
     * Is called inside atomic action.
     *
     * @param refUsages usages to be filtered
     * @return true if preprocessed successfully
     */
    open fun preprocessUsages(refUsages: Ref<Array<out UsageInfo>>): Boolean {
        prepareSuccessful()
        return true
    }

    /**
     * Is called inside atomic action.
     */
    open fun isPreviewUsages(usages: Array<out UsageInfo>): Boolean {
        return myIsPreviewUsages
    }

    open fun isPreviewUsages(): Boolean {
        return myIsPreviewUsages
    }

    private fun computeUnloadedModulesFromUseScope(descriptor: UsageViewDescriptor): Set<UnloadedModuleDescription> {
        if (ModuleManager.getInstance(myProject!!).unloadedModuleDescriptions.isEmpty()) {
            //optimization
            return emptySet()
        }
        val unloadedModulesInUseScope: MutableSet<UnloadedModuleDescription> = LinkedHashSet()
        for (element in descriptor.elements) {
            val useScope = element.useScope
            if (useScope is GlobalSearchScope) {
                unloadedModulesInUseScope.addAll(useScope.unloadedModulesBelongingToScope)
            }
        }
        return unloadedModulesInUseScope
    }


    open fun setPreviewUsages(isPreviewUsages: Boolean) {
        myIsPreviewUsages = isPreviewUsages
    }

    open fun setPrepareSuccessfulSwingThreadCallback(prepareSuccessfulSwingThreadCallback: Runnable?) {
        myPrepareSuccessfulSwingThreadCallback = prepareSuccessfulSwingThreadCallback
    }

    lateinit var transaction: RefactoringTransaction
        internal set

    /**
     * Is called in a command and inside atomic action.
     */
    internal abstract fun performRefactoring(usages: Array<out UsageInfo>)

    @ApiStatus.Experimental
    protected open fun canPerformRefactoringInBranch(): Boolean {
        return false
    }

    @ApiStatus.Experimental
    protected open fun performRefactoringInBranch(usages: Array<UsageInfo>, branch: ModelBranch?) {
        throw UnsupportedOperationException()
    }

    protected abstract fun getCommandName(): @NlsContexts.Command String

    protected open fun doRun() {
        if (!PsiDocumentManager.getInstance(myProject!!).commitAllDocumentsUnderProgress()) {
            return
        }
        val refUsages = Ref<Array<out UsageInfo>>()
        val refErrorLanguage = Ref<Language>()
        val refProcessCanceled = Ref<Boolean>()
        val anyException = Ref<Boolean>()
        val indexNotReadyException = Ref<Boolean>()
        DumbService.getInstance(myProject!!).completeJustSubmittedTasks()
        val findUsagesRunnable = Runnable {
            try {
                refUsages.set(
                    ReadAction.compute<Array<out UsageInfo>, RuntimeException> { findUsages() }
                )
            } catch (e: UnknownReferenceTypeException) {
                refErrorLanguage.set(e.elementLanguage)
            } catch (e: ProcessCanceledException) {
                refProcessCanceled.set(java.lang.Boolean.TRUE)
            } catch (e: IndexNotReadyException) {
                indexNotReadyException.set(java.lang.Boolean.TRUE)
            } catch (e: Throwable) {
                anyException.set(java.lang.Boolean.TRUE)
                logger.error(e) {}
            }
        }
        if (!ProgressManager.getInstance().runProcessWithProgressSynchronously(
                findUsagesRunnable, RefactoringBundle.message("progress.text"),
                true, myProject
            )
        ) {
            return
        }
        if (!refErrorLanguage.isNull) {
            Messages.showErrorDialog(
                myProject,
                RefactoringBundle.message("unsupported.refs.found", refErrorLanguage.get().displayName),
                RefactoringBundle.message("error.title")
            )
            return
        }
        if (!indexNotReadyException.isNull || DumbService.isDumb(myProject!!)) {
            DumbService.getInstance(myProject!!)
                .showDumbModeNotification(RefactoringBundle.message("refactoring.dumb.mode.notification"))
            return
        }
        if (!refProcessCanceled.isNull) {
            Messages.showErrorDialog(
                myProject,
                RefactoringBundle.message("refactoring.index.corruption.notifiction"),
                RefactoringBundle.message("error.title")
            )
            return
        }
        if (!anyException.isNull) {
            //do not proceed if find usages fails
            return
        }
        assert(!refUsages.isNull) { "Null usages from processor $this" }
        if (!preprocessUsages(refUsages)) return
        val usages = refUsages.get()!!
        val descriptor = createUsageViewDescriptor(usages)
        var isPreview = isPreviewUsages(usages) || !computeUnloadedModulesFromUseScope(descriptor).isEmpty()
        if (!isPreview) {
            isPreview = !ensureElementsWritable(usages, descriptor) || UsageViewUtil.hasReadOnlyUsages(usages)
            if (isPreview) {
                StatusBarUtil.setStatusBarInfo(myProject!!, RefactoringBundle.message("readonly.occurences.found"))
            }
        }
        if (isPreview) {
            previewRefactoring(usages)
        } else {
            execute(usages)
        }
    }

    protected open fun previewRefactoring(usages: Array<out UsageInfo>) {
        if (ApplicationManager.getApplication().isUnitTestMode) {
            if (!PREVIEW_IN_TESTS) throw RuntimeException(
                "Unexpected preview in tests: " + StringUtil.join(
                    usages,
                    { obj: UsageInfo -> obj.toString() }, ", "
                )
            )
            ensureElementsWritable(usages, createUsageViewDescriptor(usages))
            execute(usages)
            return
        }
        val viewDescriptor = createUsageViewDescriptor(usages)
        val elements = viewDescriptor.elements
        val targets = PsiElement2UsageTargetAdapter.convert(elements, true)
        val factory =
            Factory<UsageSearcher> {
                object : UsageInfoSearcherAdapter() {
                    override fun generate(processor: Processor<in Usage>) {
                        ApplicationManager.getApplication().runReadAction {
                            for (i in elements.indices) {
                                elements[i] = targets[i].element
                            }
                            refreshElements(elements)
                        }
                        processUsages(processor, myProject!!)
                    }

                    override fun findUsages(): Array<out UsageInfo> {
                        return this@BaseRefactoringProcessor.findUsages()
                    }
                }
            }
        showUsageView(viewDescriptor, factory, usages)
    }

    protected open fun skipNonCodeUsages(): Boolean {
        return false
    }

    private fun ensureElementsWritable(usages: Array<out UsageInfo>, descriptor: UsageViewDescriptor): Boolean {
        // protect against poorly implemented equality
        val elements: MutableSet<PsiElement> = ReferenceOpenHashSet()
        for (usage in usages) {
            if (skipNonCodeUsages() && usage.isNonCodeUsage()) {
                continue
            }
            val element = usage.element
            if (element != null) {
                elements.add(element)
            }
        }
        elements.addAll(getElementsToWrite(descriptor))
        return ensureFilesWritable(myProject!!, elements)
    }

    private fun ensureFilesWritable(project: Project, elements: Collection<PsiElement>): Boolean {
        val psiElements = PsiUtilCore.toPsiElementArray(elements)
        return CommonRefactoringUtil.checkReadOnlyStatus(project, *psiElements)
    }

    open fun executeEx(usages: Array<out UsageInfo>) {
        execute(usages)
    }

    open fun execute(usages: Array<out UsageInfo>) {
        CommandProcessor.getInstance().executeCommand(myProject, {
            val usageInfos: MutableCollection<UsageInfo> =
                LinkedHashSet(listOf(*usages))
            PsiDocumentManager.getInstance(myProject!!).commitAllDocuments()
            // WARN 此处增加了事务的粒度，可能带来某些异常，但是此功能是必需的
            val listenerManager = RefactoringListenerManager.getInstance(myProject) as RefactoringListenerManagerImpl
            myTransaction = listenerManager.startTransaction()
            transaction = myTransaction
            doRefactoring(usageInfos)
            myTransaction.commit()
            if (isGlobalUndoAction()) CommandProcessor.getInstance()
                .markCurrentCommandAsGlobal(myProject)
            SuggestedRefactoringProvider.getInstance(myProject!!).reset()
        }, getCommandName(), null, getUndoConfirmationPolicy())
    }

    protected open fun isGlobalUndoAction(): Boolean {
        return DataManager.getInstance().dataContextFromFocusAsync.blockingGet(3000)
            ?.let { CommonDataKeys.EDITOR.getData(it) } == null
    }

    protected open fun getUndoConfirmationPolicy(): UndoConfirmationPolicy {
        return UndoConfirmationPolicy.DEFAULT
    }

    private fun createPresentation(descriptor: UsageViewDescriptor, usages: Array<out Usage>): UsageViewPresentation {
        val presentation = UsageViewPresentation()
        presentation.tabText = RefactoringBundle.message("usageView.tabText")
        presentation.targetsNodeText = descriptor.processedElementsHeader
        presentation.isShowReadOnlyStatusAsRed = true
        presentation.isShowCancelButton = true
        presentation.searchString = RefactoringBundle.message("usageView.usagesText")
        var codeUsageCount = 0
        var nonCodeUsageCount = 0
        var dynamicUsagesCount = 0
        val codeFiles: MutableSet<PsiFile?> = HashSet()
        val nonCodeFiles: MutableSet<PsiFile?> = HashSet()
        val dynamicUsagesCodeFiles: MutableSet<PsiFile?> = HashSet()
        for (usage in usages) {
            if (usage is PsiElementUsage) {
                val element = usage.element ?: continue
                val containingFile = element.containingFile
                if (usage is UsageInfo2UsageAdapter && usage.usageInfo.isDynamicUsage) {
                    dynamicUsagesCount++
                    dynamicUsagesCodeFiles.add(containingFile)
                } else if (usage.isNonCodeUsage) {
                    nonCodeUsageCount++
                    nonCodeFiles.add(containingFile)
                } else {
                    codeUsageCount++
                    codeFiles.add(containingFile)
                }
            }
        }
        codeFiles.remove(null)
        nonCodeFiles.remove(null)
        dynamicUsagesCodeFiles.remove(null)
        presentation.codeUsagesString = UsageViewBundle.message(
            "usage.view.results.node.prefix",
            UsageViewBundle.message("usage.view.results.node.code"),
            descriptor.getCodeReferencesText(codeUsageCount, codeFiles.size)
        )
        presentation.nonCodeUsagesString = UsageViewBundle.message(
            "usage.view.results.node.prefix",
            UsageViewBundle.message("usage.view.results.node.non.code"),
            descriptor.getCodeReferencesText(nonCodeUsageCount, nonCodeFiles.size)
        )
        presentation.setDynamicUsagesString(
            UsageViewBundle.message(
                "usage.view.results.node.prefix",
                UsageViewBundle.message("usage.view.results.node.dynamic"),
                descriptor.getCodeReferencesText(dynamicUsagesCount, dynamicUsagesCodeFiles.size)
            )
        )
        return presentation
    }

    /**
     * Processes conflicts (possibly shows UI). In case we're running in unit test mode this method will
     * throw [BaseRefactoringProcessor.ConflictsInTestsException] that can be handled inside a test.
     * Thrown exception would contain conflicts' messages.
     *
     * @param project   project
     * @param conflicts map with conflict messages and locations
     * @return true if refactoring could proceed or false if refactoring should be cancelled
     */
    open fun processConflicts(project: Project, conflicts: MultiMap<PsiElement?, String>): Boolean {
        if (conflicts.isEmpty) return true
        if (ApplicationManager.getApplication().isUnitTestMode) {
            if (ConflictsInTestsException.isTestIgnore) return true
            throw ConflictsInTestsException(conflicts.values())
        }
        val conflictsDialog = ConflictsDialog(project, conflicts)
        return conflictsDialog.showAndGet()
    }

    private fun showUsageView(
        viewDescriptor: UsageViewDescriptor,
        factory: Factory<out UsageSearcher>,
        usageInfos: Array<out UsageInfo>,
    ) {
        val viewManager = UsageViewManager.getInstance(myProject)
        val initialElements = viewDescriptor.elements
        val targets: Array<out UsageTarget> = PsiElement2UsageTargetAdapter.convert(initialElements, true)
        val convertUsagesRef = Ref<Array<out Usage>>()
        if (!ProgressManager.getInstance().runProcessWithProgressSynchronously(
                {
                    ApplicationManager.getApplication().runReadAction {
                        convertUsagesRef.set(
                            UsageInfo2UsageAdapter.convert(usageInfos)
                        )
                    }
                },
                RefactoringBundle.message("refactoring.preprocess.usages.progress"), true, myProject
            )
        ) return
        if (convertUsagesRef.isNull) return
        val usages = convertUsagesRef.get()
        val presentation = createPresentation(viewDescriptor, usages)
        if (myUsageView == null) {
            myUsageView = viewManager.showUsages(targets, usages, presentation, factory)
            customizeUsagesView(viewDescriptor, myUsageView!!)
        } else {
            myUsageView!!.removeUsagesBulk(myUsageView!!.usages)
            (myUsageView as UsageViewImpl).appendUsagesInBulk(listOf(*usages))
        }
        val unloadedModules = computeUnloadedModulesFromUseScope(viewDescriptor)
        if (unloadedModules.isNotEmpty()) {
            myUsageView!!.appendUsage(UnknownUsagesInUnloadedModules(unloadedModules))
        }
    }

    protected open fun customizeUsagesView(viewDescriptor: UsageViewDescriptor, usageView: UsageView) {
        val refactoringRunnable = Runnable {
            val usagesToRefactor = UsageViewUtil.getNotExcludedUsageInfos(usageView)
            val infos = usagesToRefactor.toArray<UsageInfo>(UsageInfo.EMPTY_ARRAY)
            if (ensureElementsWritable(infos, viewDescriptor)) {
                execute(infos)
            }
        }
        val canNotMakeString = RefactoringBundle.message("usageView.need.reRun")
        addDoRefactoringAction(usageView, refactoringRunnable, canNotMakeString)
        usageView.setRerunAction(object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent?) {
                run()
            }
        })
    }

    private fun addDoRefactoringAction(
        usageView: UsageView,
        refactoringRunnable: Runnable,
        canNotMakeString: String,
    ) {
        usageView.addPerformOperationAction(
            refactoringRunnable, getCommandName(), canNotMakeString,
            RefactoringBundle.message("usageView.doAction"), false
        )
    }

    protected fun doRefactoring(usageInfoSet: MutableCollection<UsageInfo>) {
        val iterator = usageInfoSet.iterator()
        while (iterator.hasNext()) {
            val usageInfo = iterator.next()
            val element = usageInfo.element
            if (element == null || !isToBeChanged(usageInfo)) {
                iterator.remove()
            }
        }
        val commandName = getCommandName()
        val action = LocalHistory.getInstance().startAction(commandName)
        val writableUsageInfos = usageInfoSet.toArray<UsageInfo>(UsageInfo.EMPTY_ARRAY)
        try {
            val preparedData = LinkedHashMap<RefactoringHelper<Any?>, Any?>()
            val prepareHelpersRunnable = Runnable {
                for (helper in RefactoringHelper.EP_NAME.extensionList) {
                    val operation =
                        ReadAction.compute<Any?, RuntimeException> {
                            helper.prepareOperation(
                                writableUsageInfos
                            )
                        }
                    preparedData[helper] = operation
                }
            }
            ProgressManager.getInstance().runProcessWithProgressSynchronously(
                prepareHelpersRunnable,
                RefactoringBundle.message("refactoring.prepare.progress"), false, myProject
            )
            val app = ApplicationManagerEx.getApplicationEx()
            val inBranch = Registry.`is`("run.refactorings.in.model.branch") && canPerformRefactoringInBranch()
            if (inBranch) {
                callPerformRefactoring(writableUsageInfos) { performInBranch(writableUsageInfos) }
            } else if (Registry.`is`("run.refactorings.under.progress")) {
                app.runWriteActionWithNonCancellableProgressInDispatchThread(
                    commandName, myProject, null
                ) {
                    callPerformRefactoring(
                        writableUsageInfos
                    ) { performRefactoring(writableUsageInfos) }
                }
            } else {
                app.runWriteAction {
                    callPerformRefactoring(writableUsageInfos) {
                        performRefactoring(
                            writableUsageInfos
                        )
                    }
                }
            }
            DumbService.getInstance(myProject!!).completeJustSubmittedTasks()
            for ((key, value) in preparedData) {
                key.performOperation(myProject!!, value)
            }
            if (!inBranch) {
                if (Registry.`is`("run.refactorings.under.progress")) {
                    app.runWriteActionWithNonCancellableProgressInDispatchThread(
                        commandName, myProject, null
                    ) { performPsiSpoilingRefactoring() }
                } else {
                    app.runWriteAction { performPsiSpoilingRefactoring() }
                }
            }
        } finally {
            action.finish()
        }
        val count = writableUsageInfos.size
        if (count > 0) {
            StatusBarUtil.setStatusBarInfo(
                myProject!!,
                RefactoringBundle.message("statusBar.refactoring.result", count)
            )
        } else {
            if (!isPreviewUsages(writableUsageInfos)) {
                StatusBarUtil.setStatusBarInfo(myProject!!, RefactoringBundle.message("statusBar.noUsages"))
            }
        }
    }

    private fun callPerformRefactoring(usageInfos: Array<UsageInfo>, perform: Runnable) {
        val refactoringId = getRefactoringId()
        if (refactoringId != null) {
            val data = getBeforeData()
            data?.addUsages(listOf(*usageInfos))
            myProject!!.messageBus.syncPublisher(RefactoringEventListener.REFACTORING_EVENT_TOPIC)
                .refactoringStarted(refactoringId, data)
        }
        try {
            if (refactoringId != null) {
                val action1: UndoableAction = UndoRefactoringAction(myProject!!, refactoringId)
                UndoManager.getInstance(myProject!!).undoableActionPerformed(action1)
            }
            perform.run()
        } finally {
            if (refactoringId != null) {
                myProject!!.messageBus
                    .syncPublisher(RefactoringEventListener.REFACTORING_EVENT_TOPIC)
                    .refactoringDone(refactoringId, getAfterData(usageInfos))
            }
        }
    }

    private fun performInBranch(usageInfos: Array<UsageInfo>) {
        val computable =
            ThrowableComputable<ModelPatch, RuntimeException> {
                ReadAction.compute<ModelPatch, RuntimeException> {
                    ModelBranch.performInBranch(
                        myProject!!
                    ) { branch: ModelBranch? ->
                        performRefactoringInBranch(
                            usageInfos,
                            branch
                        )
                    }
                }
            }
        val patch = ProgressManager.getInstance().runProcessWithProgressSynchronously(
            computable, getCommandName(), true, myProject
        )
        if (!ApplicationManager.getApplication().isUnitTestMode && isPreviewUsages()) {
            displayPreview(patch)
        }
        WriteAction.run<RuntimeException> { patch.applyBranchChanges() }
    }

    private fun displayPreview(patch: ModelPatch) {
        val preview = VcsFacade.getInstance().createPatchPreviewComponent(myProject!!, patch)
        if (preview != null) {
            val builder =
                DialogBuilder(myProject).title(RefactoringBundle.message("usageView.tabText")).centerPanel(preview)
            if (builder.show() != DialogWrapper.OK_EXIT_CODE) {
                throw ProcessCanceledException()
            }
        }
    }

    protected open fun isToBeChanged(usageInfo: UsageInfo): Boolean {
        return usageInfo.isWritable
    }

    /**
     * Non-ModelBranch refactorings that spoil PSI (write something directly to documents etc.) should
     * do that in this method.<br></br>
     * This method is called immediately after
     * `[.performRefactoring]`.
     * For branch-aware refactorings, please do this work inside [.performRefactoringInBranch].
     */
    protected open fun performPsiSpoilingRefactoring() {}

    protected open fun prepareSuccessful() {
        if (myPrepareSuccessfulSwingThreadCallback != null) {
            // make sure that dialog is closed in swing thread
            try {
                ApplicationManager.getApplication().invokeAndWait(
                    myPrepareSuccessfulSwingThreadCallback!!
                )
            } catch (e: RuntimeException) {
                logger.error(e) {}
            }
        }
    }

    fun run() {
        val baseRunnable =
            Runnable { SlowOperations.allowSlowOperations<RuntimeException> { doRun() } }
        val runnable = if (shouldDisableAccessChecks()) Runnable {
            NonProjectFileWritingAccessProvider.disableChecksDuring(
                baseRunnable
            )
        } else baseRunnable
        if (ApplicationManager.getApplication().isUnitTestMode) {
            ApplicationManager.getApplication().assertIsWriteThread()
            runnable.run()
            return
        }
        if (ApplicationManager.getApplication().isWriteAccessAllowed) {
            logger.error(Exception()) {
                "Refactorings should not be started inside write action\n " +
                        "because they start progress inside and any read action from the progress task would cause the deadlock"
            }
            DumbService.getInstance(myProject!!).smartInvokeLater(runnable)
        } else {
            runnable.run()
        }
    }

    protected open fun shouldDisableAccessChecks(): Boolean {
        return false
    }

    class ConflictsInTestsException(private val messages: Collection<String>) :
        RuntimeException() {
        fun getMessages(): Collection<String> {
            val result: MutableList<String> = ArrayList(messages)
            for (i in messages.indices) {
                result[i] = result[i].replace("<[^>]+>".toRegex(), "")
            }
            return result
        }

        override val message: String
            get() {
                val result = ArrayList(messages)
                result.sort()
                return StringUtil.join(result, "\n")
            }

        companion object {
            var isTestIgnore = false
                private set
        }
    }

    protected open fun showConflicts(conflicts: MultiMap<PsiElement?, String>, usages: Array<out UsageInfo>?): Boolean {
        if (!conflicts.isEmpty && ApplicationManager.getApplication().isUnitTestMode) {
            if (!ConflictsInTestsException.isTestIgnore) throw ConflictsInTestsException(conflicts.values())
            return true
        }
        if (myPrepareSuccessfulSwingThreadCallback != null && !conflicts.isEmpty) {
            val refactoringId = getRefactoringId()
            if (refactoringId != null) {
                val conflictUsages = RefactoringEventData()
                conflictUsages.putUserData(RefactoringEventData.CONFLICTS_KEY, conflicts.values())
                myProject!!.messageBus.syncPublisher(RefactoringEventListener.REFACTORING_EVENT_TOPIC)
                    .conflictsDetected(refactoringId, conflictUsages)
            }
            val conflictsDialog = prepareConflictsDialog(conflicts, usages)
            if (!conflictsDialog.showAndGet()) {
                if (conflictsDialog.isShowConflicts) prepareSuccessful()
                return false
            }
        }
        prepareSuccessful()
        return true
    }

    protected open fun prepareConflictsDialog(
        conflicts: MultiMap<PsiElement?, String>,
        usages: Array<out UsageInfo>?,
    ): ConflictsDialog {
        val conflictsDialog = createConflictsDialog(conflicts, usages)
        conflictsDialog.setCommandName(getCommandName())
        return conflictsDialog
    }

    protected open fun getBeforeData(): RefactoringEventData? {
        return null
    }

    protected open fun getAfterData(usages: Array<UsageInfo>): RefactoringEventData? {
        return null
    }

    protected open fun getRefactoringId(): @NonNls String? {
        return null
    }

    protected open fun createConflictsDialog(
        conflicts: MultiMap<PsiElement?, String>,
        usages: Array<out UsageInfo>?,
    ): ConflictsDialog {
        return ConflictsDialog(
            myProject!!,
            conflicts,
            if (usages == null) null else Runnable { execute(usages) },
            false,
            true
        )
    }

    protected open fun getElementsToWrite(descriptor: UsageViewDescriptor): Collection<PsiElement> {
        return listOf(*descriptor.elements)
    }

    class UnknownReferenceTypeException(val elementLanguage: Language) :
        RuntimeException()

    private class UndoRefactoringAction(
        private val myProject: Project,
        private val myRefactoringId: String,
    ) : BasicUndoableAction() {
        override fun undo() {
            myProject.messageBus.syncPublisher(RefactoringEventListener.REFACTORING_EVENT_TOPIC)
                .undoRefactoring(myRefactoringId)
        }

        override fun redo() {}
    }
}