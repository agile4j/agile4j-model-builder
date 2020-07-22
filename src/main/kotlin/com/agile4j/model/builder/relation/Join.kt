package com.agile4j.model.builder.relation

import com.agile4j.model.builder.build.BuildContext
import com.agile4j.utils.open.OpenPair
import kotlin.reflect.KClass

/**
 * @author liurenpeng
 * Created on 2020-06-04
 */

class JoinPair<out A, out J>(first: A, second: J) : OpenPair<A, J>(first, second)
val <A, J> JoinPair<A, J>.aClazz get() = first
val <A, J> JoinPair<A, J>.jClazz get() = second

infix fun <A: Any, J: Any> KClass<A>.join(clazz: KClass<J>) = JoinPair(this, clazz)

infix fun <A: Any, J: Any, JI> JoinPair<KClass<A>, KClass<J>>.by(mapper: (A) -> JI) {
    val joinClazzToMapperMap = BuildContext
        .joinHolder.computeIfAbsent(this.aClazz) {mutableMapOf()}
    val mappers = joinClazzToMapperMap.computeIfAbsent(this.jClazz) {mutableListOf()}
    mappers.add(mapper)
}
