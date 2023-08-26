package team.jlm.refactoring.replace

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiNewExpression
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.SearchScope
import team.jlm.refactoring.BaseRefactoring

class ReplaceByReflectRefactoring(
    project: Project,
    refactoringScope: SearchScope = GlobalSearchScope.projectScope(project),
    prepareSuccessfulCallback: Runnable? = null,
    psiNewExpression: PsiNewExpression,
) : BaseRefactoring<ReplaceByReflectProcessor>(
    ReplaceByReflectProcessor(project, refactoringScope, prepareSuccessfulCallback, psiNewExpression)
)