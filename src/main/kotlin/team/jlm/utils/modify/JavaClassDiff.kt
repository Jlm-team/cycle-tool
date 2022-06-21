package team.jlm.utils.modify

class JavaClassDiff() {
    constructor(classDiffType: JavaClassDiffType, classSingleDiff: java.util.ArrayList<JavaClassInnerDiff>) : this() {
        this.classDiffType = classDiffType
        this.classSingleDiff = classSingleDiff
    }

    var classDiffType: JavaClassDiffType = JavaClassDiffType.CLASS_MODIFY
    var classSingleDiff: ArrayList<JavaClassInnerDiff> = ArrayList()
}