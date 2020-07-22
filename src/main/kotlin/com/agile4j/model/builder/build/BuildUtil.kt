package com.agile4j.model.builder.build

import com.agile4j.model.builder.ModelBuildException
import com.agile4j.model.builder.build.AccompaniesAndTargetsDTO.Companion.emptyDTO
import com.agile4j.model.builder.build.BuildContext.tToAHolder
import com.agile4j.model.builder.delegate.ITargetDelegate.ScopeKeys.modelBuilderScopeKey
import com.agile4j.model.builder.delegate.ModelBuilderDelegate
import com.agile4j.utils.util.ArrayUtil
import com.agile4j.utils.util.CollectionUtil
import com.agile4j.utils.util.MapUtil
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.createType
import kotlin.reflect.jvm.javaType
import kotlin.reflect.jvm.jvmErasure

/**
 * abbreviations:
 * T        target
 * IOA      accompanyIndex or accompany
 * @author liurenpeng
 * Created on 2020-07-09
 */

internal var Any.buildInModelBuilder : ModelBuilder by ModelBuilderDelegate()

internal fun <IOA, T : Any> buildTargets(
    buildMultiPair: BuildMultiPair<KClass<T>>,
    ioas: Collection<IOA>
): Set<T> {
    val dto = buildAccompaniesAndTargets(buildMultiPair, ioas)
    if (dto.isEmpty) return emptySet()
    injectModelBuilder(buildMultiPair.modelBuilder, dto.targets)
    injectAccompaniesAndTargets(buildMultiPair.modelBuilder, dto)
    return dto.targets
}

/**
 * @return [property] is TargetClass or Collection<TargetClass>
 */
internal fun isTargetRelatedProperty(property: KProperty<*>) : Boolean {
    val clazz = property.returnType.jvmErasure
    if (isTargetClass(clazz)) return true
    val isCollection = Collection::class.java.isAssignableFrom(clazz.java)
    if (!isCollection) return false

    val pType = property.returnType.javaType as? ParameterizedType ?: return false
    val actualTypeArguments = pType.actualTypeArguments
    if (ArrayUtil.isEmpty(actualTypeArguments)) return false
    return isTargetType(actualTypeArguments[0])
}

private fun isTargetClass(clazz: KClass<*>) = tToAHolder.keys.contains(clazz)

private fun isTargetType(type: Type) = tToAHolder.keys.map { it.java }.contains(type)

private fun <IOA, T : Any> buildAccompaniesAndTargets(
    buildMultiPair: BuildMultiPair<KClass<T>>,
    sources: Collection<IOA>
): AccompaniesAndTargetsDTO<T> {
    if (CollectionUtil.isEmpty(sources)) {
        return emptyDTO()
    }
    val targetClazz = buildMultiPair.targetClazz
    if (!BuildContext.tToAHolder.keys.contains(targetClazz)) {
        throw ModelBuildException("unregistered targetClass:$targetClazz")
    }

    val accompanyClazz = BuildContext.tToAHolder[targetClazz]!!
    val indexToAccompanyMap : Map<out Any, Any> = buildAccompanyMap(accompanyClazz, sources)
    val accompanyToTargetMap = indexToAccompanyMap.values
        .map { accompany ->  buildTarget(targetClazz, accompanyClazz, accompany) to accompany }.toMap()
    return AccompaniesAndTargetsDTO(indexToAccompanyMap, accompanyToTargetMap)
}

private data class AccompaniesAndTargetsDTO<T> (
    val indexToAccompanyMap: Map<out Any, Any>,
    val targetToAccompanyMap: Map<T, Any>,

    val targets: Set<T> = targetToAccompanyMap.keys.toSet(),
    val isEmpty: Boolean = MapUtil.isEmpty(targetToAccompanyMap) || MapUtil.isEmpty(indexToAccompanyMap)
) {
    companion object {
        fun <T> emptyDTO() : AccompaniesAndTargetsDTO<T> = AccompaniesAndTargetsDTO(emptyMap(), emptyMap())
    }
}

private fun <T : Any> buildTarget(
    targetClazz: KClass<T>,
    accompanyClazz: KClass<*>,
    accompany: Any
): T {
    return targetClazz.constructors.stream()
        .filter { it.parameters.size == 1 }
        .filter { it.parameters[0].type == accompanyClazz.createType() }
        .findFirst()
        .map { it.call(accompany) }
        .orElseThrow { ModelBuildException("no suitable constructor was found for targetClazz: $targetClazz") }
}

/**
 * @return accompanyIndex -> accompany
 */
@Suppress("UNCHECKED_CAST")
private fun <IOA> buildAccompanyMap(
    accompanyClazz: KClass<*>,
    indies: Collection<IOA>
): Map<out Any, Any> {
    return if (accompanyClazz.isInstance(indies.toList()[0])) {
        // buildByAccompany
        val accompanyIndexer = BuildContext.indexerHolder[accompanyClazz] as (IOA) -> Any
        indies.map { accompanyIndexer.invoke(it) to it }.toMap() as Map<out Any, Any>
    } else { // TODO 类型判断是否为accompanyIndex
        // buildByAccompanyIndex
        val accompanyBuilder = BuildContext.builderHolder[accompanyClazz] as (Collection<IOA>) -> Map<Any, Any>

        // 缓存中可能有，先从缓存查一下
        val modelBuilder = modelBuilderScopeKey.get()
        val joinCacheMap = modelBuilder?.getJoinCacheMap(accompanyClazz)
        if (modelBuilder != null && MapUtil.isNotEmpty(joinCacheMap)) {
            // 有缓存
            val cached = joinCacheMap!!.filter { indies.contains(it.key as IOA) }
            val unCachedIndies = indies.filter { !cached.keys.contains(it as Any) }

            val result = mutableMapOf<Any, Any>()
            result.putAll(cached)
            if (CollectionUtil.isNotEmpty(unCachedIndies)) {
                result.putAll(accompanyBuilder.invoke(unCachedIndies))
            }
            result.toMap()
        } else {
            // 没缓存，全部直接查
            accompanyBuilder.invoke(indies)
        }
    }
}

/**
 * 把modelBuilder注入到targets中
 */
private fun <T : Any> injectModelBuilder(
    modelBuilder: ModelBuilder,
    targets: Set<T>
) = targets.forEach { it.buildInModelBuilder = (modelBuilder) }

/**
 * 把accompanies和targets注入到modelBuilder中
 */
private fun <T : Any> injectAccompaniesAndTargets(
    modelBuilder: ModelBuilder,
    dto: AccompaniesAndTargetsDTO<T>
) {
    modelBuilder.indexToAccompanyMap.putAll(dto.indexToAccompanyMap)
    modelBuilder.targetToAccompanyMap.putAll(dto.targetToAccompanyMap)

    modelBuilder.putAllJoinCacheMap(modelBuilder.accompanyClazz, modelBuilder.indexToAccompanyMap)
    modelBuilder.putAllJoinTargetCacheMap(modelBuilder.targetClazz, modelBuilder.indexToTargetMap)
}

