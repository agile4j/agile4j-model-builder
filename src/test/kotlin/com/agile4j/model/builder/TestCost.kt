package com.agile4j.model.builder

import com.agile4j.model.builder.build.BuildContext
import com.agile4j.model.builder.delegate.isColl
import com.agile4j.model.builder.delegate.isList
import com.agile4j.model.builder.delegate.isSet
import com.agile4j.model.builder.mock.MovieView
import com.agile4j.model.builder.mock.Video
import com.agile4j.model.builder.mock.getInteractionsByMovieIds
import com.agile4j.model.builder.mock.idBorder
import com.agile4j.model.builder.utils.returnKType
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.junit.Test
import java.lang.reflect.ParameterizedType
import kotlin.reflect.jvm.javaType

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

    @Test
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

    @Test
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

    @Test
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

    @Test
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


    @Test
    fun testMB() {
        // : (Collection<Long>) -> Map<Long, MovieInteraction?>
        println("0-----" + System.nanoTime())
        val mapper = ::getInteractionsByMovieIds
        println("1-----" + System.nanoTime())
        val type = (mapper.returnKType?.javaType as? ParameterizedType)?.actualTypeArguments?.get(1)!!
        println("2-----" + System.nanoTime())
        val isColl = !isColl(type)
        println("3-----" + System.nanoTime())
        val cType =  (type as? ParameterizedType) ?.actualTypeArguments?.get(0)
        println("4-----" + System.nanoTime())
        val isColl2 = isColl(type)
        println("5-----" + System.nanoTime())
        val isSet = isSet(type)
        println("6-----" + System.nanoTime())
        val isList = isList(type)
        println("7-----" + System.nanoTime())
        val isA = if (isColl) BuildContext.isA(cType) else BuildContext.isA(type)
        println("8-----" + System.nanoTime())
        val isI = if (isColl) BuildContext.isI(cType) else BuildContext.isI(type)
        println("9-----" + System.nanoTime())
        val isT = if (isColl) BuildContext.isT(cType) else BuildContext.isT(type)
    }
}