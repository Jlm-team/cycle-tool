package team.jlm.coderefactor.code

/**
 * 类间依赖关系枚举
 * @see <a href="https://github.com/multilang-depends/depends">depends</a>
 */
enum class DependencyType {
    /**
     * Import is a relation between files. It indicates that File A includes or imports from File B.
     * 文件间的依赖关系，例如文件A导入了文件B，具有方向性。
     */
    IMPORT,

    /**
     * Contain is a relation between code elements(entities).
     * It indicates that Element A contains Element B. For example,
     * A class could contains a member, A function could contains a variable, etc.
     * 元素间的依赖关系，类A包含了类B表示类A以类B的对象作为成员
     */
    CONTAIN,

    /**
     * Parameter is a relation of function/method and it's parameters.
     * 函数和其参数的关系
     */
    PARAMETER,

    /**
     * Call is a relation of function/method invocation.
     * 代码块和代码块内调用的方法的关系
     */
    CALL,

    /**
     * Return is a relation of function/method and it's return type(s).
     * 函数和其返回值类型的关系
     */
    RETURN,

    /**
     * Throw is similar as Return, it is a relation of function/method and it's throws type(s).
     * 函数和函数抛出的异常类型的关系
     */
    THROW,

    /**
     * Implement is a relation between a function or class implementation, and it's prototype/interface.
     * 类和类实现的接口的关系
     */
    IMPLEMENT,

    /**
     * Extends means inherit of OO language
     * 类和类继承的类的关系，即继承关系
     */
    EXTENDS,

    /**
     * Create is a relation of the function and objects it created
     * 函数和函数内创建的对象的关系
     */
    CREATE,

    /**
     * Cast is a relation of an expression and the casted types
     * 表达式和强制类型转换的类型的关系
     */
    CAST,

    /**
     * Use is a relation of an expression and the types/variables used by the expression.
     * 表达式和表达式使用的变量或类型的关系
     */
    USE,

    /**
     * The Annotation relation in Java
     * java类和java注解的关系
     */
    ANNOTATION,
    DEPEND
}