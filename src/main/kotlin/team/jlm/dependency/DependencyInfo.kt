package team.jlm.dependency

import team.jlm.psi.cache.INullablePsiCache

/**
 * Dependency info
 *
 * @property userType 依赖所处位置的依赖类型
 * @property providerType 依赖语句的依赖类型
 * @property userCache 依赖所处位置的成员的缓存
 * @property providerCache 依赖的缓存
 * @property userCache 依赖所处位置的缓存
 * @constructor Create empty Dependency info
 */
class DependencyInfo(
    val userType: DependencyUserType,
    val providerType: DependencyProviderType,
    val userCache: INullablePsiCache<*> = INullablePsiCache.EMPTY,
    val providerCache: INullablePsiCache<*> = INullablePsiCache.EMPTY,
    val posCache: INullablePsiCache<*> = INullablePsiCache.EMPTY
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DependencyInfo) return false

        if (userType != other.userType) return false
        return providerType == other.providerType
    }

    override fun hashCode(): Int {
        var result = userType.hashCode()
        result = 31 * result + providerType.hashCode()
        return result
    }

    override fun toString(): String {
        return "Dependency(pos: $userType, type: $providerType)"
    }

    companion object {
        @JvmStatic
        val Extends = DependencyInfo(DependencyUserType.EXTENDS, DependencyProviderType.EXTENDS)
    }
}