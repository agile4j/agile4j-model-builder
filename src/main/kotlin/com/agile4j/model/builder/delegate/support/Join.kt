package com.agile4j.model.builder.delegate.support

import com.agile4j.model.builder.ModelBuildException
import com.agile4j.model.builder.accessor.BaseJoinAccessor
import com.agile4j.model.builder.accessor.JoinAccessor.Companion.joinAccessor
import com.agile4j.model.builder.accessor.JoinTargetAccessor.Companion.joinTargetAccessor
import com.agile4j.model.builder.build.buildInModelBuilder
import com.agile4j.model.builder.delegate.ITargetDelegate
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.jvmErasure

/**
 * @author liurenpeng
 * Created on 2020-06-18
 */
@Suppress("UNCHECKED_CAST")
class Join<T>(private val joinFieldName: String) : ITargetDelegate<T> {

    override fun buildTarget(outerTarget: Any, property: KProperty<*>): T =
        build(outerTarget, property, ::joinTargetAccessor)

    override fun buildAccompany(outerTarget: Any, property: KProperty<*>): T =
        build(outerTarget, property, ::joinAccessor)

    private fun build(
        outerTarget: Any,
        property: KProperty<*>,
        accessor: (KClass<Any>) -> BaseJoinAccessor<Any, Any, Any>
    ): T {
        val modelBuilder = outerTarget.buildInModelBuilder
        val joinClazz = property.returnType.jvmErasure as KClass<Any>
        val accompany = modelBuilder.targetToAccompanyMap[outerTarget]!!
        val accompanies = modelBuilder.indexToAccompanyMap.values
        val joinIndex = accompany.javaClass.kotlin.memberProperties.stream()
            .filter { joinFieldName == it.name }.findFirst().map { it.get(accompany) }
            .orElseThrow { ModelBuildException("not found joinIndex. " +
                    "outerTarget:$outerTarget accompany:$accompany joinFieldName:$joinFieldName")}
        return accessor(joinClazz).get(accompanies)[accompany]!![joinIndex] as T
    }
}