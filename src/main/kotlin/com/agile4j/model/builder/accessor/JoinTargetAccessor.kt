package com.agile4j.model.builder.accessor

import com.agile4j.model.builder.build.BuildContext
import com.agile4j.model.builder.build.ModelBuilder
import com.agile4j.model.builder.build.buildInModelBuilder
import com.agile4j.model.builder.buildMulti
import com.agile4j.model.builder.by
import com.agile4j.utils.access.CacheAccessor
import com.agile4j.utils.util.CollectionUtil
import com.agile4j.utils.util.MapUtil
import java.util.stream.Collectors
import kotlin.reflect.KClass

/**
 * JTAI:JoinTargetAccompanyIndex
 * @author liurenpeng
 * Created on 2020-06-18
 */
class JoinTargetAccessor<A: Any, JTAI, JT: Any>(private val joinTargetClazz: KClass<Any>) : CacheAccessor<A, Map<JTAI, JT>>() {
    @Suppress("UNCHECKED_CAST")
    override fun realGet(sources: Collection<A>): Map<A, Map<JTAI, JT>> {
        val accompanies = sources.toSet()
        if (CollectionUtil.isEmpty(accompanies)) return emptyMap()
        val joinAccompanyClazzToMapperMap = BuildContext.joinHolder[accompanies.elementAt(0)::class]
        if (MapUtil.isEmpty(joinAccompanyClazzToMapperMap)) return emptyMap()
        val joinAccompanyClazz = BuildContext.accompanyHolder[joinTargetClazz]
        val mapper = joinAccompanyClazzToMapperMap!![joinAccompanyClazz] as MutableList<(A) -> JTAI>

        val accompanyToJoinTargetAccompanyIndices : Map<A, Set<JTAI>> = accompanies.map { it to
                mapper.map { mapper -> (mapper.invoke(it)) }.toSet()}.toMap()
        val targets = ModelBuilder() buildMulti joinTargetClazz by (accompanyToJoinTargetAccompanyIndices.values
            .stream().flatMap { it.stream() }.collect(Collectors.toSet()) as Set<JTAI>)
        return accompanyToJoinTargetAccompanyIndices.mapValues { (_, joinTargetAccompanyIndices) ->
            val currTargets = targets.filter { joinTargetAccompanyIndices
                .contains(it.buildInModelBuilder!!.accompanyToIndexMap[
                        it.buildInModelBuilder!!.targetToAccompanyMap[it]]) }.toList()
            currTargets.map { target -> parseTargetToAccompanyIndex(target) to target }.toMap()
        } as Map<A, Map<JTAI, JT>>
    }

    @Suppress("UNCHECKED_CAST")
    fun parseTargetToAccompanyIndex(target : Any) : Any {
        val modelBuilder = target.buildInModelBuilder
        val accompanyToIndexMap = modelBuilder!!.indexToAccompanyMap.map { (k, v) -> v to k}.toMap()
        return modelBuilder.targetToAccompanyMap.mapValues { accompanyToIndexMap[it.value] }[target] ?: error("")
    }
}