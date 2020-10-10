package com.agile4j.model.builder

import com.agile4j.model.builder.mock.MovieView
import com.agile4j.model.builder.mock.Video
import com.agile4j.model.builder.mock.idBorder
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.benmanes.caffeine.cache.Caffeine
import org.junit.Test

/**
 * 耗时
 * @author liurenpeng
 * Created on 2020-09-22
 */

class TestCost: BaseTest() {
    @Test
    fun testBuilder() {
        val mapper = ObjectMapper().registerKotlinModule()
        val movieIds = (1..idBorder).toList()
        val startMilli = System.currentTimeMillis()
        val movieViews = movieIds mapMulti MovieView::class
        val jsonStr = mapper.writeValueAsString(movieViews)
        println("jsonStr:$jsonStr")
        val endMilli = System.currentTimeMillis()
        val cost = endMilli - startMilli
        println("cost:$cost")
    }

    //@Test
    fun testCaffeine() {
        val t1 = System.nanoTime()
        val c1 = Caffeine.newBuilder().weakValues().build<Any, Any>()
        val t2 = System.nanoTime()
        val c2 = Caffeine.newBuilder().weakValues().build<Any, Any>()
        val t3 = System.nanoTime()
        val c3 = Caffeine.newBuilder().weakValues().build<Any, Any>()
        val t4 = System.nanoTime()
        val c4 = Caffeine.newBuilder().weakValues().build<Any, Any>()
        val t5 = System.nanoTime()
        val c5 = Caffeine.newBuilder().weakValues().build<Any, Any>()
        val t6 = System.nanoTime()
        val c6 = Caffeine.newBuilder().weakValues().build<Any, Any>()
        val t7 = System.nanoTime()
        val c7 = Caffeine.newBuilder().weakValues().build<Any, Any>()
        val t8 = System.nanoTime()
        val c8 = Caffeine.newBuilder().weakValues().build<Any, Any>()
        val t9 = System.nanoTime()
        val c9 = Caffeine.newBuilder().weakValues().build<Any, Any>()
        val t10 = System.nanoTime()
        println("==========t1:$t1")
        println("==========t2:$t2  di:\t${t2 - t1}")
        println("==========t3:$t3  di:\t${t3 - t2}")
        println("==========t4:$t4  di:\t${t4 - t3}")
        println("==========t5:$t5  di:\t${t5 - t4}")
        println("==========t6:$t6  di:\t${t6 - t5}")
        println("==========t7:$t7  di:\t${t7 - t6}")
        println("==========t8:$t8  di:\t${t8 - t7}")
        println("==========t9:$t9  di:\t${t9 - t8}")
        println("=========t10:$t10  di:\t${t10 - t9}")
    }

    //@Test
    fun testIndexer1() {
        val times = 10000
        val video = Video(1)

        val startTime1 = System.nanoTime()
        for (index in 1..times){
            val i = video.id
        }
        val useTime1 = System.nanoTime() - startTime1

        println("useTime1:$useTime1") // 599503
    }

    //@Test
    @Suppress("UNCHECKED_CAST")
    fun testIndexer2() {
        val times = 10000
        val video = Video(1)
        val indexer = Video::id as (Any) -> Any

        val startTime2 = System.nanoTime()
        for (index in 1..times){
            val i = indexer.invoke(video)
        }
        val useTime2 = System.nanoTime() - startTime2

        println("useTime2:$useTime2") // 1239347
    }

    //@Test
    @Suppress("UNCHECKED_CAST")
    fun testIndexer3() {
        val times = 10000
        val video = Video(1)
        val kProperty = Video::id

        val startTime3 = System.nanoTime()
        for (index in 1..times){
            val i = kProperty.get(video)
        }
        val useTime3 = System.nanoTime() - startTime3

        println("useTime3:$useTime3") // 1438171
    }

    //@Test
    @Suppress("UNCHECKED_CAST")
    fun testIndexer4() {
        val times = 10000
        val video = Video(1)
        val map = mutableMapOf<Video, Long>()
        map[video] = 1

        val startTime4 = System.nanoTime()
        for (index in 1..times){
            val i = map[video]
        }
        val useTime4 = System.nanoTime() - startTime4

        println("useTime4:$useTime4") // 1105512
    }
}