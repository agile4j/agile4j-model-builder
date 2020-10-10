package com.agile4j.model.builder.build

import com.agile4j.model.builder.build.BuildContext.builderHolder
import com.agile4j.model.builder.build.BuildContext.constructorHolder
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
import kotlin.reflect.KFunction
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
    val t1= System.nanoTime()
    val filteredIxas = ixas.stream()
        .filter { it != null }.distinct().collect(Collectors.toList())
    val t2 = System.nanoTime()
    if (CollectionUtil.isEmpty(filteredIxas)) return emptyList()

    val t3 = System.nanoTime()
    Scope.beginScope()
    val t4 = System.nanoTime()
    val dto = buildDTO(modelBuilder, tClazz, filteredIxas)
    val t5 = System.nanoTime()
    if (dto.isEmpty) return emptyList()

    val t6 = System.nanoTime()
    dto.targets.forEach { it.buildInModelBuilder = modelBuilder }
    val t7 = System.nanoTime()
    modelBuilder.putCurrIAT(dto.iToA, dto.tToA)

    val t8 = System.nanoTime()
    val mapper = if (dto.isA) modelBuilder::getCurrTByA else modelBuilder::getCurrTByI
    val t9 = System.nanoTime()
    //val result = ixas.stream().map { mapper.call(it) as T }.filter{ it != null }.collect(Collectors.toList())
    val result = mutableListOf<T>()
    ixas.forEach { ixa -> mapper.call(ixa)?.let{ result.add(it as T) } }
    val t10 = System.nanoTime()
    println("==========t1:$t1")
    println("==========t2:$t2  di:\t${t2 - t1}")
    println("==========t3:$t3  di:\t${t3 - t2}")
    println("==========t4:$t4  di:\t${t4 - t3}")
    println("==========t5:$t5  di:\t${t5 - t4}")
    println("==========t6:$t6  di:\t${t6 - t5}")
    println("==========t7:$t7  di:\t${t7 - t6}")
    println("==========t8:$t8  di:\t${t8 - t7}")
    println("==========t9:$t9  di:\t${t9 - t8}")
    println("=========t10:$t10  di:\t${t10 - t9}")
    println("===========t:$t10  di:\t${t10 - t1}")
    return result
}


private fun <IXA: Any, T: Any> buildDTO(
    modelBuilder: ModelBuilder,
    tClazz: KClass<T>,
    ixas: Collection<IXA?>
): DTO<T> {
    val t1= System.nanoTime()
    if (!isT(tClazz)) err("$tClazz is not target class")
    val t2 = System.nanoTime()
    val aClazz = getAClazzByT(tClazz) ?: err("$tClazz not fount it's aClazz")
    val t3 = System.nanoTime()
    val iClazz = getIClazzByA(aClazz) ?: err("$aClazz not fount it's iClazz")

    val t4 = System.nanoTime()
    val isA = when (val ixaClazz = ixas.first()!!::class) {
        aClazz -> true
        iClazz -> false
        else -> err("$ixaClazz is neither index class nor accompany class")
    }

    val t5 = System.nanoTime()
    val iToA: Map<Any, Any> = cacheAndGetUnNullIToA(buildIToA(aClazz, ixas, isA), aClazz, modelBuilder)
    val t6 = System.nanoTime()
    val tToA: Map<T, Any> = cacheAndGetTToA(buildTToA(iToA, tClazz, aClazz), tClazz, modelBuilder)
    val t7 = System.nanoTime()
    println("~~~~~~~~~~t1:$t1")
    println("~~~~~~~~~~t2:$t2  di:\t${t2 - t1}")
    println("~~~~~~~~~~t3:$t3  di:\t${t3 - t2}")
    println("~~~~~~~~~~t4:$t4  di:\t${t4 - t3}")
    println("~~~~~~~~~~t5:$t5  di:\t${t5 - t4}")
    println("~~~~~~~~~~t6:$t6  di:\t${t6 - t5}")
    println("~~~~~~~~~~t7:$t7  di:\t${t7 - t6}")
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
    val t1= System.nanoTime()
    modelBuilder.putGlobalTToACache(tClazz, tToA)
    val t2= System.nanoTime()
    println("!!!!!!!!!!t1:$t1")
    println("!!!!!!!!!!t2:$t2  di:\t${t2 - t1}")
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

@Suppress("UNCHECKED_CAST")
private fun <T : Any> buildTToA(
    iToA: Map<Any, Any>,
    tClazz: KClass<T>,
    aClazz: KClass<*>): Map<T, Any> {
    val t1= System.nanoTime()
    /*val constructor = tClazz.constructors.stream()
        .filter { it.parameters.size == 1 }
        .filter { it.parameters[0].type == aClazz.createType() }
        .findFirst()
        .orElse(null) ?: err(
        "no suitable constructor found for targetClass:$tClazz. accompanyClass:$aClazz")*/
    val constructor = constructorHolder.get(tClazz) as KFunction<T>? ?: err(
        "no suitable constructor found for targetClass:$tClazz. accompanyClass:$aClazz")
    val t2 = System.nanoTime()
    val result = iToA.values.associateBy({ constructor.call(it) }, { it })
    /*val result = mutableMapOf<T, Any>()
    iToA.values.forEach { a -> result[constructor.call(a)] = a }*/
    val t3 = System.nanoTime()
    println("##########t1:$t1")
    println("##########t2:$t2  di:\t${t2 - t1}")
    println("##########t3:$t3  di:\t${t3 - t2}")
    return result
}
        //= iToA.values.associateBy({ buildTarget(tClazz, aClazz, it) }, { it })

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
