package team.jlm.refactoring

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiJvmMember
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.refactoring.RefactoringImpl
import team.jlm.refactoring.move.staticA2B.MoveStaticMembersBetweenTwoClassesProcessor

class MoveStaticMembersBetweenTwoClasses(
    project: Project,
    refactoringScope: SearchScope = GlobalSearchScope.projectScope(project),
    prepareSuccessfulCallback: Runnable? = null,
    members0: Array<PsiJvmMember>,
    targetClassName0: String,
    members1: Array<PsiJvmMember>,
    targetClassName1: String,
) :
    RefactoringImpl<MoveStaticMembersBetweenTwoClassesProcessor>(
        MoveStaticMembersBetweenTwoClassesProcessor(
            project, refactoringScope, prepareSuccessfulCallback,
            members0, targetClassName0, members1, targetClassName1
        )
    )
