package com.agile4j.model.builder.accessor

import com.agile4j.model.builder.ModelBuildException
import com.agile4j.model.builder.build.BuildContext
import com.agile4j.model.builder.buildMulti
import com.agile4j.model.builder.by
import com.agile4j.model.builder.delegate.ITargetDelegate.ScopeKeys.modelBuilderScopeKey
import com.agile4j.model.builder.utils.reverseKV
import com.agile4j.utils.access.IAccessor
import com.agile4j.utils.util.CollectionUtil
import com.agile4j.utils.util.MapUtil
import java.util.stream.Collectors
import kotlin.reflect.KClass

/**
 * @param A accompany
 * @param AI accompanyIndex
 * @param OJT outJoinTarget
 * @author liurenpeng
 * Created on 2020-06-18
 */
@Suppress("UNCHECKED_CAST")
class OutJoinTargetAccessor<A : Any, AI, OJT: Any>(private val outJoinTargetPoint: String) : IAccessor<A, OJT> {

    override fun get(sources: Collection<A>): Map<A, OJT> {
        val modelBuilder = modelBuilderScopeKey.get()
            ?: throw ModelBuildException("modelBuilderScopeKey not init")
        val accompanies = sources.toSet()
        val mapper = getMapper(accompanies)

        val accompanyClazz = accompanies.elementAt(0)::class
        val indexer = BuildContext.indexerHolder[accompanyClazz] as (A) -> AI
        val accompanyToAccompanyIndexMap: Map<A, AI> = accompanies.map { it to indexer.invoke(it) }.toMap()
        val accompanyIndexToAccompanyMap = accompanyToAccompanyIndexMap.reverseKV()

        val allCacheMap = modelBuilder.getOutJoinTargetCacheMap(outJoinTargetPoint) as Map<A, Any>
        val cached = allCacheMap.filterKeys { accompanies.contains(it) }
        val unCachedAccompanies = accompanies.filter { !cached.keys.contains(it) }
        val unCachedKeys = unCachedAccompanies.map { accompanyToAccompanyIndexMap[it] ?: error("3423") }


        val allAccompanyIndexToOutJoinTargetMap = mutableMapOf<AI, Any>()
        allAccompanyIndexToOutJoinTargetMap.putAll(allCacheMap.mapKeys { accompanyToAccompanyIndexMap[it.key] ?: error("67455")})


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
            } else if (MapUtil.isNotEmpty(cached)) {
                isCollection = Collection::class.java.isAssignableFrom(
                    cached.values.elementAt(0)::class.java
                )

                outJoinTargetClazz = if (!isCollection) {
                    cached.values.elementAt(0)::class as KClass<Any>
                } else {
                    val coll = cached.values.elementAt(0) as Collection<Any>
                    coll.elementAt(0)::class as KClass<Any>
                }

            } else {
                return emptyMap()
            }

            val outJoinAccompanies = if (!isCollection) {
                buildAccompanyIndexToOutJoinAccompanyMap.values
            } else {
                buildAccompanyIndexToOutJoinAccompanyMap.values.stream().flatMap {
                    it as Collection<Any>
                    it.stream()
                }.collect(Collectors.toList())
            }
            modelBuilder buildMulti outJoinTargetClazz by outJoinAccompanies
            // TODO  加到modelBuilder里？
            val outJoinAccompanyToOutJoinTargetMap = modelBuilder
                .accompanyToTargetMap as Map<Any, OJT>

            /*val buildAccompanyIndexToOutJoinTargetMap = buildAccompanyIndexToOutJoinAccompanyMap
                .mapValues { outJoinAccompanyToOutJoinTargetMap[it.value] ?: error("23") }*/

            val buildAccompanyToOutJoinTargetMap = if (!isCollection) {
                buildAccompanyToOutJoinAccompanyMap.mapValues { v -> outJoinAccompanyToOutJoinTargetMap[v] ?: error("423") }
            } else {
                buildAccompanyToOutJoinAccompanyMap.mapValues { e ->
                    val collValue = e.value as Collection<Any>
                    collValue.map { v -> outJoinAccompanyToOutJoinTargetMap[v] ?: error("423") }
                }
            }
            modelBuilder.putAllOutJoinTargetCacheMap(outJoinTargetPoint, buildAccompanyToOutJoinTargetMap) // 入缓存


            val buildAccompanyIndexToOutJoinTargetMap = buildAccompanyToOutJoinTargetMap
                .mapKeys { accompanyToAccompanyIndexMap[it.key] ?: error("") }

            allAccompanyIndexToOutJoinTargetMap.putAll(buildAccompanyIndexToOutJoinTargetMap)
        } else if (MapUtil.isNotEmpty(cached))  { // TODO 这块代码重复了，后期优化
            isCollection = Collection::class.java.isAssignableFrom(
                cached.values.elementAt(0)::class.java
            )
            outJoinTargets = if (isCollection) {
                cached.values.stream().flatMap { v ->
                    v as Collection<Any>
                    v.stream()
                }.collect(Collectors.toSet())
            } else {
                cached.values
            }
        } else {
            return emptyMap()
        }



        val result =  accompanyToAccompanyIndexMap.mapValues { (_, accompanyIndex) ->
            allAccompanyIndexToOutJoinTargetMap[accompanyIndex]
        } as Map<A, OJT>
        return result
    }

    private fun getMapper(accompanies: Set<A>): (Collection<AI>) -> Map<AI, Any> {
        if (CollectionUtil.isEmpty(accompanies)) throw ModelBuildException("accompanies is empty")
        val accompanyClazz = accompanies.elementAt(0)::class
        val outJoinPointToMapperMap = BuildContext
            .outJoinHolder[accompanyClazz] as Map<String, (Collection<AI>) -> Map<AI, Any>>
        if (MapUtil.isEmpty(outJoinPointToMapperMap)) throw ModelBuildException("outJoinPointToMapperMap is empty")
        return outJoinPointToMapperMap[outJoinTargetPoint]
            ?: throw ModelBuildException("not found matched mapper. outJoinTargetPoint:$outJoinTargetPoint")
    }
}