package com.agile4j.model.builder.build

import com.agile4j.model.builder.build.BuildContext.getAClazzByT
import com.agile4j.model.builder.build.BuildContext.getBuilder
import com.agile4j.model.builder.build.BuildContext.getConstructor
import com.agile4j.model.builder.build.BuildContext.getIClazzByA
import com.agile4j.model.builder.build.BuildContext.getIndexer
import com.agile4j.model.builder.build.BuildContext.isT
import com.agile4j.model.builder.delegate.ModelBuilderDelegate
import com.agile4j.model.builder.exception.ModelBuildException.Companion.err
import com.agile4j.utils.scope.Scope
import com.agile4j.utils.util.CollectionUtil
import com.agile4j.utils.util.MapUtil
import java.util.stream.Collectors
import java.util.stream.Collectors.toSet
import kotlin.reflect.KClass

/**
 * abbreviations:
 * T        target
 * IXA      index or accompany
 * @author liurenpeng
 * Created on 2020-07-09
 */

internal var Any.buildInModelBuilder : ModelBuilder by ModelBuilderDelegate()

internal fun <T: Any> filterTargets(
    targets: Collection<T>,
    filter: (T) -> Boolean
): List<T> {
    val modelBuilders = targets.stream()
        .map { it.buildInModelBuilder }.collect(toSet())
    if (modelBuilders.size != 1) err("targets not build by modelBuilder. targets:$targets")
    val modelBuilder = modelBuilders.first()

    val partitionTargets = targets.stream()
        .collect(Collectors.partitioningBy(filter))
    modelBuilder.removeByTColl(partitionTargets[false])
    return partitionTargets[true] ?: emptyList()
}

internal fun <T: Any> filterTargetValueMap(
    targetValueMap: LinkedHashMap<Any, T>,
    filter: (T) -> Boolean
): LinkedHashMap<Any, T> {
    val targets = targetValueMap.values
    val modelBuilders = targets.stream()
        .map { it.buildInModelBuilder }.collect(toSet())
    if (modelBuilders.size != 1) err("targets not build by modelBuilder. targets:$targets")
    val modelBuilder = modelBuilders.first()

    val partitionTargets = targets.stream()
        .collect(Collectors.partitioningBy(filter))
    val disableTargets = partitionTargets[false]
    val enableTargets = partitionTargets[true]

    modelBuilder.removeByTColl(disableTargets)
    if (CollectionUtil.isEmpty(disableTargets)) return targetValueMap
    if (CollectionUtil.isEmpty(enableTargets)) return LinkedHashMap()

    val result = LinkedHashMap<Any, T>()
    targetValueMap.entries.stream()
        .filter{ enableTargets!!.contains(it.value) }
        .forEach { result[it.key] = it.value }
    return result
}

@Suppress("UNCHECKED_CAST")
internal fun <IXA: Any, T: Any> buildTargets(
    modelBuilder: ModelBuilder,
    tClazz: KClass<T>,
    ixas: Collection<IXA?>
): List<T> {
    val targets = mutableListOf<T>()

    val dto = commonBuild(modelBuilder, tClazz, ixas) ?: return targets
    val tMapper = tMapper(dto, modelBuilder)

    ixas.forEach { ixa -> tMapper.invoke(ixa)?.let{ targets.add(it as T) } }
    return targets
}

@Suppress("UNCHECKED_CAST")
internal fun <IXA: Any, T: Any> buildTargetMapOfI(
    modelBuilder: ModelBuilder,
    tClazz: KClass<T>,
    ixas: Collection<IXA?>
): LinkedHashMap<Any, T> {
    val iToTMap = LinkedHashMap<Any, T>()

    val dto = commonBuild(modelBuilder, tClazz, ixas) ?: return iToTMap
    val tMapper = tMapper(dto, modelBuilder)
    val iMapper = iMapper(dto, modelBuilder)

    ixas.forEach { ixa ->
        val t = tMapper.invoke(ixa)
        val i = iMapper.invoke(ixa)
        if (t != null && i != null) iToTMap[i] = (t as T)
    }
    return iToTMap
}

@Suppress("UNCHECKED_CAST")
internal fun <IXA: Any, T: Any> buildTargetMapOfA(
    modelBuilder: ModelBuilder,
    tClazz: KClass<T>,
    ixas: Collection<IXA?>
): LinkedHashMap<Any, T> {
    val aToTMap = LinkedHashMap<Any, T>()

    val dto = commonBuild(modelBuilder, tClazz, ixas) ?: return aToTMap
    val tMapper = tMapper(dto, modelBuilder)
    val aMapper = aMapper(dto, modelBuilder)

    ixas.forEach { ixa ->
        val t = tMapper.invoke(ixa)
        val a = aMapper.invoke(ixa)
        if (t != null && a != null) aToTMap[a] = (t as T)
    }
    return aToTMap
}

/**
 * @return (IXA) -> I
 */
private fun <T: Any> iMapper(dto: DTO<T>, modelBuilder: ModelBuilder): (Any?) -> Any? =
    if (dto.isA) modelBuilder::getCurrIByA else { it -> it }

/**
 * @return (IXA) -> A
 */
private fun <T: Any> aMapper(dto: DTO<T>, modelBuilder: ModelBuilder): (Any?) -> Any? =
    if (dto.isA) { it -> it } else modelBuilder::getCurrAByI

/**
 * @return (IXA) -> T
 */
private fun <T: Any> tMapper(dto: DTO<T>, modelBuilder: ModelBuilder): (Any?) -> Any? =
    if (dto.isA) modelBuilder::getCurrTByA else modelBuilder::getCurrTByI

private fun <IXA: Any, T: Any> commonBuild(
    modelBuilder: ModelBuilder,
    tClazz: KClass<T>,
    ixas: Collection<IXA?>
): DTO<T>? {
    if (CollectionUtil.isEmpty(ixas)) return null
    val filteredIxas = mutableListOf<IXA>()
    ixas.forEach { ixa -> ixa?.let { if (!filteredIxas.contains(it)) filteredIxas.add(it) } }
    if (CollectionUtil.isEmpty(filteredIxas)) return null

    Scope.beginScope()
    val dto = buildDTO(modelBuilder, tClazz, filteredIxas)
    if (dto.isEmpty) return null

    dto.targets.forEach { it.buildInModelBuilder = modelBuilder }
    modelBuilder.putCurrIAT(dto.iToA, dto.tToA)
    return dto
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

    val iToA: Map<Any, Any> = cacheAndGetUnNullIToA(buildIToA(
        aClazz, ixas, isA, modelBuilder), aClazz, modelBuilder)
    val tToA: Map<T, Any> = cacheAndGetTToA(buildTToA(
        iToA, tClazz, aClazz), tClazz, modelBuilder)
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
internal fun cacheAndGetUnNullIToA(
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
private fun <IXA: Any> buildIToA(
    aClazz: KClass<*>,
    ixas: Collection<IXA>,
    isA: Boolean, // ixa是i还是a true:isA false:isI
    modelBuilder: ModelBuilder
): Map<Any?, Any?> = if (isA) { // buildByAccompany. IXA is A
    val accompanyIndexer = getIndexer<IXA, Any>(aClazz)
    ixas.associateBy({accompanyIndexer.invoke(it)}, {it})
} else { // buildByAccompanyIndex. IXA is I
    //buildIToAByIs(aClazz, ixas, nullableModelBuilder())
    buildIToAByIs(aClazz, ixas, modelBuilder)
}

@Suppress("UNCHECKED_CAST")
internal fun <IXA : Any> buildIToAByIs(
    aClazz: KClass<*>,
    ixas: Collection<IXA>,
    modelBuilder: ModelBuilder
): Map<Any?, Any?> {
    val builder = getBuilder<Any?, Any?>(aClazz)
    //if (modelBuilder == null) return realBuildIToA(builder, ixas)

    val cacheResp = modelBuilder.getGlobalIToACache(aClazz, ixas)
    val cachedIToA = cacheResp.cached
    if (MapUtil.isEmpty(cachedIToA)) return realBuildIToA(builder, ixas)

    val unCachedIs = cacheResp.unCached as Collection<IXA>
    return if (CollectionUtil.isEmpty(unCachedIs)) cachedIToA else
        cachedIToA + realBuildIToA(builder, unCachedIs)
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
