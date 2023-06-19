package team.jlm.refactoring.multi

import com.intellij.psi.PsiElement
import com.intellij.refactoring.listeners.RefactoringElementListener
import com.intellij.refactoring.listeners.impl.RefactoringTransaction
import team.jlm.utils.collections.UnionFindSet

class RefactoringTransactionWrapper(
    private val transaction: RefactoringTransaction,
) : RefactoringTransaction {
    private val oldToNew = HashMap<PsiElement, PsiElement>()
    private val oldNewUFSet = UnionFindSet<PsiElement>()

    fun findNew(psiElement: PsiElement): PsiElement {
        return oldToNew[oldNewUFSet.getHead(psiElement)] ?: psiElement
    }

    private fun elementChanged(oldElement: PsiElement, newElement: PsiElement) {
        if (!oldNewUFSet.contains(oldElement)) {
            oldNewUFSet.add(oldElement)
        }
        if (!oldNewUFSet.contains(newElement)) {
            oldNewUFSet.add(newElement)
        }
        oldNewUFSet.union(oldElement, newElement)
        oldToNew[oldNewUFSet.getHead(oldElement)!!] = newElement
    }

    override fun getElementListener(element: PsiElement): RefactoringElementListener {
        return RefactoringElementListenerWrapper(transaction.getElementListener(element)) {
            elementChanged(element, it)
        }
    }

    override fun commit() {
        // as this is a wrapper
        // nothing happened
    }

    fun finalCommit() {
        transaction.commit()
    }
}