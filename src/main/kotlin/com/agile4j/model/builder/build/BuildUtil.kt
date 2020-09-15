package com.agile4j.model.builder.build

import com.agile4j.model.builder.build.BuildContext.builderHolder
import com.agile4j.model.builder.build.BuildContext.indexerHolder
import com.agile4j.model.builder.build.BuildContext.isT
import com.agile4j.model.builder.build.BuildContext.tToAHolder
import com.agile4j.model.builder.build.DTO.Companion.emptyDTO
import com.agile4j.model.builder.delegate.ModelBuilderDelegate
import com.agile4j.model.builder.exception.ModelBuildException
import com.agile4j.model.builder.exception.ModelBuildException.Companion.err
import com.agile4j.model.builder.scope.Scopes.nullableModelBuilder
import com.agile4j.utils.scope.Scope
import com.agile4j.utils.util.CollectionUtil
import com.agile4j.utils.util.MapUtil
import kotlin.reflect.KClass
import kotlin.reflect.full.createType

/**
 * abbreviations:
 * T        target
 * IXA      index or accompany
 * @author liurenpeng
 * Created on 2020-07-09
 */

internal val emptyHolder = Any()

internal var Any.buildInModelBuilder : ModelBuilder by ModelBuilderDelegate()

internal fun <IXA: Any, T: Any> buildTargets(
    modelBuilder: ModelBuilder,
    tClazz: KClass<T>,
    ixas: Collection<IXA?>
): Set<T> {
    val filteredIxas = ixas.filter { it != null }.toSet()
    if (CollectionUtil.isEmpty(filteredIxas)) return emptySet()

    Scope.beginScope()
    val dto = buildDTO(tClazz, filteredIxas)
    if (dto.isEmpty) return emptySet()
    injectModelBuilder(modelBuilder, dto.targets)
    injectAccompaniesAndTargets(modelBuilder, dto)
    return dto.targets.filter { it != null }.map { it as T }.toSet()
}

private fun <IXA: Any, T: Any> buildDTO(
    tClazz: KClass<T>,
    ixas: Collection<IXA?>
): DTO<T?> {
    if (!isT(tClazz)) err("$tClazz is not target class")
    val aClazz = tToAHolder[tClazz]
    val iClazz = BuildContext.aToIHolder[aClazz]
    val ixaClazz = ixas.first()!!::class
    if (ixaClazz != iClazz && ixaClazz != aClazz)
        err("$ixaClazz is neither index class nor accompany class")

    if (CollectionUtil.isEmpty(ixas)) return emptyDTO()
    val iToA : Map<Any?, Any?> = buildIToA(aClazz!!, ixas)
    val tToA = iToA.values.map { a ->
        buildTarget(tClazz, aClazz, a) to a }.toMap()
    return DTO(iToA, tToA)
}

private data class DTO<T> (
    val iToA: Map<Any?, Any?>,
    val tToA: Map<T?, Any?>
) {
    val targets: Set<T?> = tToA.keys.toSet()
    val isEmpty: Boolean = MapUtil.isEmpty(tToA) || MapUtil.isEmpty(iToA)
            || CollectionUtil.isEmpty(tToA.keys.filter { it != null }.toList())
            || CollectionUtil.isEmpty(tToA.values.filter { it != null }.toList())
            || CollectionUtil.isEmpty(iToA.keys.filter { it != null }.toList())
            || CollectionUtil.isEmpty(iToA.values.filter { it != null }.toList())
    companion object {
        fun <T> emptyDTO() : DTO<T?> = DTO(emptyMap(), emptyMap())
    }
}

private fun <T : Any> buildTarget(
    tClazz: KClass<T>,
    aClazz: KClass<*>,
    a: Any?
): T? = if (a == null) null else tClazz.constructors.stream()
        .filter { it.parameters.size == 1 }
        .filter { it.parameters[0].type == aClazz.createType() }
        .findFirst()
        .map { it.call(a) }
        .orElseThrow { ModelBuildException("no suitable constructor found"
                + " for targetClass:$tClazz. accompanyClass:$aClazz") }

/**
 * @return index -> accompany
 */
@Suppress("UNCHECKED_CAST")
private fun <IXA> buildIToA(
    aClazz: KClass<*>,
    ioas: Collection<IXA>
): Map<Any?, Any?> {
    if (aClazz.isInstance(ioas.first())) { // buildByAccompany. IOA is A
        val accompanyIndexer = indexerHolder[aClazz] as (IXA) -> Any
        return ioas.map { accompanyIndexer.invoke(it) to it }.toMap()
    } else { // buildByAccompanyIndex. IOA is I
        val builder = builderHolder[aClazz]
                as (Collection<IXA>) -> Map<Any?, Any?>

        val modelBuilder = nullableModelBuilder()
        val iToACache = modelBuilder?.getIToACache(aClazz)
        if (modelBuilder == null || MapUtil.isEmpty(iToACache)) return builder.invoke(ioas)

        val cached = iToACache!!.filter { ioas.contains(it.key as IXA) }
        val unCachedIs = ioas.filter { !cached.keys.contains(it as Any) }
        if (CollectionUtil.isEmpty(unCachedIs)) return cached

        val buildIToA = builder.invoke(unCachedIs)
        val unFetchedIs = unCachedIs.filter { !buildIToA.keys.contains(it) }.toSet()
        val unFetchedIToA = unFetchedIs.map { it to null }.toMap()
        return cached + buildIToA + unFetchedIToA
    }
}

/**
 * 把modelBuilder注入到targets中
 */
private fun <T : Any> injectModelBuilder(
    modelBuilder: ModelBuilder,
    targets: Set<T?>
) = targets.forEach { it?.buildInModelBuilder = modelBuilder }

/**
 * 把accompanies和targets注入到modelBuilder中
 */
private fun <T : Any> injectAccompaniesAndTargets(
    modelBuilder: ModelBuilder,
    dto: DTO<T?>
) {
    modelBuilder.putIAT(dto.iToA, dto.tToA)
    /*modelBuilder.iToA.putAll(dto.iToA)
    modelBuilder.tToA.putAll(dto.tToA)*/

    modelBuilder.putAllIToACache(modelBuilder.aClazz, dto.iToA)
    modelBuilder.putAllTToACache(modelBuilder.tClazz, dto.tToA)
}

