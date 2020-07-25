package com.agile4j.model.builder.relation

import com.agile4j.model.builder.build.BuildContext
import com.agile4j.utils.open.OpenPair
import kotlin.reflect.KClass

/**
 * abbreviations:
 * A        accompany
 * IJ       inJoin
 * IJI      inJoinIndex
 * @author liurenpeng
 * Created on 2020-06-04
 */

class InJoinPair<out A, out IJ>(first: A, second: IJ) : OpenPair<A, IJ>(first, second)
val <A, IJ> InJoinPair<A, IJ>.aClazz get() = first
val <A, IJ> InJoinPair<A, IJ>.ijClazz get() = second

infix fun <A: Any, IJ: Any> KClass<A>.inJoin(ijClazz: KClass<IJ>) = InJoinPair(this, ijClazz)

infix fun <A: Any, IJ: Any, IJI> InJoinPair<KClass<A>, KClass<IJ>>.by(mapper: (A) -> IJI) {
    val joinClazzToMapperMap = BuildContext
        .inJoinHolder.computeIfAbsent(this.aClazz) {mutableMapOf()}
    val mappers = joinClazzToMapperMap.computeIfAbsent(this.ijClazz) {mutableListOf()}
    mappers.add(mapper)
}
