package team.jlm.coderefactor.plugin.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiField
import com.intellij.psi.PsiJvmMember
import com.intellij.psi.PsiMember
import com.intellij.refactoring.JavaRefactoringFactory
import com.xyzboom.algorithm.graph.GEdge
import com.xyzboom.algorithm.graph.Tarjan
import team.jlm.coderefactor.code.DependencyType
import team.jlm.coderefactor.code.IG
import team.jlm.psi.cache.PsiMemberCacheImpl
import team.jlm.utils.getAllClassesInProject

val importDependencySet = HashSet<DependencyType>(
    arrayListOf(
        DependencyType.IMPORT_LIST,
        DependencyType.IMPORT_STATIC_STATEMENT,
        DependencyType.IMPORT_STATEMENT,
        DependencyType.IMPORT_STATIC_REFERENCE,
        DependencyType.STATIC_REFERENCE,
        DependencyType.STATIC_CALL,
    )
)

class CycleDependencyAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val classes = getAllClassesInProject(project)
        classes.removeIf {
            val path = it.containingFile.originalFile.containingDirectory.toString()
            it.containingClass != null /*||
                    path.contains("test", true) || path.contains("docs", false)
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
            } else {
                println("${row[0]} ${row[1]}")
            }
        }
        for (p in ig.adjList) {
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
        }
        println()
    }

    private fun handleOnlyStaticFieldsInOneClass(
        dpList: MutableList<DependencyType>,
        ig: IG,
        edge: GEdge<String>,
        project: Project,
    ) {
        val staticFields = ArrayList<PsiJvmMember>()
        for (i in dpList.indices) {
            val cache = ig.dependencyPsiMap[edge]?.get(i) ?: continue
            if (cache is PsiMemberCacheImpl) {
                staticFields.add(cache.getPsi(project))
            }
        }
        if (staticFields.isEmpty()) return
        val move = JavaRefactoringFactory.getInstance(project)
            .createMoveMembers(
                staticFields.toArray(arrayOf<PsiMember>()),
                edge.nodeFrom.data, "public"
            )
        move.run()
    }
}
