package team.jlm.utils.gittools.entity

import com.github.difflib.patch.DeltaType
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiJavaFile
import team.jlm.utils.getPsiJavaFile


class JavaText(
    val Lines: ArrayList<String>,
    val project: Project
) {
    private var filePsi: PsiJavaFile
    private val text: String
    var elements: ArrayList<TextElement>

    init {
        text = text()
        filePsi = getPsiJavaFile(project, text)
        elements = ArrayList<TextElement>()
    }


    data class TextElement(
        val element: PsiElement,
        val type: DeltaType
    )

    fun text(): String {
        val sb = StringBuilder()
        for (i in Lines)
            if (i == "")
                continue
            else {
                if (i.endsWith(",") or i.endsWith("."))
                    sb.append(i)
                else
                    sb.append(i).append("\n")
            }
        return sb.toString()
    }

    fun getRowElement(str: String): PsiElement? {
        return this.filePsi.findElementAt(text.indexOf(str))
    }

    fun sloveElement(line:String,type: DeltaType){
        this.elements.add(TextElement(getRowElement(line)!!,type))
    }

}