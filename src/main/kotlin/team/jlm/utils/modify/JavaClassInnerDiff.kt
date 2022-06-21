package team.jlm.utils.modify

/**
 * 这个类代表类的变化的细节部分
 */
class JavaClassInnerDiff(
    val classSingleDiffType: JavaClassInnerDiffType,
    val before: String,
    val afte: String
) {

}