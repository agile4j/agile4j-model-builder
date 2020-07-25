package com.agile4j.model.builder

import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.jvm.reflect

/**
 * @author liurenpeng
 * Created on 2020-07-24
 */

fun main() {
    println("testFunction")
    /*val incr = fun(a: Int) = a + 1
    println("--:" + incr.reflect()?.returnType)*/

    //printReturnType(::incr)

    //printGFunReturnType(::incr)

    //M::class iBy M::id
    //M::class iBy fun(m: M) = m.id

    /*println((M::id)::class)
    println((fun(m: M) = m.id)::class)*/

    /*println((M::id).returnType)
    println(fun(m: M) = m.id)*/

    M::class iiiBy M::id
    M::class iiiBy fun(m: M) = m.id
}

fun incr(a: Int) = a + 1

fun printReturnType(f: (Int) -> Int) {
    println("--:" + f.reflect()?.returnType)
}

fun <P: Any, R> printGFunReturnType(f: (P) -> R) {
    println("--:" + f.reflect()?.returnType)
}


infix fun <A: Any, AI> KClass<A>.iBy(indexer: (A) -> AI) {
    println("--:" + indexer.reflect()?.returnType)
}

infix fun <A: Any, AI: Any> KClass<A>.iiBy(callable: KCallable<AI>) {
    println("++:" + callable.returnType)
}

infix fun <A: Any, AI: Any> KClass<A>.iiiBy(indexer: (A) -> AI) {
    if (indexer is KCallable<*>) {
        println("==:" + indexer.returnType)
    } else {
        println("==:" + indexer.reflect()?.returnType)
    }
}

data class M(val id: Long, val authorId: Long, val checkerId: Long) {
    val name: String by lazy { "" }
}