package com.agile4j.model.builder

import com.agile4j.model.builder.accessor.JoinAccessor
import com.agile4j.model.builder.accessor.JoinTargetAccessor
import com.agile4j.model.builder.accessor.OutJoinAccessor
import com.agile4j.model.builder.accessor.OutJoinTargetAccessor
import com.agile4j.model.builder.delegate.ModelBuilderDelegate
import com.agile4j.utils.open.OpenPair
import com.agile4j.utils.util.ArrayUtil
import com.agile4j.utils.util.CollectionUtil
import java.lang.reflect.ParameterizedType
import java.util.Collections.singleton
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.createType
import kotlin.reflect.jvm.javaGetter
import kotlin.reflect.jvm.jvmErasure

/**
 * @author liurenpeng
 * Created on 2020-06-04
 */

class ModelBuilder {
    val targetToAccompanyMap: MutableMap<Any, Any> = mutableMapOf()
    val indexToAccompanyMap: MutableMap<Any, Any> = mutableMapOf()
    val accompanyToIndexMap: MutableMap<Any, Any> = mutableMapOf()

    val joinAccessorMap : MutableMap<KClass<*>, JoinAccessor<Any, Any, Any>> = mutableMapOf()
    val joinTargetAccessorMap : MutableMap<KClass<*>, JoinTargetAccessor<Any, Any, Any>> = mutableMapOf()
    val outJoinAccessorMap : MutableMap<String, OutJoinAccessor<Any, Any, Any>> = mutableMapOf()
    val outJoinTargetAccessorMap : MutableMap<String, OutJoinTargetAccessor<Any, Any, Any>> = mutableMapOf()
}

class BuildSinglePair<out T>(modelBuilder: ModelBuilder, value: T) : OpenPair<ModelBuilder, T>(modelBuilder, value)
val <T> BuildSinglePair<T>.modelBuilder get() = first
val <T> BuildSinglePair<T>.targetClazz get() = second

class BuildMultiPair<out T>(modelBuilder: ModelBuilder, value: T) : OpenPair<ModelBuilder, T>(modelBuilder, value)
val <T> BuildMultiPair<T>.modelBuilder get() = first
val <T> BuildMultiPair<T>.targetClazz get() = second

infix fun <T : Any> ModelBuilder.buildSingle(clazz: KClass<T>) =
    BuildSinglePair(this, clazz)

infix fun <T : Any> ModelBuilder.buildMulti(clazz: KClass<T>) =
    BuildMultiPair(this, clazz)

infix fun <T : Any, I> BuildSinglePair<KClass<T>>.by(index: I): T? {
    val coll = this.modelBuilder buildMulti this.targetClazz by singleton(index)
    return if (CollectionUtil.isEmpty(coll)) null else coll.toList()[0]
}

infix fun <T : Any, I> BuildMultiPair<KClass<T>>.by(indies: Collection<I>) : Collection<T> {
    val targets = buildTargets(this, indies)
    injectModelBuilder(this, targets)
    injectRelation(targets)
    return targets
}

fun <T : Any, A : Any> buildAndInjectTargetsByAccompanies(
    pair: BuildMultiPair<KClass<T>>, accompanies: Collection<A>) : Collection<T> {
    val targets = buildTargetsByAccompanies(pair, accompanies)
    injectModelBuilder(pair, targets)
    injectRelation(targets)
    return targets
}

@Suppress("UNCHECKED_CAST")
private fun <T : Any, A : Any> buildTargetsByAccompanies(
    buildMultiPair: BuildMultiPair<KClass<T>>, accompanies: Collection<A>): Set<T> {
    val accompanyClazz = BuildContext.accompanyHolder[buildMultiPair.targetClazz]
    val accompanyIndexer = BuildContext.indexerHolder[accompanyClazz] as (A) -> Any
    val accompanyMap = accompanies.map { accompanyIndexer.invoke(it) to it }.toMap() as Map<out Any, Any>
    buildMultiPair.modelBuilder.indexToAccompanyMap.putAll(accompanyMap)
    buildMultiPair.modelBuilder.accompanyToIndexMap.putAll(accompanyMap.map { (k, v) -> v to k })
    return accompanyMap.values.map { accompany ->
        val target = buildMultiPair.targetClazz.constructors.stream()
            .filter { it.parameters.size == 1 }
            .filter { it.parameters[0].type == accompanyClazz!!.createType() }
            .findFirst().map { it.call(accompany) }.orElse(null)
        target?.let { buildMultiPair.modelBuilder.targetToAccompanyMap.put(it, accompany) }
        target
    }.toSet()
}

@Suppress("UNCHECKED_CAST")
private fun <I, T : Any> buildTargets(
    buildMultiPair: BuildMultiPair<KClass<T>>, indies: Collection<I>): Set<T> {
    val accompanyClazz = BuildContext.accompanyHolder[buildMultiPair.targetClazz]
    val accompanyBuilder = BuildContext.builderHolder[accompanyClazz] as (Collection<I>) -> Map<Any, Any>
    val accompanyMap = accompanyBuilder.invoke(indies)
    buildMultiPair.modelBuilder.indexToAccompanyMap.putAll(accompanyMap)
    buildMultiPair.modelBuilder.accompanyToIndexMap.putAll(accompanyMap.map { (k, v) -> v to k })
    return accompanyMap.values.map { accompany ->
        val target = buildMultiPair.targetClazz.constructors.stream()
            .filter { it.parameters.size == 1 }
            .filter { it.parameters[0].type == accompanyClazz!!.createType() }
            .findFirst().map { it.call(accompany) }.orElse(null)
        target?.let { buildMultiPair.modelBuilder.targetToAccompanyMap.put(it, accompany) }
        target
    }.toSet()
}

private fun <T : Any> injectModelBuilder(buildMultiPair: BuildMultiPair<KClass<T>>, targets: Set<T>) {
    targets.forEach { it.buildInModelBuilder = buildMultiPair.modelBuilder }
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
    val accompanyClazz = target.buildInModelBuilder!!.indexToAccompanyMap.values.elementAt(0)::class
    val joinAccessorMap = target.buildInModelBuilder?.joinAccessorMap
    BuildContext.joinHolder[accompanyClazz]?.forEach { (joinClazz, _) ->
        joinAccessorMap!![joinClazz] = JoinAccessor(joinClazz)
    }
}

private fun <T : Any> injectJoinTargetAccessor(targets: Set<T>) {
    targets.forEach (::injectSingleTargetJoinTargetAccessor )
}

@Suppress("UNCHECKED_CAST")
private fun <T : Any> injectSingleTargetJoinTargetAccessor(target: T) {
    val accompanyClazz = target.buildInModelBuilder!!.indexToAccompanyMap.values.elementAt(0)::class
    val joinTargetAccessorMap = target.buildInModelBuilder?.joinTargetAccessorMap
    BuildContext.joinHolder[accompanyClazz]?.forEach { (joinClazz, _) ->
        var joinTargetClazz: KClass<*>? = BuildContext.accompanyHolder
            .map { (k, v) -> v to k }.toMap()[joinClazz] ?: return@forEach
        joinTargetClazz = joinTargetClazz as KClass<Any>
        joinTargetAccessorMap!![joinTargetClazz] =
            JoinTargetAccessor(joinTargetClazz)
    }
}

private fun <T : Any> injectOutJoinAccessor(targets: Set<T>) {
    targets.forEach (::injectSingleTargetOutJoinAccessor )
}

private fun <T : Any> injectSingleTargetOutJoinAccessor(target: T) {
    val accompanyClazz = target.buildInModelBuilder!!.indexToAccompanyMap.values.elementAt(0)::class
    val outJoinAccessorMap = target.buildInModelBuilder?.outJoinAccessorMap
    BuildContext.outJoinHolder[accompanyClazz]?.forEach { (outJoinPoint, _) ->
        outJoinAccessorMap!![outJoinPoint] = OutJoinAccessor(outJoinPoint)
    }
}

private fun <T : Any> injectOutJoinTargetAccessor(targets: Set<T>) {
    targets.forEach (::injectSingleTargetOutJoinTargetAccessor )
}

private fun <T : Any> injectSingleTargetOutJoinTargetAccessor(target: T) {
    val accompanyClazz = target.buildInModelBuilder!!.indexToAccompanyMap.values.elementAt(0)::class
    val outJoinTargetAccessorMap = target.buildInModelBuilder?.outJoinTargetAccessorMap
    BuildContext.outJoinHolder[accompanyClazz]?.forEach { (outJoinPoint, _) ->
        outJoinTargetAccessorMap!![outJoinPoint] =
            OutJoinTargetAccessor(outJoinPoint)
    }
}

var Any.buildInModelBuilder : ModelBuilder? by ModelBuilderDelegate()

fun isBuildTargetClass(property: KProperty<*>) : Boolean {
    val isCollection = Collection::class.java.isAssignableFrom(property.returnType.jvmErasure.java)
    if (!isCollection) {
        return BuildContext.accompanyHolder.keys.contains(property.returnType.jvmErasure)
    }

    val type = property.javaGetter!!.genericReturnType as? ParameterizedType
        ?: return isBuildTargetClass(property.returnType.jvmErasure)
    val actualTypeArguments = type.actualTypeArguments
    if (ArrayUtil.isEmpty(actualTypeArguments)) {
        return false
    }
    val actualTypeArgumentType = actualTypeArguments[0]
    return BuildContext.accompanyHolder.keys.map { it.java }.contains(actualTypeArgumentType)
}


fun isBuildTargetClass(clazz: KClass<*>) : Boolean {
    return BuildContext.accompanyHolder.keys.contains(clazz)
}

