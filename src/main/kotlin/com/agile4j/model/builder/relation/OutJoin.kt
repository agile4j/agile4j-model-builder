package com.agile4j.model.builder.relation

import com.agile4j.model.builder.build.BuildContext
import com.agile4j.utils.open.OpenPair
import kotlin.reflect.KClass

/**
 * @author liurenpeng
 * Created on 2020-06-04
 */

class OutJoinPair<out A>(first: A, second: String) : OpenPair<A, String>(first, second)
val <A> OutJoinPair<A>.aClazz get() = first
val <A> OutJoinPair<A>.outJoinPoint get() = second

infix fun <A: Any> KClass<A>.outJoin(outJoinPoint: String) = OutJoinPair(this, outJoinPoint)

infix fun <A: Any, OJX: Any, AI> OutJoinPair<KClass<A>>.by(mapper: (Collection<AI>) -> Map<AI, OJX>) {
    val outJoinPointToMapperMap = BuildContext
        .outJoinHolder.computeIfAbsent(this.aClazz) {mutableMapOf()}
    outJoinPointToMapperMap.putIfAbsent(this.outJoinPoint, mapper)
}