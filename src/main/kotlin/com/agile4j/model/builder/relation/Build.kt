package com.agile4j.model.builder.relation

import com.agile4j.model.builder.build.BuildContext
import kotlin.reflect.KClass

/**
 * @author liurenpeng
 * Created on 2020-06-17
 */

infix fun <A: Any, AI> KClass<A>.buildBy(builder: (Collection<AI>) -> Map<AI, A>) {
    BuildContext.builderHolder[this] = builder
}