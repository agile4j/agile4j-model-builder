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

@Suppress("UNCHECKED_CAST")
internal fun <IXA: Any, T: Any> buildTargets(
    modelBuilder: ModelBuilder,
    tClazz: KClass<T>,
    ixas: Collection<IXA?>
): Collection<T> {
    val filteredIxas = ixas.stream()
        .filter { it != null }.distinct().collect(Collectors.toList())
    if (CollectionUtil.isEmpty(filteredIxas)) return emptyList()

    Scope.beginScope()
    val dto = buildDTO(modelBuilder, tClazz, filteredIxas)
    if (dto.isEmpty) return emptyList()

    dto.targets.forEach { it.buildInModelBuilder = modelBuilder }
    modelBuilder.putCurrIAT(dto.iToA, dto.tToA)

    val mapper = if (dto.isA) modelBuilder::getCurrTByA else modelBuilder::getCurrTByI
    return ixas.stream().map { mapper.call(it) as T }.filter{ it != null }.collect(Collectors.toList())
}


private fun <IXA: Any, T: Any> buildDTO(
    modelBuilder: ModelBuilder,
    tClazz: KClass<T>,
    ixas: Collection<IXA?>
): DTO<T> {
    if (!isT(tClazz)) err("$tClazz is not target class")
    val aClazz = getAClazzByT(tClazz) ?: err("$tClazz not fount it's aClazz")
    val iClazz = getIClazzByA(aClazz) ?: err("$aClazz not fount it's iClazz")

    val isA = when (val ixaClazz = ixas.first()!!::class) {
        aClazz -> true
        iClazz -> false
        else -> err("$ixaClazz is neither index class nor accompany class")
    }

    val iToA: Map<Any, Any> = cacheAndGetUnNullIToA(buildIToA(aClazz, ixas, isA), aClazz, modelBuilder)
    val tToA: Map<T, Any> = cacheAndGetTToA(buildTToA(iToA, tClazz, aClazz), tClazz, modelBuilder)
    return DTO(isA, iToA, tToA)
}

private data class DTO<T> (
    val isA: Boolean, // ixa是i还是a true:isA false:isI
    val iToA: Map<Any, Any>,
    val tToA: Map<T, Any>
) {
    val targets: Set<T> = tToA.keys
    val isEmpty: Boolean = MapUtil.isEmpty(iToA)
}

@Suppress("UNCHECKED_CAST")
private fun cacheAndGetUnNullIToA(
    iToA: Map<Any?, Any?>,
    aClazz: KClass<*>,
    modelBuilder: ModelBuilder): Map<Any, Any> {
    return modelBuilder.putGlobalIToACacheAndReturnUnNull(aClazz, iToA)
}

private fun <T: Any> cacheAndGetTToA(
    tToA: Map<T, Any>,
    tClazz: KClass<*>,
    modelBuilder: ModelBuilder): Map<T, Any> {
    modelBuilder.putGlobalTToACache(tClazz, tToA)
    return tToA
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
        .orElseThrow { ModelBuildException("no suitable constructor found"
                + " for targetClass:$tClazz. accompanyClass:$aClazz") }

private fun <T : Any> buildTToA(
    iToA: Map<Any, Any>,
    tClazz: KClass<T>,
    aClazz: KClass<*>
) = iToA.values.associateBy({ buildTarget(tClazz, aClazz, it) }, { it })

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
        val builder = builderHolder[aClazz]
                as (Collection<IXA>) -> Map<Any?, Any?>

        val modelBuilder = nullableModelBuilder() ?: return realBuildIToA(builder, ixas)

        val cachedIToA = modelBuilder.getGlobalIToACache(aClazz, ixas)
        if (MapUtil.isEmpty(cachedIToA)) return realBuildIToA(builder, ixas)

        val unCachedIs = ixas.filter { !cachedIToA.keys.contains(it) }
        return if (CollectionUtil.isEmpty(unCachedIs)) cachedIToA else
            cachedIToA + realBuildIToA(builder, unCachedIs)
    }
}

private fun <IXA> realBuildIToA(
    builder: (Collection<IXA>) -> Map<Any?, Any?>,
    ixas: Collection<IXA>
): Map<Any?, Any?> {
    val buildIToA = builder.invoke(ixas)
    if (buildIToA.size == ixas.size) {
        return buildIToA
    }
    val unFetchedIs = ixas.filter { !buildIToA.keys.contains(it) }
    val unFetchedIToA = unFetchedIs.associateWith { null }
    return buildIToA + unFetchedIToA
}
