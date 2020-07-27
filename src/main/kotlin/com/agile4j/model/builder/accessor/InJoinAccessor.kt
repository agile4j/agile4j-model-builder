package com.agile4j.model.builder.accessor

/**
 * abbreviations:
 * A        accompany
 * JI       joinIndex
 * JM       joinModel
 * @author liurenpeng
 * Created on 2020-06-18
 */
/*
@Suppress("UNCHECKED_CAST")
class InJoinAccessor<A: Any, JI: Any, JM: Any>(
    private val joinClazz: KClass<Any>
) : BaseInJoinAccessor<A, JI, JM>(joinClazz) {

    override val jmIsTargetRelated: Boolean get() = false

    override val allCached: Map<JI, JM>
        get() = modelBuilder().getJoinCacheMap(joinClazz) as Map<JI, JM>

    override fun buildJiToJm(unCachedJis: Collection<JI>): Map<JI, JM> {
        val builder = builderHolder[joinClazz] as (Collection<JI>) -> Map<JI, JM>
        val buildJiToJm = builder.invoke(unCachedJis)
        modelBuilder().putAllJoinCacheMap(joinClazz, buildJiToJm) // 入缓存
        return buildJiToJm
    }

    companion object {
        fun <A: Any, JI: Any, JM: Any> joinAccessor(
            joinClazz: KClass<Any>
        ): BaseInJoinAccessor<A, JI, JM> = InJoinAccessor(joinClazz)
    }
}*/
