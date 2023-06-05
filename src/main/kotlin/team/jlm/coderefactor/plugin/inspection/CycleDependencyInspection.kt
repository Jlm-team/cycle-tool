package team.jlm.coderefactor.plugin.inspection

import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool
import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.packageDependencies.ForwardDependenciesBuilder
import com.intellij.psi.*
import com.intellij.util.containers.stream
import team.jlm.utils.graph.GEdge
import team.jlm.utils.graph.GNode
import team.jlm.dependency.DependencyProviderType
import team.jlm.coderefactor.code.IG

import mu.KotlinLogging
import team.jlm.dependency.DependencyUserType

private val logger = KotlinLogging.logger{}

class CycleDependencyInspection : AbstractBaseJavaLocalInspectionTool() {
    companion object {
        const val DESCRIPTION_TEMPLATE = "Cycle dependency between %s and %s"

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
                            ig.addEdge(clazzName, selfName, DependencyProviderType.OTHER, DependencyUserType.OTHER)
                            val edgePair = ig.adjList[GNode(selfName)] ?: return@run
                            if (edgePair.edgeOut.contains(GEdge(GNode(selfName), GNode(clazzName)))) {
                                print("detected cycle: ")
                                logger.debug { "${selfElement.name} --> ${clazz.name}" }
                                if (!addedCycles.contains(Pair(clazz.qualifiedName, selfElement.qualifiedName))) {
                                    addedCycles.add(Pair(clazz.qualifiedName, selfElement.qualifiedName))
                                    addedCycles.add(Pair(selfElement.qualifiedName, clazz.qualifiedName))
                                    cycles.add(Pair(clazz, selfElement))
                                }
                            }
//                            logger.debug{ "${selfElement.name} --> ${clazz.name} : ${dependElement.dependencyType}")
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
            logger.debug { "i1 ${p.first.containingFile == psiFile}" }
            if (i1 != null && classes.contains(p.first.qualifiedName)) {
                holder.registerProblem(i1, DESCRIPTION_TEMPLATE.format(p.first.name, p.second.name), null)
            }
            val i2 = p.second.nameIdentifier
            logger.debug { "i2 ${p.second.containingFile == psiFile}" }
            if (i2 != null && classes.contains(p.second.qualifiedName)) {
                holder.registerProblem(i2, DESCRIPTION_TEMPLATE.format(p.first.name, p.second.name), null)
            }
        }
        super.inspectionFinished(session, holder)
    }
}