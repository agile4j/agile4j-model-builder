package com.agile4j.model.builder.relation

import com.agile4j.model.builder.build.BuildContext
import com.agile4j.utils.open.OpenPair
import kotlin.reflect.KClass

/**
 * @author liurenpeng
 * Created on 2020-06-04
 */

class JoinPair<out T, out J>(first: T, second: J) : OpenPair<T, J>(first, second)
val <T, J> JoinPair<T, J>.targetClazz get() = first
val <T, J> JoinPair<T, J>.joinClazz get() = second

infix fun <T: Any, J: Any> KClass<T>.join(clazz: KClass<J>) = JoinPair(this, clazz)

infix fun <T: Any, J: Any, JI> JoinPair<KClass<T>, KClass<J>>.by(mapper: (T) -> JI) {
    val joinClazzToMapperMap = BuildContext.joinHolder.computeIfAbsent(this.targetClazz) {mutableMapOf()}
    val mappers = joinClazzToMapperMap.computeIfAbsent(this.joinClazz) {mutableListOf()}
    mappers.add(mapper)
}
