package com.agile4j.model.builder

import com.agile4j.model.builder.mock.MovieView
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.junit.Test
import kotlin.concurrent.thread

/**
 * 并发线程安全
 * @author liurenpeng
 * Created on 2020-09-22
 */

class TestConcurrent: BaseTest() {

    private val mapper = ObjectMapper().registerKotlinModule()

    @Test
    fun testConcurrent() {
        fun run() = thread(start = true, isDaemon = false, name = "mbTest", priority = 3) {
            val views = movieIds1A2 mapMulti MovieView::class
            val json = mapper.writeValueAsString(views)
            println(json.length)
        }
    }


}