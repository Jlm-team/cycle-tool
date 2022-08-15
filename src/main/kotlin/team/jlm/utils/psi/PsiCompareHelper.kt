package team.jlm.utils.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiJavaFile

class PsiCompareHelper(val element: PsiElement) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PsiCompareHelper

        if (!element.textMatches(other.element)) return false

        return true
    }

    override fun hashCode(): Int {
        return element.hashCode()
    }

    override fun toString(): String {
        return element.toString()
    }
}

fun leafPsiHelpersFromPsiElement(psiElement: PsiElement): List<PsiCompareHelper> {
    val result = ArrayList<PsiCompareHelper>()
    val c = psiElement.children
    if (c.isEmpty()){
        result.add(PsiCompareHelper(psiElement))
        return result
    }
    for (e in c) {
        if (e == null) continue
        result.addAll(leafPsiHelpersFromPsiElement(e))
    }
    return result
}

fun createPsiHelpersFromFile(f: PsiJavaFile): List<PsiCompareHelper> {
    return leafPsiHelpersFromPsiElement(f)
}