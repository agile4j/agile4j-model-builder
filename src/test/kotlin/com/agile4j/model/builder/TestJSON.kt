package com.agile4j.model.builder

import com.agile4j.model.builder.mock.MovieView
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.junit.Assert
import org.junit.Test


/**
 * @author liurenpeng
 * Created on 2020-08-03
 */

class TestJSON: BaseTest() {
    @Test
    fun test() {
        val movieView = movieId1 mapSingle MovieView::class
        val mapper = ObjectMapper().registerKotlinModule()
        val currJsonStr = mapper.writeValueAsString(movieView)
        println("currJsonStr:$currJsonStr")
        Assert.assertEquals(1190, currJsonStr.length)
    }
}