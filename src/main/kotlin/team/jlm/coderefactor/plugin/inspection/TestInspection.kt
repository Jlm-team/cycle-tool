package team.jlm.coderefactor.plugin.inspection

import com.intellij.codeInspection.*
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.ui.DocumentAdapter
import com.intellij.util.IncorrectOperationException
import com.siyeh.ig.psiutils.ExpressionUtils
import org.jetbrains.annotations.NonNls
import java.awt.FlowLayout
import java.util.*
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.event.DocumentEvent

class TestInspection : AbstractBaseJavaLocalInspectionTool() {
    companion object {
        val QUICK_FIX_NAME = "SDK: " +
                InspectionsBundle.message("inspection.comparing.references.use.quickfix")
        private val LOG = Logger.getInstance("#com.intellij.codeInspection.ComparingReferencesInspection")

    }
    // Defines the text of the quick fix intention

    private val myQuickFix = CriQuickFix()

    // This string holds a list of classes relevant to this inspection.
    @NonNls
    var CHECKED_CLASSES = "java.lang.String;java.util.Date"

    /**
     * This method is called to get the panel describing the inspection.
     * It is called every time the user selects the inspection in preferences.
     * The user has the option to edit the list of [.CHECKED_CLASSES].
     *
     * @return panel to display inspection information.
     */
    override fun createOptionsPanel(): JComponent {
        val panel = JPanel(FlowLayout(FlowLayout.LEFT))
        val checkedClasses = JTextField(CHECKED_CLASSES)
        checkedClasses.document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(event: DocumentEvent) {
                CHECKED_CLASSES = checkedClasses.text
            }
        })
        panel.add(checkedClasses)
        return panel
    }

    /**
     * This method is overridden to provide a custom visitor.
     * that inspects expressions with relational operators '==' and '!='.
     * The visitor must not be recursive and must be thread-safe.
     *
     * @param holder     object for visitor to register problems found.
     * @param isOnTheFly true if inspection was run in non-batch mode
     * @return non-null visitor for this inspection.
     * @see JavaElementVisitor
     */
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {
            /**
             * This string defines the short message shown to a user signaling the inspection found a problem.
             * It reuses a string from the inspections bundle.
             */
            @NonNls
            private val DESCRIPTION_TEMPLATE = "SDK " +
                    InspectionsBundle.message("inspection.comparing.references.problem.descriptor")

            /**
             * Avoid defining visitors for both Reference and Binary expressions.
             *
             * @param psiReferenceExpression The expression to be evaluated.
             */
            override fun visitReferenceExpression(psiReferenceExpression: PsiReferenceExpression) {}

            /**
             * Evaluate binary psi expressions to see if they contain relational operators '==' and '!=', AND they contain
             * classes contained in CHECKED_CLASSES. The evaluation ignores expressions comparing an object to null.
             * IF this criteria is met, add the expression to the problems list.
             *
             * @param expression The binary expression to be evaluated.
             */
            override fun visitBinaryExpression(expression: PsiBinaryExpression) {
                super.visitBinaryExpression(expression)
                val opSign = expression.operationTokenType
                if (opSign === JavaTokenType.EQEQ || opSign === JavaTokenType.NE) {
                    // The binary expression is the correct type for this inspection
                    val lOperand = expression.lOperand
                    val rOperand = expression.rOperand
                    if (rOperand == null || ExpressionUtils.isNullLiteral(lOperand) || ExpressionUtils.isNullLiteral(
                            rOperand
                        )
                    ) {
                        return
                    }
                    // Nothing is compared to null, now check the types being compared
                    val lType = lOperand.type
                    val rType = rOperand.type
                    if (isCheckedType(lType) || isCheckedType(rType)) {
                        // Identified an expression with potential problems, add to list with fix object.
                        holder.registerProblem(
                            expression,
                            DESCRIPTION_TEMPLATE, myQuickFix
                        )
                    }
                }
            }

            /**
             * Verifies the input is the correct `PsiType` for this inspection.
             *
             * @param type The `PsiType` to be examined for a match
             * @return `true` if input is `PsiClassType` and matches one of the classes
             * in the [ComparingReferencesInspection.CHECKED_CLASSES] list.
             */
            private fun isCheckedType(type: PsiType?): Boolean {
                if (type !is PsiClassType) {
                    return false
                }
                val tokenizer = StringTokenizer(CHECKED_CLASSES, ";")
                while (tokenizer.hasMoreTokens()) {
                    val className = tokenizer.nextToken()
                    if (type.equalsToText(className)) {
                        return true
                    }
                }
                return false
            }
        }
    }

    /**
     * This class provides a solution to inspection problem expressions by manipulating the PSI tree to use 'a.equals(b)'
     * instead of '==' or '!='.
     */
    private class CriQuickFix : LocalQuickFix {
        /**
         * Returns a partially localized string for the quick fix intention.
         * Used by the test code for this plugin.
         *
         * @return Quick fix short name.
         */
        override fun getName(): String {
            return QUICK_FIX_NAME
        }

        /**
         * This method manipulates the PSI tree to replace 'a==b' with 'a.equals(b)' or 'a!=b' with '!a.equals(b)'.
         *
         * @param project    The project that contains the file being edited.
         * @param descriptor A problem found by this inspection.
         */
        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            try {
                val binaryExpression = descriptor.psiElement as PsiBinaryExpression
                val opSign = binaryExpression.operationTokenType
                val lExpr = binaryExpression.lOperand
                val rExpr = binaryExpression.rOperand ?: return
                val factory = JavaPsiFacade.getInstance(project).elementFactory
                val equalsCall = factory.createExpressionFromText("a.equals(b)", null) as PsiMethodCallExpression
                equalsCall.methodExpression.qualifierExpression!!.replace(lExpr)
                equalsCall.argumentList.expressions[0].replace(rExpr)
                val result = binaryExpression.replace(equalsCall) as PsiExpression
                if (opSign === JavaTokenType.NE) {
                    val negation = factory.createExpressionFromText("!a", null) as PsiPrefixExpression
                    negation.operand!!.replace(result)
                    result.replace(negation)
                }
            } catch (e: IncorrectOperationException) {
                LOG.error(e)
            }
        }

        override fun getFamilyName(): String {
            return name
        }
    }

}