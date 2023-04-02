package team.jlm.dependency

import team.jlm.psi.cache.IPsiCache

class DependencyInfo(
    val posType: DependencyPosType,
    val type: DependencyType,
    val posPsi: IPsiCache<*> = IPsiCache.EMPTY,
    val psi: IPsiCache<*> = IPsiCache.EMPTY,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DependencyInfo) return false

        if (posType != other.posType) return false
        if (type != other.type) return false

        return true
    }

    override fun hashCode(): Int {
        var result = posType.hashCode()
        result = 31 * result + type.hashCode()
        return result
    }

    override fun toString(): String {
        return "Dependency(pos: $posType, type: $type)"
    }

    companion object {
        @JvmStatic
        val Extends = DependencyInfo(DependencyPosType.EXTENDS, DependencyType.EXTENDS)
    }
}