package team.jlm.refactoring.move.staticA2B

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiJvmMember
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.SearchScope
import team.jlm.refactoring.BaseRefactoring
import team.jlm.refactoring.BaseRefactoringProcessor
import team.jlm.refactoring.MultiRefactoringProcessor

/**
 * Move static members between two classes
 *
 * @constructor
 *
 * @param project
 * @param refactoringScope
 * @param prepareSuccessfulCallback
 * @param members0
 * @param targetClassName0
 * @param members1
 * @param targetClassName1
 * @see [MoveStaticMembersBetweenTwoClassesProcessor]
 */
class MoveStaticMembersBetweenTwoClasses(
    project: Project,
    refactoringScope: SearchScope = GlobalSearchScope.projectScope(project),
    prepareSuccessfulCallback: Runnable? = null,
    members0: Array<PsiJvmMember>,
    targetClassName0: String,
    members1: Array<PsiJvmMember>,
    targetClassName1: String,
) : BaseRefactoring<MultiRefactoringProcessor>(
    MultiRefactoringProcessor(project, ArrayList<BaseRefactoringProcessor>().apply {
        add(
            MoveStaticMembersBetweenTwoClassesProcessor(
                project, refactoringScope, prepareSuccessfulCallback,
                members0, targetClassName0, members1, targetClassName1
            )
        )
    }, commandName = "Move members between two classes").apply {
        setPreviewUsages(true)
    }
)
