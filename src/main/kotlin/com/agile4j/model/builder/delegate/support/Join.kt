package com.agile4j.model.builder.delegate.support

import com.agile4j.model.builder.accessor.BaseJoinAccessor
import com.agile4j.model.builder.accessor.JoinAccessor.Companion.joinAccessor
import com.agile4j.model.builder.accessor.JoinTargetAccessor.Companion.joinTargetAccessor
import com.agile4j.model.builder.build.buildInModelBuilder
import com.agile4j.model.builder.delegate.ITargetDelegate
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.jvmErasure

/**
 * @author liurenpeng
 * Created on 2020-06-18
 */
@Suppress("UNCHECKED_CAST")
class Join<T: Any, A: Any, JI: Any>(private val mapper: (A) -> JI) : ITargetDelegate<T> {

    override fun buildTarget(outerTarget: Any, property: KProperty<*>): T? =
        build(outerTarget, property, ::joinTargetAccessor)

    override fun buildAccompany(outerTarget: Any, property: KProperty<*>): T? =
        build(outerTarget, property, ::joinAccessor)

    private fun build(
        outerTarget: Any,
        property: KProperty<*>,
        accessor: (KClass<Any>) -> BaseJoinAccessor<A, JI, Any>
    ): T? {
        val modelBuilder = outerTarget.buildInModelBuilder
        val joinClazz = property.returnType.jvmErasure as KClass<Any>
        val accompany = modelBuilder.targetToAccompanyMap[outerTarget]!! as A
        val accompanies = modelBuilder.indexToAccompanyMap.values as Collection<A>
        val joinIndex = mapper.invoke(accompany)
        return accessor(joinClazz).get(accompanies)[accompany]?.get(joinIndex) as T?
    }
}