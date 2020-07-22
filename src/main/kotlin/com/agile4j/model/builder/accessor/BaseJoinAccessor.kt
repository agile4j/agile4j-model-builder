package com.agile4j.model.builder.accessor

import com.agile4j.model.builder.ModelBuildException
import com.agile4j.model.builder.ModelBuildException.Companion.err
import com.agile4j.model.builder.build.BuildContext
import com.agile4j.model.builder.delegate.ITargetDelegate
import com.agile4j.utils.util.CollectionUtil
import com.agile4j.utils.util.MapUtil
import kotlin.reflect.KClass

/**
 * TODO 支持Collection<JM>
 * abbreviations:
 * A        accompany
 * JI       joinIndex
 * JM       joinModel: if [JoinAccessor] JM else [JoinTargetAccessor] JT
 * @author liurenpeng
 * Created on 2020-07-22
 */
@Suppress("UNCHECKED_CAST")
abstract class BaseJoinAccessor<A: Any, JI:Any, JM: Any>(
    private val joinClazz: KClass<Any>
) {

    protected val modelBuilder = ITargetDelegate.ScopeKeys.modelBuilderScopeKey.get()
        ?: throw ModelBuildException("modelBuilderScopeKey not init")

    abstract val allCached: Map<JI, JM>

    abstract val jmIsTargetRelated: Boolean

    abstract fun get(accompanies: Collection<A>): Map<A, Map<JI, JM>>

    private fun getRealJoinClazz(joinClazz: KClass<Any>) =
        if (jmIsTargetRelated) BuildContext.accompanyHolder[joinClazz] else joinClazz

    protected fun <JX> getMappers(accompanies: Collection<A>): List<(A) -> JI> {
        if (CollectionUtil.isEmpty(accompanies)) err("accompanies is empty")
        val accompanyClazz = accompanies.first()::class
        val joinClazzToMapperMap = BuildContext.joinHolder[accompanyClazz]
        if (MapUtil.isEmpty(joinClazzToMapperMap)) err("joinClazzToMapperMap is empty")
        val mappers = joinClazzToMapperMap!![getRealJoinClazz(joinClazz)]
        if (CollectionUtil.isEmpty(mappers)) err("mappers is empty")
        return mappers!!.toList() as List<(A) -> JI>
    }
}