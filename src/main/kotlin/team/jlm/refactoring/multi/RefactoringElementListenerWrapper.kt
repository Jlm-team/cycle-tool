package team.jlm.refactoring.multi

import com.intellij.psi.PsiElement
import com.intellij.refactoring.listeners.RefactoringElementListener

class RefactoringElementListenerWrapper(
    private val refactoringElementListener: RefactoringElementListener,
    private val elementChanged: (PsiElement) -> Unit,
) : RefactoringElementListener {
    override fun elementMoved(newElement: PsiElement) {
        elementChanged(newElement)
        return refactoringElementListener.elementMoved(newElement)
    }

    override fun elementRenamed(newElement: PsiElement) {
        elementChanged(newElement)
        return refactoringElementListener.elementRenamed(newElement)
    }
}