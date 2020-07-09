package com.agile4j.model.builder.accessor

import com.agile4j.model.builder.ModelBuilder
import com.agile4j.model.builder.build.BuildContext
import com.agile4j.model.builder.build.buildAndInjectTargetsByAccompanies
import com.agile4j.model.builder.build.buildInModelBuilder
import com.agile4j.model.builder.buildMulti
import com.agile4j.utils.access.CacheAccessor
import com.agile4j.utils.util.CollectionUtil
import com.agile4j.utils.util.MapUtil
import java.util.stream.Collectors
import kotlin.reflect.KClass

/**
 * @author liurenpeng
 * Created on 2020-06-18
 */
class OutJoinTargetAccessor<A : Any, AI, OJT>(private val outJoinTargetPoint: String) : CacheAccessor<A, OJT>() {
    @Suppress("UNCHECKED_CAST")
    override fun realGet(sources: Collection<A>): Map<A, OJT> {
        val accompanies = sources.toSet()
        if (CollectionUtil.isEmpty(accompanies)) return emptyMap()
        val outJoinAccompanyPointToMapperMap = BuildContext.outJoinHolder[accompanies.elementAt(0)::class]
        if (MapUtil.isEmpty(outJoinAccompanyPointToMapperMap)) return emptyMap()
        val mapper = outJoinAccompanyPointToMapperMap!![outJoinTargetPoint] as (Collection<AI>) -> Map<AI, Any>

        val indexer = BuildContext.indexerHolder[accompanies.elementAt(0)::class] as (A) -> AI
        val accompanyToAccompanyIndexMap: Map<A, AI> = accompanies.map { it to indexer.invoke(it) }.toMap()
        val accompanyIndexToOutJoinAccompanyMap = mapper.invoke(accompanyToAccompanyIndexMap.values)

        val isCollection = Collection::class.java.isAssignableFrom(
            accompanyIndexToOutJoinAccompanyMap.values.elementAt(0)::class.java
        )
        val outJoinAccompanyClazz = if (!isCollection) {
            accompanyIndexToOutJoinAccompanyMap.values.elementAt(0)::class
        } else {
            val coll = accompanyIndexToOutJoinAccompanyMap.values.elementAt(0) as Collection<Any>
            coll.elementAt(0)::class
        }

        val accompanyToTargetMap = BuildContext.accompanyHolder.map { (k, v) -> v to k }.toMap()
        val outJoinTargetClazz = accompanyToTargetMap[outJoinAccompanyClazz] as KClass<Any>

        val outJoinAccompanies = if (!isCollection) {
            accompanyIndexToOutJoinAccompanyMap.values
        } else {
            accompanyIndexToOutJoinAccompanyMap.values.stream().flatMap {
                it as Collection<Any>
                it.stream()
            }.collect(Collectors.toList())
        }
        val outJoinTargets = buildAndInjectTargetsByAccompanies(
            ModelBuilder() buildMulti outJoinTargetClazz, outJoinAccompanies
        )

        return accompanyToAccompanyIndexMap.mapValues { (_, accompanyIndex) ->
            val outJoinAccompany = accompanyIndexToOutJoinAccompanyMap[accompanyIndex]
            val target = if (!isCollection) {
                outJoinTargets.first { outJoinTarget ->
                    outJoinTarget.buildInModelBuilder!!
                        .targetToAccompanyMap[outJoinTarget] == outJoinAccompany
                }
            } else {
                outJoinTargets.filter { outJoinTarget ->
                    outJoinAccompany as Collection<Any>
                    outJoinAccompany.contains(outJoinTarget.buildInModelBuilder!!
                        .targetToAccompanyMap[outJoinTarget])
                }
            }
            target
        } as Map<A, OJT>
    }
}