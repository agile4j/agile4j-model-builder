package com.agile4j.model.builder

import com.agile4j.model.builder.mock.MovieView
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberExtensionProperties
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.memberExtensionProperties
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.staticProperties
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

    /*M::class iiiBy M::id
    M::class iiiBy fun(m: M) = m.id*/

    delegate()
}

fun delegate() {
    MovieView::class.memberProperties.forEach { println(it.name) }
}

fun properties() {
    println("-----staticProperties")
    MovieView::class.staticProperties.forEach { println(it.name) }
    println()

    println("-----declaredMemberExtensionProperties")
    MovieView::class.declaredMemberExtensionProperties.forEach { println(it.name) }
    println()

    println("-----memberExtensionProperties")
    MovieView::class.memberExtensionProperties.forEach { println(it.name) }
    println()

    println("-----memberProperties")
    MovieView::class.memberProperties.forEach { println(it.name) }
    println()

    println("-----declaredMemberProperties")
    MovieView::class.declaredMemberProperties.forEach { println(it.name) }
    println()
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