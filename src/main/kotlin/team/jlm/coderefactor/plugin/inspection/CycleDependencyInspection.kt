package team.jlm.coderefactor.plugin.inspection

import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool
import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.packageDependencies.ForwardDependenciesBuilder
import com.intellij.psi.*
import com.intellij.util.containers.stream
import com.xyzboom.algorithm.graph.GEdge
import com.xyzboom.algorithm.graph.GNode
import team.jlm.coderefactor.code.DependencyType
import team.jlm.coderefactor.code.IG
import team.jlm.coderefactor.code.dependencyType
import kotlin.streams.toList

class CycleDependencyInspection : AbstractBaseJavaLocalInspectionTool() {
    companion object {
        const val DESCRIPTION_TEMPLATE = "Cycle dependency"

    }

    val ig = IG(ArrayList())
    val cycles = HashSet<Pair<PsiClass, PsiClass>>()
    val addedCycles = HashSet<Pair<String?, String?>>()
    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
    ): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitClass(clazz: PsiClass?) {
                if (clazz == null) return
                if (clazz.containingClass != null) return
                ForwardDependenciesBuilder.analyzeFileDependencies(clazz.containingFile as PsiJavaFile)
                { dependElement: PsiElement, selfElement: PsiElement ->
                    run {
                        if (selfElement is PsiClass) {
                            val selfName = selfElement.name
                            val clazzName = clazz.name
                            if (selfName == null || clazzName == null) {
                                return@run
                            }
                            ig.addEdge(clazzName, selfName, DependencyType.DEPEND)
                            val edgePair = ig.adjList[GNode(selfName)] ?: return@run
                            if (edgePair.edgeOut.contains(GEdge(GNode(selfName), GNode(clazzName)))) {
                                print("detected cycle: ")
                                println("${selfElement.name} --> ${clazz.name}")
                                if (!addedCycles.contains(Pair(clazz.qualifiedName, selfElement.qualifiedName))) {
                                    addedCycles.add(Pair(clazz.qualifiedName, selfElement.qualifiedName))
                                    addedCycles.add(Pair(selfElement.qualifiedName, clazz.qualifiedName))
                                    cycles.add(Pair(clazz, selfElement))
                                }
                            }
                            println("${selfElement.name} --> ${clazz.name} : ${dependElement.dependencyType}")
                        }
                    }
                }
            }
        }
    }

    override fun inspectionFinished(session: LocalInspectionToolSession, holder: ProblemsHolder) {
        val psiFile = session.file
        if (psiFile !is PsiJavaFile) return
        val classes = psiFile.classes.stream().map {
            it.qualifiedName ?: ""
        }.toArray()
        for (p in cycles) {
            val i1 = p.first.nameIdentifier
            if (i1 != null && classes.contains(p.first.qualifiedName)) {
                holder.registerProblem(i1, DESCRIPTION_TEMPLATE, null)
            }
            val i2 = p.second.nameIdentifier
            if (i2 != null && classes.contains(p.second.qualifiedName)) {
                holder.registerProblem(i2, DESCRIPTION_TEMPLATE, null)
            }
        }
        super.inspectionFinished(session, holder)
    }
}