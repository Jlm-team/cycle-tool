package team.jlm.coderefactor.code

class Dependency(val type: DependencyType, val dependencyText: String) {
    override fun toString(): String {
        return "Dependency(type=$type, dependencyText='$dependencyText')"
    }
}