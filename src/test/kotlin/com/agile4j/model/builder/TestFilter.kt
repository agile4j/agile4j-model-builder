package com.agile4j.model.builder

import com.agile4j.model.builder.mock.MovieView
import org.junit.Assert
import org.junit.Test

/**
 * 过滤器
 * @author liurenpeng
 * Created on 2020-11-20
 */

class TestFilter: BaseTest() {
    @Test
    fun testFilter() {
        val views = movieIds1A2 mapMulti MovieView::class filterBy {it.id > 1}
        Assert.assertEquals(views.size, 1)
        printMultiMovieView(views)
    }
}