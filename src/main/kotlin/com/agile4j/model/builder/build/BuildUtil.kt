package com.agile4j.model.builder.build

import com.agile4j.model.builder.ModelBuildException
import com.agile4j.model.builder.accessor.JoinAccessor
import com.agile4j.model.builder.accessor.JoinTargetAccessor
import com.agile4j.model.builder.accessor.OutJoinAccessor
import com.agile4j.model.builder.accessor.OutJoinTargetAccessor
import com.agile4j.model.builder.delegate.ModelBuilderDelegate
import com.agile4j.utils.util.ArrayUtil
import com.agile4j.utils.util.CollectionUtil
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.createType
import kotlin.reflect.jvm.javaGetter
import kotlin.reflect.jvm.jvmErasure

/**
 * @author liurenpeng
 * Created on 2020-07-09
 */

internal val Any.buildInModelBuilder : ModelBuilder by ModelBuilderDelegate()

fun <I, T : Any> buildTargets(buildMultiPair: BuildMultiPair<KClass<T>>, sources: Collection<I>): Set<T> {
    val targets = initTargets(buildMultiPair, sources)
    injectModelBuilder(buildMultiPair, targets)
    injectRelation(targets)
    return targets
}

fun isTargetClass(property: KProperty<*>) : Boolean {
    val isCollection = Collection::class.java.isAssignableFrom(property.returnType.jvmErasure.java)
    if (!isCollection) {
        return isTargetClass(property.returnType.jvmErasure)
    }

    val type = property.javaGetter?.genericReturnType as? ParameterizedType
        ?: return isTargetClass(property.returnType.jvmErasure)
    val actualTypeArguments = type.actualTypeArguments
    if (ArrayUtil.isEmpty(actualTypeArguments)) {
        return false
    }
    val actualTypeArgumentType = actualTypeArguments[0]
    return isTargetClass(actualTypeArgumentType)
}

private fun isTargetClass(clazz: KClass<*>) : Boolean {
    return BuildContext.accompanyHolder.keys.contains(clazz)
}

private fun isTargetClass(type: Type) : Boolean {
    return BuildContext.accompanyHolder.keys.map { it.java }.contains(type)
}

@Suppress("UNCHECKED_CAST")
private fun <IOA, T : Any> initTargets(
    buildMultiPair: BuildMultiPair<KClass<T>>, indies: Collection<IOA>): Set<T> {
    if (CollectionUtil.isEmpty(indies)) {
        return emptySet()
    }
    if (!BuildContext.accompanyHolder.keys.contains(buildMultiPair.targetClazz)) {
        throw ModelBuildException("unregistered targetClass:" + buildMultiPair.targetClazz)
    }

    val accompanyClazz = BuildContext.accompanyHolder[buildMultiPair.targetClazz]!!
    val accompanyMap : Map<out Any, Any> = if (accompanyClazz.isInstance(indies.toList()[0])) {
        // buildByAccompany
        val accompanyIndexer = BuildContext.indexerHolder[accompanyClazz] as (IOA) -> Any
        indies.map { accompanyIndexer.invoke(it) to it}.toMap() as Map<out Any, Any>
    } else {
        // buildByAccompanyIndex
        val accompanyBuilder = BuildContext.builderHolder[accompanyClazz] as (Collection<IOA>) -> Map<Any, Any>
        accompanyBuilder.invoke(indies)
    }

    buildMultiPair.modelBuilder.indexToAccompanyMap.putAll(accompanyMap)
    buildMultiPair.modelBuilder.accompanyToIndexMap.putAll(accompanyMap.map { (k, v) -> v to k })
    return accompanyMap.values.map { accompany ->
        val target = buildMultiPair.targetClazz.constructors.stream()
            .filter { it.parameters.size == 1 }
            .filter { it.parameters[0].type == accompanyClazz.createType() }
            .findFirst().map { it.call(accompany) }.orElse(null)
        target?.let { buildMultiPair.modelBuilder.targetToAccompanyMap.put(it, accompany) }
        target
    }.toSet()
}

private fun <T : Any> injectModelBuilder(buildMultiPair: BuildMultiPair<KClass<T>>, targets: Set<T>) {
    targets.forEach { it.buildInModelBuilder.merge(buildMultiPair.modelBuilder) }
}

private fun <T : Any> injectRelation(targets: Set<T>) {
    injectJoinAccessor(targets)
    injectJoinTargetAccessor(targets)
    injectOutJoinAccessor(targets)
    injectOutJoinTargetAccessor(targets)
}

private fun <T : Any> injectJoinAccessor(targets: Set<T>) {
    targets.forEach (::injectSingleTargetJoinAccessor )
}

private fun <T : Any> injectSingleTargetJoinAccessor(target: T) {
    val accompanyClazz = target.buildInModelBuilder.indexToAccompanyMap.values.elementAt(0)::class
    val joinAccessorMap = target.buildInModelBuilder.joinAccessorMap
    BuildContext.joinHolder[accompanyClazz]?.forEach { (joinClazz, _) ->
        joinAccessorMap[joinClazz] = JoinAccessor(joinClazz)
    }
}

private fun <T : Any> injectJoinTargetAccessor(targets: Set<T>) {
    targets.forEach (::injectSingleTargetJoinTargetAccessor )
}

@Suppress("UNCHECKED_CAST")
private fun <T : Any> injectSingleTargetJoinTargetAccessor(target: T) {
    val accompanyClazz = target.buildInModelBuilder.indexToAccompanyMap.values.elementAt(0)::class
    val joinTargetAccessorMap = target.buildInModelBuilder.joinTargetAccessorMap
    BuildContext.joinHolder[accompanyClazz]?.forEach { (joinClazz, _) ->
        var joinTargetClazz: KClass<*>? = BuildContext.accompanyHolder
            .map { (k, v) -> v to k }.toMap()[joinClazz] ?: return@forEach
        joinTargetClazz = joinTargetClazz as KClass<Any>
        joinTargetAccessorMap[joinTargetClazz] =
            JoinTargetAccessor(joinTargetClazz)
    }
}

private fun <T : Any> injectOutJoinAccessor(targets: Set<T>) {
    targets.forEach (::injectSingleTargetOutJoinAccessor )
}

private fun <T : Any> injectSingleTargetOutJoinAccessor(target: T) {
    val accompanyClazz = target.buildInModelBuilder.indexToAccompanyMap.values.elementAt(0)::class
    val outJoinAccessorMap = target.buildInModelBuilder.outJoinAccessorMap
    BuildContext.outJoinHolder[accompanyClazz]?.forEach { (outJoinPoint, _) ->
        outJoinAccessorMap[outJoinPoint] = OutJoinAccessor(outJoinPoint)
    }
}

private fun <T : Any> injectOutJoinTargetAccessor(targets: Set<T>) {
    targets.forEach (::injectSingleTargetOutJoinTargetAccessor )
}

private fun <T : Any> injectSingleTargetOutJoinTargetAccessor(target: T) {
    val accompanyClazz = target.buildInModelBuilder.indexToAccompanyMap.values.elementAt(0)::class
    val outJoinTargetAccessorMap = target.buildInModelBuilder.outJoinTargetAccessorMap
    BuildContext.outJoinHolder[accompanyClazz]?.forEach { (outJoinPoint, _) ->
        outJoinTargetAccessorMap[outJoinPoint] =
            OutJoinTargetAccessor(outJoinPoint)
    }
}

