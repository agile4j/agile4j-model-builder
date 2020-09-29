package com.agile4j.model.builder

import com.agile4j.model.builder.map.WeakValueHashMap
import org.junit.Assert
import org.junit.Test

/**
 * @author liurenpeng
 * Created on 2020-08-03
 */

class TestWeakValueHashMap: BaseTest() {

    @Test
    fun test() {
        val map = WeakValueHashMap<Any, Any>()
        val c: Any? = Any()
        var v: Any? = Any()
        map["c"] = c
        map["k"] = v
        println("c:" + map["c"])
        println("k:" + map["k"])
        v = null
        System.gc()
        println("c:" + map["c"])
        println("k:" + map["k"])
        Assert.assertNotNull(map["c"])
        Assert.assertNull(map["k"])
    }
}