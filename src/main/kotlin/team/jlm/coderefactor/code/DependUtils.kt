package team.jlm.coderefactor.code

import com.intellij.openapi.project.Project
import com.intellij.packageDependencies.DependencyVisitorFactory
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.JavaElementType.*
import com.intellij.psi.impl.source.tree.TreeElement
import team.jlm.dependency.DependencyProviderType
import team.jlm.utils.psi.getPsiJavaFile

fun getDependencyList(fileString: String, project: Project): List<Dependency> {
    val psiClass = getPsiJavaFile(project, fileString)
    val result = ArrayList<Dependency>(psiClass.textLength / 380)//估算类中大约有多少个依赖项
    psiClass.containingFile.accept(
        DependVisitor(
            { dependElementInThisFile: PsiElement, dependElement: PsiElement ->
                run {
                    if (dependElement !is PsiClass) return@run
//                    val dependencyType = dependElementInThisFile.dependencyType
                    val dependencyProviderType = DependencyProviderType.OTHER
                    val dependencyRange = dependElementInThisFile.textRange
                    val dependencyStr: String =
                        fileString.subSequence(dependencyRange.startOffset, dependencyRange.endOffset).toString()
                    result.add(Dependency(dependencyProviderType, dependencyStr, dependElement.qualifiedName))
                }
            }, DependencyVisitorFactory.VisitorOptions.fromSettings(project)
        )
    )
//    ForwardDependenciesBuilder.analyzeFileDependencies(psiClass.containingFile as PsiJavaFile)
//    { dependElementInThisFile: PsiElement, dependElement: PsiElement ->
//        run {
//            if (dependElement !is PsiClass) return@run
//            val dependencyType = dependElementInThisFile.dependencyType
//            val dependencyRange = dependElementInThisFile.textRange
//            val dependencyStr: String =
//                fileString.subSequence(dependencyRange.startOffset, dependencyRange.endOffset).toString()
//            result.add(Dependency(dependencyType, dependencyStr, dependElement.qualifiedName))
//        }
//    }
    return result
}

val PsiElement.dependencyProviderType: DependencyProviderType
    get() {
        if (this !is TreeElement)
            return DependencyProviderType.OTHER
//        logger.debug{ ("$this, ${this.treeParent.elementType}, ${this.treeParent.treeParent.elementType}")
        val dependencyProviderType = when (this.elementType) {
            JAVA_CODE_REFERENCE ->
                when (this.treeParent.elementType) {
                    EXTENDS_LIST -> DependencyProviderType.EXTENDS
                    IMPLEMENTS_LIST -> DependencyProviderType.IMPLEMENT
                    IMPORT_STATEMENT -> DependencyProviderType.IMPORT_STATEMENT
                    IMPORT_LIST -> DependencyProviderType.IMPORT_LIST
                    IMPORT_STATIC_STATEMENT -> DependencyProviderType.IMPORT_STATIC_STATEMENT
                    IMPORT_STATIC_REFERENCE -> DependencyProviderType.IMPORT_STATIC_FIELD
                    ANNOTATION -> DependencyProviderType.ANNOTATION
                    TYPE -> when (this.treeParent.treeParent.elementType) {
                        FIELD -> DependencyProviderType.CONTAIN
                        PARAMETER -> DependencyProviderType.PARAMETER
                        LOCAL_VARIABLE -> DependencyProviderType.USE
                        CLASS_OBJECT_ACCESS_EXPRESSION -> DependencyProviderType.CLASS_OBJECT_ACCESS
                        else -> DependencyProviderType.OTHER
                    }

                    NEW_EXPRESSION -> DependencyProviderType.CREATE
                    else -> DependencyProviderType.OTHER
                }

            REFERENCE_EXPRESSION ->
                when (this.treeParent.elementType) {
                    REFERENCE_EXPRESSION -> when (this.treeParent.treeParent.elementType) {
                        METHOD_CALL_EXPRESSION -> DependencyProviderType.STATIC_METHOD
                        else -> DependencyProviderType.STATIC_FIELD
                    }

                    else -> DependencyProviderType.OTHER
                }

            else -> DependencyProviderType.OTHER
        }
        return dependencyProviderType
    }

