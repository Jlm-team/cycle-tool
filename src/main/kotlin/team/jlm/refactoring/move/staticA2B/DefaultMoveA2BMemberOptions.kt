package team.jlm.refactoring.move.staticA2B

import com.intellij.psi.PsiJvmMember
import com.intellij.psi.PsiModifier
import com.intellij.refactoring.move.moveMembers.MoveMembersOptions

class DefaultMoveA2BMemberOptions(
    private val members: Array<out PsiJvmMember>,
    private val className: String,
) : MoveMembersOptions {
    override fun getSelectedMembers() = members
    override fun getTargetClassName(): String = className
    override fun getMemberVisibility(): String = PsiModifier.PUBLIC
    override fun makeEnumConstant(): Boolean = false
}