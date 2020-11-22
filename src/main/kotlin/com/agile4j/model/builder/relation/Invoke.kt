package com.agile4j.model.builder.relation

import kotlin.reflect.KClass

/**
 * @author liurenpeng
 * Created on 2020-11-11
 */
operator fun <A: Any> KClass<A>.invoke(relation: KClass<A>.() -> Unit):KClass<A>  {
    relation()
    return this
}

fun <A: Any> KClass<A>.targets(vararg targets: KClass<*>) {
    targets.forEach { it accompanyBy this }
}