package team.jlm.utils.gittools.entity

class JavaClassDiff() {
    constructor(classDiffType: JavaClassDiffType, classSingleDiff: java.util.ArrayList<JavaClassInnerDiff>) : this() {
        this.classDiffType = classDiffType
        this.classSingleDiff = classSingleDiff
    }

    var classDiffType: JavaClassDiffType = JavaClassDiffType.CLASS_MODIFY
    var classSingleDiff: ArrayList<JavaClassInnerDiff> = ArrayList()
}