package team.jlm.refactoring

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.util.PsiTreeUtil
import team.jlm.utils.psi.getAllClassesInJavaFile
import team.jlm.utils.psi.getAllJavaFilesInProject

fun handleDeprecatedMethod(project: Project): HashMap<String, Pair<String, String?>> {
    val javaFileList = getAllJavaFilesInProject(project)
    val deprecatedMap = HashMap<String, Pair<String, String?>>()
    javaFileList.forEach { javaFile ->
        val classes = getAllClassesInJavaFile(javaFile)
        classes.forEach lassLoop@{ cl ->
            cl.methods.forEach methodLoop@{ method ->
                val externalMethods = PsiTreeUtil.collectElementsOfType(method, PsiMethodCallExpression::class.java)
                externalMethods.forEach exMethodLoop@{ exMethod ->
                    val methodCall = exMethod.resolveMethod() ?: return@exMethodLoop
                    if (methodCall.containingClass != cl && methodCall.isDeprecated) {
                        methodCall.docComment?.let {
                            it.findTagByName("@deprecated")?.let { doc ->
                                deprecatedMap.put(
                                    cl.qualifiedName + "." + method.name,
                                    Pair(methodCall.name, doc.text)
                                )
                            } ?: deprecatedMap.put(
                                cl.qualifiedName + "." + method.name,
                                Pair(methodCall.name, null)
                            )
                        } ?: deprecatedMap.put(cl.qualifiedName + "." + method.name, Pair(methodCall.name, null))
                    }
                }
            }
        }
    }
    return deprecatedMap
}