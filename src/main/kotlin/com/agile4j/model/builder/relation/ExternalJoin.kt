package com.agile4j.model.builder.relation

import com.agile4j.model.builder.build.BuildContext
import com.agile4j.utils.open.OpenPair
import kotlin.reflect.KClass

/**
 * @author liurenpeng
 * Created on 2020-06-04
 */

class ExJoinPair<out A>(first: A, second: String) : OpenPair<A, String>(first, second)
val <A> ExJoinPair<A>.aClazz get() = first
val <A> ExJoinPair<A>.exJoinPoint get() = second

infix fun <A: Any> KClass<A>.exJoin(exJoinPoint: String) = ExJoinPair(this, exJoinPoint)

infix fun <A: Any, OJX: Any, AI> ExJoinPair<KClass<A>>.by(mapper: (Collection<AI>) -> Map<AI, OJX>) {
    val outJoinPointToMapperMap = BuildContext
        .exJoinHolder.computeIfAbsent(this.aClazz) { mutableMapOf() }
    outJoinPointToMapperMap.putIfAbsent(this.exJoinPoint, mapper)
}