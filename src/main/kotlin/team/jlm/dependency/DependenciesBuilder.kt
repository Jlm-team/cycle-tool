package team.jlm.dependency

import com.intellij.packageDependencies.DependencyVisitorFactory
import com.intellij.packageDependencies.JavaDependencyVisitorFactory
import com.intellij.psi.*
import com.intellij.psi.impl.source.tree.JavaElementType
import com.intellij.psi.util.elementType
import com.intellij.psi.util.parentsOfType
import com.intellij.refactoring.suggested.startOffset
import mu.KotlinLogging
import team.jlm.coderefactor.code.dependencyProviderType
import team.jlm.psi.cache.IPsiCache
import team.jlm.psi.cache.PsiMemberCacheImpl
import team.jlm.utils.psi.getOuterClass

private val logger = KotlinLogging.logger {}

class DependenciesBuilder {
    companion object {
        @JvmStatic
        private val factory = JavaDependencyVisitorFactory()

        @JvmStatic
        fun analyzeClassDependencies(
            clazz: PsiClass,
            dependencyFilter: (PsiClass) -> Boolean,
            processor: (PsiClass, DependencyProviderType, DependencyUserType, IPsiCache<*>, IPsiCache<*>) -> Unit,
        ) {
            analyzeClassDependencies(clazz,
                object : DependencyFilter {
                    override fun doFilter(providerClass: PsiClass): Boolean {
                        return dependencyFilter(providerClass)
                    }
                },
                object : DependencyProcessor {
                    override fun process(
                        providerClass: PsiClass,
                        providerType: DependencyProviderType,
                        userType: DependencyUserType,
                        providerPsiCache: IPsiCache<*>,
                        userPsiCache: IPsiCache<*>,
                    ) {
                        processor(
                            providerClass,
                            providerType,
                            userType,
                            providerPsiCache,
                            userPsiCache
                        )
                    }
                })
        }

        @JvmStatic
        fun analyzeClassDependencies(
            clazz: PsiClass,
            dependencyFilter: DependencyFilter,
            processor: DependencyProcessor,
        ) {
            val visitor = factory.getVisitor(inner@{ userEle: PsiElement, providerEle: PsiElement ->
//            logger.debug { "${dependEle.javaClass}" }
                val providerClass = providerEle.getOuterClass(false) ?: return@inner
                val userClass = userEle.getOuterClass(false) ?: return@inner
                if (userClass !== clazz) return@inner
                if (!dependencyFilter.doFilter(providerClass)) {
                    return@inner
                }
                val providerClassName = providerClass.qualifiedName ?: return@inner
                val userClassName = userClass.qualifiedName ?: return@inner
                var dependencyProviderType = DependencyProviderType.OTHER
                var dependencyUserType = DependencyUserType.OTHER
                var userPsiCache = IPsiCache.EMPTY
                var providerPsiCache = IPsiCache.EMPTY
                if (providerEle !is PsiClass) {
                    if (userEle.elementType == JavaElementType.METHOD_REF_EXPRESSION) {
                        dependencyProviderType = when (providerEle) {
                            is PsiMethod -> {
                                if (providerEle.modifierList.hasModifierProperty(PsiModifier.STATIC)) {
                                    DependencyProviderType.STATIC_METHOD
                                }
                                DependencyProviderType.NONSTATIC_METHOD
                            }

                            else -> {
                                DependencyProviderType.OTHER
                            }
                        }
                    } else if (providerEle is PsiJvmMember) {
                        if (providerEle.startOffset - providerClass.startOffset < 0) {
                            logger.debug { providerEle.startOffset }
                            logger.debug { providerClass.startOffset }
                            logger.debug { providerEle.text }
                            logger.debug { providerEle.parent.text }
                            logger.debug { providerEle.parent.parent.text }
                            logger.debug { providerEle.parent.parent.parent.text }
                        }
                        providerPsiCache = PsiMemberCacheImpl(
                            providerEle.startOffset - providerClass.startOffset,
                            providerClassName, providerEle.javaClass
                        )
                        dependencyProviderType = if (providerEle.hasModifierProperty(PsiModifier.STATIC)) {
                            when (providerEle) {
                                is PsiMethod -> DependencyProviderType.STATIC_METHOD
                                is PsiField -> DependencyProviderType.STATIC_FIELD
                                else -> DependencyProviderType.OTHER
                            }
                        } else {
                            when (providerEle) {
                                is PsiMethod -> DependencyProviderType.NONSTATIC_METHOD
                                is PsiField -> DependencyProviderType.NONSTATIC_FIELD
                                else -> DependencyProviderType.OTHER
                            }
                        }
                    }
                } else {
                    dependencyProviderType = userEle.dependencyProviderType
                    userPsiCache = IPsiCache.EMPTY
                }
                val fieldSet = userEle.parentsOfType<PsiField>().toSet()
                val methodSet = userEle.parentsOfType<PsiMethod>().toSet()
                if (fieldSet.isNotEmpty()) {
                    val fieldEle = fieldSet.first()
                    userPsiCache = PsiMemberCacheImpl(
                        fieldEle.startOffset - userClass.startOffset,
                        providerClassName,
                        fieldEle.javaClass
                    )
                    dependencyUserType =
                        if (fieldEle.modifierList?.hasModifierProperty(PsiModifier.STATIC) == true) {
                            DependencyUserType.FIELD_STATIC
                        } else {
                            DependencyUserType.FIELD
                        }
                } else if (methodSet.isNotEmpty()) {
                    val methodEle = methodSet.first()
                    userPsiCache = PsiMemberCacheImpl(
                        methodEle.startOffset - userClass.startOffset,
                        userClassName,
                        methodEle.javaClass
                    )
                    dependencyUserType = if (methodEle.modifierList.hasModifierProperty(PsiModifier.STATIC)) {
                        DependencyUserType.METHOD_STATIC
                    } else {
                        DependencyUserType.METHOD
                    }
                }
                processor.process(
                    providerClass, dependencyProviderType, dependencyUserType,
                    providerPsiCache, userPsiCache
                )
            }, DependencyVisitorFactory.VisitorOptions.fromSettings(clazz.project))
            clazz.accept(visitor)
        }
    }

    @FunctionalInterface
    interface DependencyFilter {
        fun doFilter(providerClass: PsiClass): Boolean
    }

    @FunctionalInterface
    interface DependencyProcessor {
        fun process(
            providerClass: PsiClass,
            providerType: DependencyProviderType,
            userType: DependencyUserType,
            providerPsiCache: IPsiCache<*>,
            userPsiCache: IPsiCache<*>,
        )
    }
}