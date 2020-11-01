package com.agile4j.model.builder

import com.agile4j.model.builder.mock.MovieView
import com.agile4j.model.builder.mock.accessTimes
import org.junit.Assert
import org.junit.Test

/**
 * 耗时
 * @author liurenpeng
 * Created on 2020-09-22
 */

class TestBuildCache: BaseTest() {
    @Test
    fun testBuildCache() {
        val movieViews = movieIds1A2 mapMulti MovieView::class
        println(movieViews.first().authorView?.movieView)
        Assert.assertEquals(2, accessTimes.get())
    }
}