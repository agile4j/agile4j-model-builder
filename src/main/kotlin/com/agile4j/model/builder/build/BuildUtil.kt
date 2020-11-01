package com.agile4j.model.builder.build

import com.agile4j.model.builder.build.BuildContext.getAClazzByT
import com.agile4j.model.builder.build.BuildContext.getBuilder
import com.agile4j.model.builder.build.BuildContext.getConstructor
import com.agile4j.model.builder.build.BuildContext.getIClazzByA
import com.agile4j.model.builder.build.BuildContext.getIndexer
import com.agile4j.model.builder.build.BuildContext.isT
import com.agile4j.model.builder.delegate.ModelBuilderDelegate
import com.agile4j.model.builder.exception.ModelBuildException.Companion.err
import com.agile4j.model.builder.scope.Scopes.nullableModelBuilder
import com.agile4j.utils.scope.Scope
import com.agile4j.utils.util.CollectionUtil
import com.agile4j.utils.util.MapUtil
import kotlin.reflect.KClass

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
    val filteredIxas = mutableListOf<IXA>()
    ixas.forEach { ixa -> ixa?.let { if (!filteredIxas.contains(it)) filteredIxas.add(it) } }
    if (CollectionUtil.isEmpty(filteredIxas)) return emptyList()

    Scope.beginScope()
    val dto = buildDTO(modelBuilder, tClazz, filteredIxas)
    if (dto.isEmpty) return emptyList()

    dto.targets.forEach { it.buildInModelBuilder = modelBuilder }
    modelBuilder.putCurrIAT(dto.iToA, dto.tToA)

    val mapper = if (dto.isA) modelBuilder::getCurrTByA else modelBuilder::getCurrTByI
    val result = mutableListOf<T>()
    ixas.forEach { ixa -> mapper.call(ixa)?.let{ result.add(it as T) } }
    return result
}


private fun <IXA: Any, T: Any> buildDTO(
    modelBuilder: ModelBuilder,
    tClazz: KClass<T>,
    ixas: Collection<IXA>
): DTO<T> {
    if (!isT(tClazz)) err("$tClazz is not target class")
    val aClazz = getAClazzByT(tClazz) ?: err("$tClazz not found it's accompany clazz")
    val iClazz = getIClazzByA(aClazz) ?: err("$aClazz not found it's index clazz")

    val isA = when (val ixaClazz = ixas.first()::class) {
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

@Suppress("UNCHECKED_CAST")
private fun <T : Any> buildTToA(
    iToA: Map<Any, Any>,
    tClazz: KClass<T>,
    aClazz: KClass<*>): Map<T, Any> {
    val constructor = getConstructor(tClazz)?: err(
        "no suitable constructor found for targetClass:$tClazz. accompanyClass:$aClazz")
    return iToA.values.associateBy({ constructor.call(it) }, { it })
}

/**
 * @return index -> accompany
 */
@Suppress("UNCHECKED_CAST")
private fun <IXA: Any> buildIToA(
    aClazz: KClass<*>,
    ixas: Collection<IXA>,
    isA: Boolean // ixa是i还是a true:isA false:isI
): Map<Any?, Any?> {
    if (isA) { // buildByAccompany. IXA is A
        val accompanyIndexer = getIndexer<IXA, Any>(aClazz)
        return ixas.associateBy({accompanyIndexer.invoke(it)}, {it})
    } else { // buildByAccompanyIndex. IXA is I
        val builder = getBuilder<Any?, Any?>(aClazz)

        val modelBuilder = nullableModelBuilder() ?: return realBuildIToA(builder, ixas)

        val cacheResp = modelBuilder.getGlobalIToACache(aClazz, ixas)
        val cachedIToA = cacheResp.cached
        if (MapUtil.isEmpty(cachedIToA)) return realBuildIToA(builder, ixas)

        val unCachedIs = cacheResp.unCached as Collection<IXA>
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
