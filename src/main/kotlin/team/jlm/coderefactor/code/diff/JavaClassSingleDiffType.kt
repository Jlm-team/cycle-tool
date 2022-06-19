package team.jlm.coderefactor.code.diff

enum class JavaClassSingleDiffType {
    METHOD_MODIFY,
    METHOD_ADD,
    METHOD_DELETE,
    FIELD_MODIFY,
    FIELD_ADD,
    FIELD_DELETE,
    ANNOTATION_ADD,
    ANNOTATION_DELETE
}