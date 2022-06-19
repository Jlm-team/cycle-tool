package team.jlm.utils.gittools.tools

import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.Change
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiJavaFile
import team.jlm.utils.getAllClassesInJavaFile
import team.jlm.utils.getPsiJavaFile
import team.jlm.utils.gittools.entity.JavaClassDiff
import team.jlm.utils.gittools.entity.JavaClassDiffType
import team.jlm.utils.gittools.entity.JavaClassInnerDiff
import team.jlm.utils.gittools.entity.JavaClassInnerDiffType


fun getClassDiff(change: Change, project: Project) :ArrayList<JavaClassDiff>{
    val res = ArrayList<JavaClassDiff>()
    val newJavaText = change.afterRevision?.content
    val oldJavaText = change.beforeRevision?.content
    val newPsiFile = newJavaText?.let { getPsiJavaFile(project, it) } as PsiJavaFile
    val oldPsiFile = oldJavaText?.let { getPsiJavaFile(project, it) } as PsiJavaFile
    val newClasses = getAllClassesInJavaFile(newPsiFile)
    val oldClases = getAllClassesInJavaFile(oldPsiFile)

    for (i in newClasses) {
        if (isClassContain(i, oldClases)) {
            //TODO 判断类是否改变
        }
        else{
            val innerList = ArrayList<JavaClassInnerDiff>()
            for(m in i.allMethods){
                innerList.add(JavaClassInnerDiff(JavaClassInnerDiffType.METHOD_ADD,"",m.text))
            }
            for(f in i.allFields){
                innerList.add(JavaClassInnerDiff(JavaClassInnerDiffType.FIELD_ADD,"",f.text))
            }
            for(a in i.annotations){
                innerList.add(JavaClassInnerDiff(JavaClassInnerDiffType.ANNOTATION_ADD,"",i.text))
            }
            res.add(JavaClassDiff(JavaClassDiffType.CLASS_ADD,innerList))
        }
    }
    return res
}


fun isClassContain(c: PsiClass, container: List<PsiClass>): Boolean {
    for (i in container) {
        if (i.name == c.name)
            return true
    }
    return false
}