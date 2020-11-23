package com.agile4j.model.builder

import com.github.benmanes.caffeine.cache.Caffeine
import org.junit.Assert
import org.junit.Test

/**
 * 验证Caffeine是通过[System.identityHashCode]而非[Object.hashCode]计算hash值
 * @author liurenpeng
 * Created on 2020-11-20
 */

class TestMapIndex: BaseTest() {
    @Test
    fun testMap() {
        val map = Caffeine.newBuilder().weakKeys().build<Any, Any>()
        val aTD = TD(1)
        val bTD = TD(1)

        Assert.assertTrue(aTD == bTD)
        Assert.assertFalse(aTD === bTD)
        map.put(aTD, "atd")
        map.put(bTD, "btd")
        Assert.assertEquals(map.asMap().size, 2)
    }

    data class TD(val id: Int)
}