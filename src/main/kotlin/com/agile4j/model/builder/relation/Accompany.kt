package com.agile4j.model.builder.relation

import com.agile4j.model.builder.build.BuildContext
import com.agile4j.model.builder.build.BuildContext.assertCanBeA
import com.agile4j.model.builder.build.BuildContext.assertCanBeT
import kotlin.reflect.KClass

/**
 * abbreviations:
 * A        accompany
 * T        target
 * @author liurenpeng
 * Created on 2020-06-17
 */

infix fun <T: Any, A: Any> KClass<T>.accompanyBy(aClazz: KClass<A>) {
    assertCanBeT(this)
    assertCanBeA(aClazz)
    BuildContext.tToAHolder[this] = aClazz
}