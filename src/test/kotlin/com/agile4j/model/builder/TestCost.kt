package com.agile4j.model.builder

import com.agile4j.model.builder.build.ModelBuilder
import com.agile4j.model.builder.mock.MovieView
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
        println("st0" + System.nanoTime())
        Caffeine.newBuilder().build<Any?, Any?>()
        println("ca0" + System.nanoTime())
        Caffeine.newBuilder().build<Any?, Any?>()
        println("ca0" + System.nanoTime())
        ModelBuilder()
        println("mb0" + System.nanoTime())
        ModelBuilder()
        println("mb0" + System.nanoTime())
        Any()
        println("an0" + System.nanoTime())
        Any()
        println("an0" + System.nanoTime())
        val map: MutableMap<String, String> = mutableMapOf()
        println("ma0" + System.nanoTime())
        val mp: MutableMap<String, String> = mutableMapOf()
        println("ma0" + System.nanoTime())
    }

    @Test
    fun testNano() {
        println("nano-----" + System.nanoTime())
        println("nano-----" + System.nanoTime())
        println("nano-----" + System.nanoTime())
        println("nano-----" + System.nanoTime())
        println("nano-----" + System.nanoTime())
        println("nano-----" + System.nanoTime())
    }
}