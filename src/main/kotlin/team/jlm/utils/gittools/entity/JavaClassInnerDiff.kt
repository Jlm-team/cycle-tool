package team.jlm.utils.gittools.entity

/**
 * 这个类代表类的变化的细节部分
 */
class JavaClassInnerDiff(
    val classSingleDiffType: JavaClassInnerDiffType,
    val before: String,
    val after: String
) {

}