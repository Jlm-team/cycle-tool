package team.jlm.coderefactor.plugin.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.packageDependencies.DependencyVisitorFactory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiJvmMember
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiModifier
import com.intellij.refactoring.JavaRefactoringFactory
import com.xyzboom.algorithm.graph.GEdge
import com.xyzboom.algorithm.graph.Tarjan
import team.jlm.coderefactor.code.DependVisitor
import team.jlm.coderefactor.code.DependencyType
import team.jlm.coderefactor.code.IG
import team.jlm.psi.cache.PsiMemberCacheImpl
import team.jlm.utils.getAllClassesInProject

val importDependencySet = HashSet<DependencyType>(
    arrayListOf(
        DependencyType.IMPORT_LIST,
        DependencyType.IMPORT_STATIC_STATEMENT,
        DependencyType.IMPORT_STATEMENT,
        DependencyType.IMPORT_STATIC_FIELD,
        DependencyType.STATIC_FIELD,
        DependencyType.STATIC_METHOD,
    )
)

class CycleDependencyAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val classes = getAllClassesInProject(project)
        classes.removeIf {
            val path = it.containingFile.originalFile.containingDirectory.toString()
            it.containingClass != null ||
                    path.contains("after", true) /*|| path.contains("docs", false)
                    || path.contains("examples", true)*/
        }
        val ig = IG(classes)
        val tarjan = Tarjan(ig)
        val result = tarjan.result
        for (row in result) {
            if (row.size != 2) {
                for (col in row) {
                    ig.delNode(col.data)
                }
            }
        }
        result.filter { it.size == 2 }.forEach {
            val row = it
            println("${row[0]} ${row[1]}")
            val edge0 = GEdge(row[0], row[1])
            val edge1 = GEdge(row[1], row[0])
            if (!handleEdge(ig, edge0, project))
                handleEdge(ig, edge1, project)
        }
        /*for (p in ig.adjList) {
            val edgePair = p.value
            for (edge in edgePair.edgeOut) {
                val dpList = ig.dependencyMap[edge] ?: continue
                val dpSet = HashSet(dpList)
                dpSet.removeAll(importDependencySet)
                if (dpSet.size == 0) {
                    handleOnlyStaticFieldsInOneClass(dpList, ig, edge, project)
                    println(edge)
                }
            }
        }*/
        println()
    }

    private fun handleEdge(
        ig: IG,
        edge: GEdge<String>,
        project: Project,
    ): Boolean {
        val dpList = ig.dependencyMap[edge] ?: return false
        val dpSet = HashSet(dpList)
        dpSet.removeAll(importDependencySet)
        return if (dpSet.size == 0) {
            println(edge)
            handleOnlyStaticFieldsInOneClass(dpList, ig, edge, project)
        } else false
    }

    private fun handleOnlyStaticFieldsInOneClass(
        dpList: MutableList<DependencyType>,
        ig: IG,
        edge: GEdge<String>,
        project: Project,
    ): Boolean {
        val staticFields = ArrayList<PsiJvmMember>()
        var nonStatic = false
        for (i in dpList.indices) {
            val cache = ig.dependencyPsiMap[edge]?.get(i) ?: continue
            if (cache is PsiMemberCacheImpl) {
                val directDependPsi = cache.getPsi(project)
                staticFields.add(directDependPsi)
                directDependPsi.accept(
                    DependVisitor(
                        { dependElementInThisFile: PsiElement, dependElement: PsiElement ->
                            println(dependElementInThisFile)
                            println(dependElement)
                            if (dependElement is PsiJvmMember) {
                                if (dependElement == directDependPsi.containingClass) {
                                    nonStatic = true
                                    return@DependVisitor
                                } else if (dependElement.hasModifierProperty(PsiModifier.STATIC)) {
                                    staticFields.add(dependElement)
                                } else if (!dependElement.hasModifierProperty(PsiModifier.PUBLIC)) {
                                    nonStatic = true
                                    return@DependVisitor
                                }
                            }
                        }, DependencyVisitorFactory.VisitorOptions.fromSettings(project)
                    )
                )
            }
        }
        if (staticFields.isEmpty() || nonStatic) return false
        val move = JavaRefactoringFactory.getInstance(project)
            .createMoveMembers(
                staticFields.toArray(arrayOf<PsiMember>()),
                edge.nodeFrom.data, "public"
            )
        move.run()
        return true
    }
}
