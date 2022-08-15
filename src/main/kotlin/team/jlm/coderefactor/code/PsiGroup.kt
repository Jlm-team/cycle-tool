package team.jlm.coderefactor.code

import com.intellij.openapi.util.TextRange
import com.intellij.packageDependencies.DependencyVisitorFactory
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.util.nextLeaf
import com.intellij.psi.util.nextLeafs
import com.xyzboom.algorithm.graph.Graph

class PsiGroup(
    private val shouldBeRange: TextRange,
    private val classNameAndTextRange: Map<String, TextRange>,
) {
    private val elements: ArrayList<PsiElement> = ArrayList(4)

    constructor(
        element: PsiElement, range: TextRange,
        classNameAndTextRange: Map<String, TextRange>,
    ) : this(range, classNameAndTextRange) {
        elements.add(element)
        var now = element
        while (now.textRange.endOffset < range.endOffset) {
            val tryGetNextLeaf = now.nextLeaf()
            if (tryGetNextLeaf != null) {
                now = tryGetNextLeaf
                elements.add(now)
            }
        }
    }

    fun add(element: PsiElement) = elements.add(element)

    private fun getRangeInClassName(range: TextRange): String {
        classNameAndTextRange.forEach { (className, textRange) ->
            run {
                if (textRange.contains(range)) {
                    return className
                }
            }
        }
        return "team.jlm.UnknownClass"
    }

    val dependencyGraph: Graph<String>
        get() {
            val result = Graph<String>()
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
                    { placeElement: PsiElement, dependElement: PsiElement ->
                        run {
                            if (dependElement !is PsiClass) return@run
                            //提取的公共父节点中可能包含不在改变文本中的节点，因此用文本偏移进行排除
                            if (!shouldBeRange.contains(placeElement.textRange)) return@run
                            dependElement.qualifiedName?.let {
                                val selfClassName = getRangeInClassName(placeElement.textRange)
//                                println("add dependency: $selfClassName --> $it")
                                result.addEdge(selfClassName, it)
                            }
                        }
                    }, DependencyVisitorFactory.VisitorOptions.INCLUDE_IMPORTS
                )
            )
            return result
        }
}