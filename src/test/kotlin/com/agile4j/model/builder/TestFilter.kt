package com.agile4j.model.builder

import com.agile4j.model.builder.build.buildInModelBuilder
import com.agile4j.model.builder.mock.MovieView
import org.junit.Test
import java.util.stream.Collectors

/**
 * 过滤器
 * @author liurenpeng
 * Created on 2020-11-20
 */

class TestFilter: BaseTest() {
    @Test
    fun testFilter() {
        val views = movieIds1A2 mapMulti MovieView::class
        val set = views.stream()
            .map { it.buildInModelBuilder }.collect(Collectors.toSet())
        println(set)
    }
}