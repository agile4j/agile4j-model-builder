package com.agile4j.model.builder.relation

import com.agile4j.model.builder.build.BuildContext
import kotlin.reflect.KClass

/**
 * @author liurenpeng
 * Created on 2020-06-17
 */

infix fun <T: Any, I> KClass<T>.buildBy(builder: (Collection<I>) -> Map<I, T>) {
    BuildContext.builderHolder[this] = builder
}