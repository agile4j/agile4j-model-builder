package com.agile4j.model.builder.relation

import com.agile4j.model.builder.build.BuildContext
import com.agile4j.utils.open.OpenPair
import kotlin.reflect.KClass

/**
 * abbreviations:
 * A        accompany
 * EJ       exJoin
 * EJI      exJoinIndex
 * @author liurenpeng
 * Created on 2020-06-04
 */

class ExJoinPair<out A, out EJ>(first: A, second: EJ) : OpenPair<A, EJ>(first, second)
val <A, EJ> ExJoinPair<A, EJ>.aClazz get() = first
val <A, EJ> ExJoinPair<A, EJ>.ejClazz get() = second

infix fun <A: Any, EJ: Any> KClass<A>.exJoin(ejClazz: KClass<EJ>) = ExJoinPair(this, ejClazz)

infix fun <A: Any, EJ: Any, AI> ExJoinPair<KClass<A>, KClass<EJ>>.by(
    mapper: (Collection<AI>) -> Map<AI, EJ>
) {
    val ejClazzToMapperMap = BuildContext
        .exJoinHolder.computeIfAbsent(this.aClazz) { mutableMapOf() }
    val mappers = ejClazzToMapperMap.computeIfAbsent(this.ejClazz) { mutableListOf() }
    mappers.add(mapper)
}