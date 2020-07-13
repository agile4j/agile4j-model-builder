package com.agile4j.model.builder

import com.agile4j.model.builder.ObjectMapperUtils.toJSON

/**
 * @author liurenpeng
 * Created on 2020-07-12
 */

fun main() {
    val a1 = A(1)
    val b1 = B(1)
    a1.b = b1
    b1.a = a1
    println(toJSON(a1))
}

data class A (val id: Long) {
    var b: B = B(id)
}

data class B (val id: Long) {
    var a: A = A(id)
}