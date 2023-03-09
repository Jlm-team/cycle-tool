package team.jlm.coderefactor.code

import com.intellij.openapi.project.Project
import com.intellij.packageDependencies.DependencyVisitorFactory
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.JavaElementType.*
import com.intellij.psi.impl.source.tree.TreeElement
import team.jlm.utils.getPsiJavaFile

fun getDependencyList(fileString: String, project: Project): List<Dependency> {
    val psiClass = getPsiJavaFile(project, fileString)
    val result = ArrayList<Dependency>(psiClass.textLength / 380)//估算类中大约有多少个依赖项
    psiClass.containingFile.accept(
        DependVisitor(
            { dependElementInThisFile: PsiElement, dependElement: PsiElement ->
                run {
                    if (dependElement !is PsiClass) return@run
//                    val dependencyType = dependElementInThisFile.dependencyType
                    val dependencyType = DependencyType.OTHER
                    val dependencyRange = dependElementInThisFile.textRange
                    val dependencyStr: String =
                        fileString.subSequence(dependencyRange.startOffset, dependencyRange.endOffset).toString()
                    result.add(Dependency(dependencyType, dependencyStr, dependElement.qualifiedName))
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

val PsiElement.dependencyType: DependencyType
    get() {
        if (this !is TreeElement)
            return DependencyType.OTHER
//        logger.debug{ ("$this, ${this.treeParent.elementType}, ${this.treeParent.treeParent.elementType}")
        val dependencyType = when (this.elementType) {
            JAVA_CODE_REFERENCE ->
                when (this.treeParent.elementType) {
                    EXTENDS_LIST -> DependencyType.EXTENDS
                    IMPLEMENTS_LIST -> DependencyType.IMPLEMENT
                    IMPORT_STATEMENT -> DependencyType.IMPORT_STATEMENT
                    IMPORT_LIST -> DependencyType.IMPORT_LIST
                    IMPORT_STATIC_STATEMENT -> DependencyType.IMPORT_STATIC_STATEMENT
                    IMPORT_STATIC_REFERENCE -> DependencyType.IMPORT_STATIC_FIELD
                    ANNOTATION -> DependencyType.ANNOTATION
                    TYPE -> when (this.treeParent.treeParent.elementType) {
                        FIELD -> DependencyType.CONTAIN
                        PARAMETER -> DependencyType.PARAMETER
                        LOCAL_VARIABLE -> DependencyType.USE
                        CLASS_OBJECT_ACCESS_EXPRESSION -> DependencyType.CLASS_OBJECT_ACCESS
                        else -> DependencyType.OTHER
                    }

                    NEW_EXPRESSION -> DependencyType.CREATE
                    else -> DependencyType.OTHER
                }

            REFERENCE_EXPRESSION ->
                when (this.treeParent.elementType) {
                    REFERENCE_EXPRESSION -> when (this.treeParent.treeParent.elementType) {
                        METHOD_CALL_EXPRESSION -> DependencyType.STATIC_METHOD
                        else -> DependencyType.STATIC_FIELD
                    }

                    else -> DependencyType.OTHER
                }

            else -> DependencyType.OTHER
        }
        return dependencyType
    }

