package com.agile4j.model.builder.build

import com.agile4j.model.builder.build.BuildContext.builderHolder
import com.agile4j.model.builder.build.BuildContext.getAClazzByT
import com.agile4j.model.builder.build.BuildContext.getIClazzByA
import com.agile4j.model.builder.build.BuildContext.indexerHolder
import com.agile4j.model.builder.build.BuildContext.isT
import com.agile4j.model.builder.delegate.ModelBuilderDelegate
import com.agile4j.model.builder.exception.ModelBuildException
import com.agile4j.model.builder.exception.ModelBuildException.Companion.err
import com.agile4j.model.builder.scope.Scopes.nullableModelBuilder
import com.agile4j.utils.scope.Scope
import com.agile4j.utils.util.CollectionUtil
import com.agile4j.utils.util.MapUtil
import java.util.stream.Collectors
import kotlin.reflect.KClass
import kotlin.reflect.full.createType

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
    ixas: Collection<IXA?>
): Collection<T> {
    val filteredIxas = ixas.stream()
        .filter { it != null }.distinct().collect(Collectors.toList())
    if (CollectionUtil.isEmpty(filteredIxas)) return emptyList()

    Scope.beginScope()
    val dto = buildDTO(tClazz, filteredIxas)
    if (dto.isEmpty) return emptyList()
    injectModelBuilder(modelBuilder, dto.targets)
    injectAccompaniesAndTargets(modelBuilder, dto)

    return dto.targets.filter { it != null }.map { it as T }.toSet()
}

private fun <IXA: Any, T: Any> buildDTO(
    tClazz: KClass<T>,
    ixas: Collection<IXA?>
): DTO<T?> {
    if (!isT(tClazz)) err("$tClazz is not target class")
    val aClazz = getAClazzByT(tClazz) ?: err("$tClazz not fount it's aClazz")
    val iClazz = getIClazzByA(aClazz) ?: err("$aClazz not fount it's iClazz")

    val isA = when (val ixaClazz = ixas.first()!!::class) {
        aClazz -> true
        iClazz -> false
        else -> err("$ixaClazz is neither index class nor accompany class")
    }

    val iToA : Map<Any?, Any?> = buildIToA(aClazz, ixas, isA)
    val tToA = iToA.values.map { a ->
        buildTarget(tClazz, aClazz, a) to a }.toMap()
    return DTO(isA, iToA, tToA)
}

private data class DTO<T> (
    val isA: Boolean, // ixa是i还是a true:isA false:isI
    val iToA: Map<Any?, Any?>,
    val tToA: Map<T?, Any?>
) {
    val targets: Set<T?> = tToA.keys.toSet()
    val isEmpty: Boolean = MapUtil.isEmpty(iToA)
            || CollectionUtil.isEmpty(iToA.keys.filter { it != null }.toList())
            || CollectionUtil.isEmpty(iToA.values.filter { it != null }.toList())
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
    ixas: Collection<IXA>,
    isA: Boolean // ixa是i还是a true:isA false:isI
): Map<Any?, Any?> {
    if (isA) { // buildByAccompany. IOA is A
        val accompanyIndexer = indexerHolder[aClazz] as (IXA) -> Any
        return ixas.associateBy({accompanyIndexer.invoke(it)}, {it})
    } else { // buildByAccompanyIndex. IOA is I
        val builder = builderHolder[aClazz] as (Collection<IXA>) -> Map<Any?, Any?>

        val modelBuilder = nullableModelBuilder() ?: return builder.invoke(ixas)
        val cachedIToA = modelBuilder.getGlobalIToACache(aClazz, ixas)
        if (MapUtil.isEmpty(cachedIToA)) return builder.invoke(ixas)

        val unCachedIs = ixas.filter { !cachedIToA.keys.contains(it) }
        if (CollectionUtil.isEmpty(unCachedIs)) return cachedIToA

        val buildIToA = builder.invoke(unCachedIs)
        val unFetchedIs = unCachedIs.filter { !buildIToA.keys.contains(it) }
        val unFetchedIToA = unFetchedIs.associateWith { null }
        return cachedIToA + buildIToA + unFetchedIToA
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
    modelBuilder.putCurrIAT(dto.iToA, dto.tToA)

    modelBuilder.putGlobalIToACache(modelBuilder.currAClazz, dto.iToA)
    modelBuilder.putGlobalTToACache(modelBuilder.currTClazz, dto.tToA)
}

