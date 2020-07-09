package com.agile4j.model.builder

import com.agile4j.model.builder.accessor.JoinAccessor
import com.agile4j.model.builder.accessor.JoinTargetAccessor
import com.agile4j.model.builder.accessor.OutJoinAccessor
import com.agile4j.model.builder.accessor.OutJoinTargetAccessor
import com.agile4j.model.builder.build.buildTargets
import com.agile4j.model.builder.build.injectModelBuilder
import com.agile4j.model.builder.build.injectRelation
import com.agile4j.utils.open.OpenPair
import com.agile4j.utils.util.CollectionUtil
import java.util.*
import java.util.Collections.singleton
import kotlin.reflect.KClass

/**
 * @author liurenpeng
 * Created on 2020-06-04
 */

class ModelBuilder {
    val targetToAccompanyMap: MutableMap<Any, Any> = WeakHashMap()
    val indexToAccompanyMap: MutableMap<Any, Any> = WeakHashMap()
    val accompanyToIndexMap: MutableMap<Any, Any> = WeakHashMap()

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

