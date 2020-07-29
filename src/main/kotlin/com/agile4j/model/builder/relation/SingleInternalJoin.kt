package com.agile4j.model.builder.relation

import com.agile4j.model.builder.build.BuildContext
import com.agile4j.utils.open.OpenPair
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

/**
 * abbreviations:
 * A        accompany
 * IJ       inJoin
 * IJI      inJoinIndex
 * @author liurenpeng
 * Created on 2020-06-04
 */

class SingleInJoinPair<out A, out IJ>(first: A, second: IJ) : OpenPair<A, IJ>(first, second)
val <A, IJ> SingleInJoinPair<A, IJ>.aClazz get() = first
val <A, IJ> SingleInJoinPair<A, IJ>.ijClazz get() = second

infix fun <A: Any, IJ: Any> KClass<A>.singleInJoin(ijClazz: KClass<IJ>) = SingleInJoinPair(this, ijClazz)

infix fun <A: Any, IJ: Any, IJI> SingleInJoinPair<KClass<A>, KClass<IJ>>.by(
    mapper: (A) -> IJI
) {
    val ijClazzToMapperMap = BuildContext
        .singleInJoinHolder.computeIfAbsent(this.aClazz) { ConcurrentHashMap() }
    val mappers = ijClazzToMapperMap.computeIfAbsent(this.ijClazz) { mutableSetOf() }
    mappers.add(mapper)
}
