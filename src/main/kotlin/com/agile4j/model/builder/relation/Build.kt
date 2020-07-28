package com.agile4j.model.builder.relation

import com.agile4j.model.builder.build.BuildContext
import kotlin.reflect.KClass

/**
 * abbreviations:
 * I        index
 * A        accompany
 * @author liurenpeng
 * Created on 2020-06-17
 */

infix fun <A: Any, I> KClass<A>.buildBy(builder: (Collection<I>) -> Map<I, A>) {
    BuildContext.builderHolder[this] = builder
}