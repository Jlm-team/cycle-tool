package team.jlm.coderefactor.code

import com.intellij.openapi.project.Project
import com.intellij.packageDependencies.ForwardDependenciesBuilder
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.impl.source.tree.JavaElementType.*
import com.intellij.psi.impl.source.tree.TreeElement
import com.intellij.psi.util.elementType
import org.jetbrains.uast.findAnyContaining
import org.jetbrains.uast.findContaining
import org.jetbrains.uast.test.env.findUElementByTextFromPsi
import team.jlm.utils.getPsiJavaFile

fun getDependencyList(fileString: String, project: Project): List<Dependency> {
    val psiClass = getPsiJavaFile(project, fileString)
    val result = ArrayList<Dependency>(psiClass.textLength / 380)//估算类中大约有多少个依赖项
    ForwardDependenciesBuilder.analyzeFileDependencies(psiClass.containingFile as PsiJavaFile)
    { dependElement: PsiElement, selfElement: PsiElement ->
        run {
            if (selfElement !is PsiClass) return@run
            val dependencyType = dependElement.dependencyType
            val dependencyRange = dependElement.textRange
            val dependencyStr: String =
                fileString.subSequence(dependencyRange.startOffset, dependencyRange.endOffset).toString()
            result.add(Dependency(dependencyType, dependencyStr))
        }
    }
    return result
}

val PsiElement.dependencyType: DependencyType
    get() {
        if (this !is TreeElement) return DependencyType.DEPEND
//        println("$this, ${this.treeParent.elementType}, ${this.treeParent.treeParent.elementType}")
        val dependencyType = when (this.elementType) {
            JAVA_CODE_REFERENCE ->
                when (this.treeParent.elementType) {
                    EXTENDS_LIST -> DependencyType.EXTENDS
                    IMPLEMENTS_LIST -> DependencyType.IMPLEMENT
                    IMPORT_STATEMENT, IMPORT_LIST, IMPORT_STATIC_STATEMENT, IMPORT_STATIC_REFERENCE,
                    -> DependencyType.IMPORT
                    ANNOTATION -> DependencyType.ANNOTATION
                    TYPE -> when (this.treeParent.treeParent.elementType) {
                        FIELD -> DependencyType.CONTAIN
                        PARAMETER -> DependencyType.PARAMETER
                        else -> DependencyType.DEPEND
                    }
                    else -> DependencyType.DEPEND
                }
            REFERENCE_EXPRESSION ->
                when (this.treeParent.elementType) {
                    REFERENCE_EXPRESSION -> when (this.treeParent.treeParent.elementType) {
                        METHOD_CALL_EXPRESSION -> DependencyType.CALL
                        else -> DependencyType.DEPEND
                    }
                    else -> DependencyType.DEPEND
                }
            else -> DependencyType.DEPEND
        }
        return dependencyType
    }

