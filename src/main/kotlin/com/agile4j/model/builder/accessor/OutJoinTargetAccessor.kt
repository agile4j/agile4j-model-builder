package com.agile4j.model.builder.accessor

import com.agile4j.model.builder.build.BuildContext
import com.agile4j.model.builder.buildMulti
import com.agile4j.model.builder.by
import com.agile4j.model.builder.delegate.ITargetDelegate.ScopeKeys.modelBuilderScopeKey
import com.agile4j.model.builder.delegate.map.WeakIdentityHashMap
import com.agile4j.utils.access.IAccessor
import com.agile4j.utils.util.CollectionUtil
import com.agile4j.utils.util.MapUtil
import java.util.stream.Collectors
import kotlin.reflect.KClass

/**
 * @author liurenpeng
 * Created on 2020-06-18
 */
class OutJoinTargetAccessor<A : Any, AI, OJT>(private val outJoinTargetPoint: String) : IAccessor<A, OJT> {
    @Suppress("UNCHECKED_CAST")
    override fun get(sources: Collection<A>): Map<A, OJT> {
        val modelBuilder = modelBuilderScopeKey.get()!!


        val accompanies = sources.toSet()
        if (CollectionUtil.isEmpty(accompanies)) return emptyMap()
        val outJoinAccompanyPointToMapperMap = BuildContext.outJoinHolder[accompanies.elementAt(0)::class]
        if (MapUtil.isEmpty(outJoinAccompanyPointToMapperMap)) return emptyMap()
        val mapper = outJoinAccompanyPointToMapperMap!![outJoinTargetPoint] as (Collection<AI>) -> Map<AI, Any>

        val indexer = BuildContext.indexerHolder[accompanies.elementAt(0)::class] as (A) -> AI
        val accompanyToAccompanyIndexMap: Map<A, AI> = accompanies.map { it to indexer.invoke(it) }.toMap()
        val accompanyIndexToAccompanyMap = accompanyToAccompanyIndexMap.map { (k, v) -> v to k }.toMap()

        //val accompanyIndices = accompanyToAccompanyIndexMap.values
        val reverseCacheMap = modelBuilder.outJoinTargetCacheMap
            .computeIfAbsent(outJoinTargetPoint) { WeakIdentityHashMap() } as MutableMap<Any, A>
        val cacheMap = reverseCacheMap.map { (k, v) -> v to k }.toMap()
        val filteredCached = cacheMap.filterKeys { accompanies.contains(it) }
        //val cachedAccompanyIndexToOutJoinTargetMap = cached.mapKeys { accompanyToAccompanyIndexMap[it.key] ?: error("43423") }
        val unCachedAccompanies = accompanies.filter { !filteredCached.keys.contains(it) }
        val unCachedKeys = unCachedAccompanies.map { accompanyToAccompanyIndexMap[it] ?: error("3423") }


        val allAccompanyIndexToOutJoinTargetMap = mutableMapOf<AI, Any>()
        allAccompanyIndexToOutJoinTargetMap.putAll(cacheMap.mapKeys { accompanyToAccompanyIndexMap[it.key] ?: error("67455")})


        var isCollection = false
        lateinit var outJoinTargets: Collection<Any>
        if (CollectionUtil.isNotEmpty(unCachedKeys)) {
            var buildAccompanyIndexToOutJoinAccompanyMap = mapper.invoke(unCachedKeys)
            val buildAccompanyToOutJoinAccompanyMap = buildAccompanyIndexToOutJoinAccompanyMap
                .mapKeys { accompanyIndexToAccompanyMap[it.key] ?: error("67455") }

            val accompanyClazzToTargetClazzMap = BuildContext.accompanyHolder.map { (k, v) -> v to k }.toMap()

            lateinit var outJoinTargetClazz: KClass<Any>
            if (MapUtil.isNotEmpty(buildAccompanyToOutJoinAccompanyMap)) {
                isCollection = Collection::class.java.isAssignableFrom(
                    buildAccompanyToOutJoinAccompanyMap.values.elementAt(0)::class.java
                )

                val outJoinAccompanyClazz = if (!isCollection) {
                    buildAccompanyToOutJoinAccompanyMap.values.elementAt(0)::class
                } else {
                    val coll = buildAccompanyToOutJoinAccompanyMap.values.elementAt(0) as Collection<Any>
                    coll.elementAt(0)::class
                }

                outJoinTargetClazz = accompanyClazzToTargetClazzMap[outJoinAccompanyClazz] as KClass<Any>
            } else if (MapUtil.isNotEmpty(filteredCached)) {
                isCollection = Collection::class.java.isAssignableFrom(
                    filteredCached.values.elementAt(0)::class.java
                )

                outJoinTargetClazz = if (!isCollection) {
                    filteredCached.values.elementAt(0)::class as KClass<Any>
                } else {
                    val coll = filteredCached.values.elementAt(0) as Collection<Any>
                    coll.elementAt(0)::class as KClass<Any>
                }

            } else {
                return emptyMap()
            }

            /*val allAccompanyIndexToOutJoinAccompanyMap = mutableMapOf<AI, Any>()
            allAccompanyIndexToOutJoinAccompanyMap.putAll(buildAccompanyIndexToOutJoinAccompanyMap)
            allAccompanyIndexToOutJoinAccompanyMap.putAll(accompanyIndexToOutJoinCached.mapValues { modelBuilder.targetToAccompanyMap[it]!! })


            val isCollection = Collection::class.java.isAssignableFrom(
                allAccompanyIndexToOutJoinAccompanyMap.values.elementAt(0)::class.java
            )
            val outJoinAccompanyClazz = if (!isCollection) {
                allAccompanyIndexToOutJoinAccompanyMap.values.elementAt(0)::class
            } else {
                val coll = allAccompanyIndexToOutJoinAccompanyMap.values.elementAt(0) as Collection<Any>
                coll.elementAt(0)::class
            }

            val accompanyClazzToTargetClazzMap = BuildContext.accompanyHolder.map { (k, v) -> v to k }.toMap()
            val outJoinTargetClazz = accompanyClazzToTargetClazzMap[outJoinAccompanyClazz] as KClass<Any>*/




            val outJoinAccompanies = if (!isCollection) {
                buildAccompanyIndexToOutJoinAccompanyMap.values
            } else {
                buildAccompanyIndexToOutJoinAccompanyMap.values.stream().flatMap {
                    it as Collection<Any>
                    it.stream()
                }.collect(Collectors.toList())
            }
             outJoinTargets = modelBuilder buildMulti outJoinTargetClazz by outJoinAccompanies
            //val outJoinTargets = ModelBuilder() buildMulti outJoinTargetClazz by outJoinAccompanies
            // TODO  加到modelBuilder里？
            val outJoinTargetToOutJoinAccompanyMap = modelBuilder.targetToAccompanyMap.map { (k, v) -> v to k }.toMap()

            /*cacheMap.putAll(outJoinTargets.map { outJoinTarget ->  (
                    if (!isCollection) {
                        buildAccompanyToOutJoinAccompanyMap.filter { e ->  e.value == modelBuilder.targetToAccompanyMap[it]}.entries
                            .stream().findFirst().map { e -> e.key }.orElseThrow { ModelBuildException("") }
                    } else {
                        buildAccompanyToOutJoinAccompanyMap.mapValues { e ->
                            val collValue = e.value as Collection<Any>
                            collValue.map { v -> outJoinTargetToOutJoinAccompanyMap[v] ?: error("423") }
                        }
                    }
                    ) to outJoinTarget as OJT}.toMap()) // 入缓存*/
            val toCache = if (!isCollection) {
                buildAccompanyToOutJoinAccompanyMap.mapValues { v -> outJoinTargetToOutJoinAccompanyMap[v] ?: error("423") }
            } else {
                buildAccompanyToOutJoinAccompanyMap.mapValues { e ->
                    val collValue = e.value as Collection<Any>
                    collValue.map { v -> outJoinTargetToOutJoinAccompanyMap[v] ?: error("423") }
                }
            }
            reverseCacheMap.putAll(toCache.map { (k, v) -> v to k }.toMap()) // 入缓存


            allAccompanyIndexToOutJoinTargetMap.putAll(buildAccompanyIndexToOutJoinAccompanyMap)
        } else if (MapUtil.isNotEmpty(filteredCached))  { // TODO 这块代码重复了，后期优化
            isCollection = Collection::class.java.isAssignableFrom(
                filteredCached.values.elementAt(0)::class.java
            )
            outJoinTargets = if (isCollection) {
                filteredCached.values.stream().flatMap { v ->
                    v as Collection<Any>
                    v.stream()
                }.collect(Collectors.toSet())
            } else {
                filteredCached.values
            }
        } else {
            return emptyMap()
        }



        val result =  accompanyToAccompanyIndexMap.mapValues { (_, accompanyIndex) ->
            /*val outJoinAccompany = allAccompanyIndexToOutJoinTargetMap[accompanyIndex]
            val target = if (!isCollection) {
                outJoinTargets.first { outJoinTarget ->
                    outJoinTarget.buildInModelBuilder
                        .targetToAccompanyMap[outJoinTarget] == outJoinAccompany
                }
            } else {
                outJoinTargets.filter { outJoinTarget ->
                    outJoinAccompany as Collection<Any>
                    outJoinAccompany.contains(outJoinTarget.buildInModelBuilder
                        .targetToAccompanyMap[outJoinTarget])
                }
            }
            target*/
            allAccompanyIndexToOutJoinTargetMap[accompanyIndex]
        } as Map<A, OJT>
        print("")
        return result
    }
}