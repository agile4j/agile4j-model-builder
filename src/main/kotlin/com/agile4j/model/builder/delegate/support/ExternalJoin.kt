package com.agile4j.model.builder.delegate.support

import com.agile4j.model.builder.accessor.BaseOutJoinAccessor
import com.agile4j.model.builder.accessor.OutJoinAccessor.Companion.outJoinAccessor
import com.agile4j.model.builder.accessor.OutJoinTargetAccessor.Companion.outJoinTargetAccessor
import com.agile4j.model.builder.build.buildInModelBuilder
import com.agile4j.model.builder.delegate.ITargetDelegate
import kotlin.reflect.KProperty

/**
 * @author liurenpeng
 * Created on 2020-06-18
 */
@Suppress("UNCHECKED_CAST")
class ExternalJoin<M>(private val exJoinPoint: String) : ITargetDelegate<M> {

    override fun buildTarget(outerTarget: Any, property: KProperty<*>): M? =
        build(outerTarget, ::outJoinTargetAccessor)

    override fun buildAccompany(outerTarget: Any, property: KProperty<*>): M? =
        build(outerTarget, ::outJoinAccessor)

    private fun build(
        outerTarget: Any,
        accessor: (String) -> BaseOutJoinAccessor<Any, Any, Any>
    ): M? {
        val modelBuilder = outerTarget.buildInModelBuilder
        val outerAccompany = modelBuilder.targetToAccompanyMap[outerTarget]!!
        val accompanies = modelBuilder.indexToAccompanyMap.values
        return accessor(exJoinPoint).get(accompanies)[outerAccompany] as M?
    }

    companion object {
        fun <M> exJoin(outJoinPoint: String) = ExternalJoin<M>(outJoinPoint)
    }
}