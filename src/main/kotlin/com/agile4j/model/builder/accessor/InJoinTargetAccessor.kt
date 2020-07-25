package com.agile4j.model.builder.accessor

import com.agile4j.model.builder.buildMulti
import com.agile4j.model.builder.by
import com.agile4j.model.builder.delegate.JoinDelegate.ScopeKeys.modelBuilder
import kotlin.reflect.KClass

/**
 * abbreviations:
 * A        accompany
 * JI       joinIndex
 * JT       joinTarget
 * @author liurenpeng
 * Created on 2020-06-18
 */
@Suppress("UNCHECKED_CAST")
class InJoinTargetAccessor<A: Any, JI:Any, JT: Any>(
    private val joinClazz: KClass<Any>
) : BaseInJoinAccessor<A, JI, JT>(joinClazz) {

    override val jmIsTargetRelated: Boolean get() = true

    override val allCached: Map<JI, JT>
        get() = modelBuilder().getJoinTargetCacheMap(joinClazz) as Map<JI, JT>

    override fun buildJiToJm(unCachedJis: Collection<JI>): Map<JI, JT> {
        modelBuilder() buildMulti joinClazz by unCachedJis
        val buildJiToJt = modelBuilder().iToT as Map<JI, JT>
        modelBuilder().putAllJoinTargetCacheMap(joinClazz, buildJiToJt)  // 入缓存
        return buildJiToJt
    }

    companion object {
        fun <A: Any, JI: Any, JM: Any> joinTargetAccessor(
            joinClazz: KClass<Any>
        ): BaseInJoinAccessor<A, JI, JM> = InJoinTargetAccessor(joinClazz)
    }
}