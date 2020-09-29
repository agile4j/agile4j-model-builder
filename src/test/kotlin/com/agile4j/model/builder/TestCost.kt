package com.agile4j.model.builder

import com.agile4j.model.builder.build.BuildContext
import com.agile4j.model.builder.delegate.isColl
import com.agile4j.model.builder.delegate.isList
import com.agile4j.model.builder.delegate.isSet
import com.agile4j.model.builder.mock.MovieView
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
    fun test() {
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

    @Test
    fun testNano() {
        println("nano-----" + System.nanoTime())
        println("nano-----" + System.nanoTime())
        println("nano-----" + System.nanoTime())
        println("nano-----" + System.nanoTime())
        println("nano-----" + System.nanoTime())
        println("nano-----" + System.nanoTime())
        println("nano-----" + System.nanoTime())
        println("nano-----" + System.nanoTime())
        println("nano-----" + System.nanoTime())
    }
}