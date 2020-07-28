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

class MultiInJoinPair<out A, out IJ>(first: A, second: IJ) : OpenPair<A, IJ>(first, second)
val <A, IJ> MultiInJoinPair<A, IJ>.aClazz get() = first
val <A, IJ> MultiInJoinPair<A, IJ>.ijClazz get() = second

infix fun <A: Any, IJ: Any> KClass<A>.multiInJoin(ijClazz: KClass<IJ>) =
    MultiInJoinPair(this, ijClazz)

infix fun <A: Any, IJ: Any, IJI> MultiInJoinPair<KClass<A>, KClass<IJ>>.by(
    mapper: (A) -> Collection<IJI>
) {
    val ijClazzToMapperMap = BuildContext
        .multiInJoinHolder.computeIfAbsent(this.aClazz) { mutableMapOf() }
    val mappers = ijClazzToMapperMap.computeIfAbsent(this.ijClazz) { mutableListOf() }
    mappers.add(mapper)
}
