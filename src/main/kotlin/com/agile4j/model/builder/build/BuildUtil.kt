package com.agile4j.model.builder.build

import com.agile4j.model.builder.ModelBuildException
import com.agile4j.model.builder.build.AccompaniesAndTargetsDTO.Companion.emptyDTO
import com.agile4j.model.builder.build.BuildContext.builderHolder
import com.agile4j.model.builder.build.BuildContext.indexerHolder
import com.agile4j.model.builder.build.BuildContext.isT
import com.agile4j.model.builder.build.BuildContext.tToAHolder
import com.agile4j.model.builder.delegate.JoinDelegate.ScopeKeys.nullableModelBuilder
import com.agile4j.model.builder.delegate.ModelBuilderDelegate
import com.agile4j.utils.util.ArrayUtil
import com.agile4j.utils.util.CollectionUtil
import com.agile4j.utils.util.MapUtil
import java.lang.reflect.ParameterizedType
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.createType
import kotlin.reflect.jvm.javaType
import kotlin.reflect.jvm.jvmErasure

/**
 * abbreviations:
 * T        target
 * IXA      index or accompany
 * @author liurenpeng
 * Created on 2020-07-09
 */

internal var Any.buildInModelBuilder : ModelBuilder by ModelBuilderDelegate()

internal fun <IXA: Any, T: Any> buildTargets(
    modelBuilder: ModelBuilder,
    tClazz: KClass<T>,
    ixas: Collection<IXA>
): Set<T> {
    val dto = buildDTO(tClazz, ixas)
    if (dto.isEmpty) return emptySet()
    injectModelBuilder(modelBuilder, dto.targets)
    injectAccompaniesAndTargets(modelBuilder, dto)
    return dto.targets
}

/**
 * @return [property] is TargetClass or Collection<TargetClass>
 */
internal fun isTargetRelatedProperty(property: KProperty<*>) : Boolean {
    val clazz = property.returnType.jvmErasure
    if (isT(clazz)) return true
    val isCollection = Collection::class.java.isAssignableFrom(clazz.java)
    if (!isCollection) return false

    val pType = property.returnType.javaType as? ParameterizedType ?: return false
    val actualTypeArguments = pType.actualTypeArguments
    if (ArrayUtil.isEmpty(actualTypeArguments)) return false
    return isT(actualTypeArguments[0])
}

private fun <IXA: Any, T: Any> buildDTO(
    tClazz: KClass<T>,
    ixas: Collection<IXA>
): AccompaniesAndTargetsDTO<T> {
    if (!isT(tClazz)) ModelBuildException.err("$tClazz is not target class")
    val aClazz = tToAHolder[tClazz]
    val iClazz = BuildContext.aToIHolder[aClazz]
    val ixaClazz = ixas.first()::class
    if (ixaClazz != iClazz && ixaClazz != aClazz)
        ModelBuildException.err("$ixaClazz is neither index class nor accompany class")

    if (CollectionUtil.isEmpty(ixas)) return emptyDTO()
    val iToA : Map<Any, Any> = buildIToA(aClazz!!, ixas)
    val tToA = iToA.values
        .map { accompany ->  buildTarget(tClazz, aClazz, accompany) to accompany }.toMap()
    return AccompaniesAndTargetsDTO(iToA, tToA)
}

private data class AccompaniesAndTargetsDTO<T> (
    val indexToAccompanyMap: Map<out Any, Any>,
    val targetToAccompanyMap: Map<T, Any>
) {
    val targets: Set<T> = targetToAccompanyMap.keys.toSet()
    val isEmpty: Boolean = MapUtil.isEmpty(targetToAccompanyMap) || MapUtil.isEmpty(indexToAccompanyMap)
    companion object {
        fun <T> emptyDTO() : AccompaniesAndTargetsDTO<T> = AccompaniesAndTargetsDTO(emptyMap(), emptyMap())
    }
}

private fun <T : Any> buildTarget(
    tClazz: KClass<T>,
    aClazz: KClass<*>,
    a: Any
): T = tClazz.constructors.stream()
        .filter { it.parameters.size == 1 }
        .filter { it.parameters[0].type == aClazz.createType() }
        .findFirst()
        .map { it.call(a) }
        .orElseThrow { ModelBuildException("no suitable constructor was found for targetClazz: $tClazz") }

/**
 * @return accompanyIndex -> accompany
 */
@Suppress("UNCHECKED_CAST")
private fun <IOA> buildIToA(
    accompanyClazz: KClass<*>,
    ioas: Collection<IOA>
): Map<Any, Any> {
    if (accompanyClazz.isInstance(ioas.first())) { // buildByAccompany. IOA is A
        val accompanyIndexer = indexerHolder[accompanyClazz] as (IOA) -> Any
        return ioas.map { accompanyIndexer.invoke(it) to it }.toMap() as Map<Any, Any>
    } else { // buildByAccompanyIndex. IOA is I
        val accompanyBuilder = builderHolder[accompanyClazz]
                as (Collection<IOA>) -> Map<Any, Any>

        val modelBuilder = nullableModelBuilder()
        val joinCacheMap = modelBuilder?.getJoinCacheMap(accompanyClazz)
        if (modelBuilder == null || MapUtil.isEmpty(joinCacheMap)) return accompanyBuilder.invoke(ioas)

        val cached = joinCacheMap!!.filter { ioas.contains(it.key as IOA) }
        val unCachedIndies = ioas.filter { !cached.keys.contains(it as Any) }
        return if (CollectionUtil.isEmpty(unCachedIndies)) cached
        else cached + accompanyBuilder.invoke(unCachedIndies)
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
    modelBuilder.iToA.putAll(dto.indexToAccompanyMap)
    modelBuilder.tToA.putAll(dto.targetToAccompanyMap)

    modelBuilder.putAllJoinCacheMap(modelBuilder.aClazz, modelBuilder.iToA)
    modelBuilder.putAllJoinTargetCacheMap(modelBuilder.tClazz, modelBuilder.iToT)
}

