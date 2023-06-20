package team.jlm.dependency

/**
 * Top dependency type
 *
 * 指示依赖点所处的位置
 *
 * @property static 依赖是否为静态
 */
enum class DependencyUserType(
    val static: Boolean = false,
) {
    /**
     * Import
     *
     * 依赖点位于import语句
     * ```java
     * import other.ClassB;
     * ```
     */
    IMPORT,

    /**
     * Import Static
     *
     * 静态的import语句
     * @see [IMPORT]
     */
    IMPORT_STATIC(true),

    /**
     * Field
     *
     * 位于域声明或赋值语句内的依赖
     * ```java
     * class ClassA {
     *      private float field1 = ClassB.getFloat();
     * }
     * ```
     */
    FIELD,

    /**
     * Field Static
     *
     * 静态的域
     * @see [FIELD]
     */
    FIELD_STATIC(true),

    /**
     * Func
     *
     * 函数或方法内的依赖
     * ```java
     * class ClassA {
     *      public static void func() {
     *          float f1 = ClassB.getFloat();
     *      }
     * }
     * ```
     */
    METHOD,

    /**
     * Func Static
     *
     * 静态函数或方法内的依赖
     * @see [METHOD]
     */
    METHOD_STATIC(true),

    /**
     * Extends
     *
     * 继承关系
     * ```java
     * class ClassA extends ClassB {}
     * ```
     */
    EXTENDS,

    /**
     * Implement
     *
     * 实现接口
     * ```java
     * class ClassA implements IInterfaceA {}
     * ```
     */
    IMPLEMENT,

    OTHER;

    val isMethod: Boolean
        get() = this == METHOD || this == METHOD_STATIC

    val isField: Boolean
        get() = this == FIELD || this == FIELD_STATIC

    val isMember: Boolean
        get() = isMethod || isField
}