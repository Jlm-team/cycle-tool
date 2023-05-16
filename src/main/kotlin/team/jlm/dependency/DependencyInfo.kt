package team.jlm.dependency

import team.jlm.psi.cache.IPsiCache

/**
 * Dependency info
 *
 * @property posType 依赖所处位置的依赖类型
 * @property type 依赖语句的依赖类型
 * @property posPsi 依赖所处位置的成员的缓存
 * @property psi 依赖的缓存
 * @constructor Create empty Dependency info
 */
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