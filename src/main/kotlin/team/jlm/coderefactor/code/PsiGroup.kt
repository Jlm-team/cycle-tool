package team.jlm.coderefactor.code

import com.intellij.packageDependencies.DependencyVisitorFactory
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiUtil
import com.intellij.psi.util.nextLeaf

class PsiGroup() {
    private val elements: ArrayList<PsiElement> = ArrayList(4)

    constructor(element: PsiElement) : this() {
        elements.add(element)
    }

    val nextLeaf: PsiElement?
        get() =
            elements[elements.size - 1].nextLeaf()

    constructor(elements: List<PsiElement>) : this() {
        this.elements.addAll(elements)
    }

    val textLength: Int
        get() = elements.sumOf { it.textLength }

    private fun switchToParent() {
        val nextSibling = elements[elements.size - 1].nextSibling
        if (nextSibling == null) {
            elements[elements.size - 1].parent?.let {
                elements.clear()
                elements.add(it)
            }
        }
    }

    fun textMatches(text: CharSequence): Boolean {
        if (text.length != textLength) return false
        var index: Int = 0
        for (element in elements) {
            val subText = text.subSequence(index, index + element.textLength)
            if (!element.textMatches(subText)) return false
            index += element.textLength
        }
        return true
    }

    fun add(element: PsiElement) = elements.add(element)

    val dependencyList: ArrayList<String>
        get() {
            val result = ArrayList<String>(4)
            var commonParentPsi: PsiElement
            if (elements.size > 1) {
                commonParentPsi = elements[0]
                while (!commonParentPsi.textRange.contains(elements[elements.size - 1].textRange)) {
                    commonParentPsi = commonParentPsi.parent
                }
            } else {
                commonParentPsi = elements[0]
            }
            commonParentPsi.accept(
                DependVisitor(
                    { dependElementInThisFile: PsiElement, dependElement: PsiElement ->
                        run {
                            if (dependElement !is PsiClass) return@run
                            dependElement.qualifiedName?.let { result.add(it) }
                        }
                    }, DependencyVisitorFactory.VisitorOptions.INCLUDE_IMPORTS
                )
            )
            return result
        }
}