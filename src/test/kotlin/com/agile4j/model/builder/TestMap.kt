package com.agile4j.model.builder

/**
 * @author liurenpeng
 * Created on 2020-07-12
 */

fun main() {
    val map = mutableMapOf<String, String>("1" to "one")
    val holder1 = Holder()
    val holder2 = Holder()

    holder1.map = map
    holder2.map = map

    holder1.map["2"] = "two"
    println("${holder2.map}")
}

class Holder {
    var map = mutableMapOf<String, String>()
}