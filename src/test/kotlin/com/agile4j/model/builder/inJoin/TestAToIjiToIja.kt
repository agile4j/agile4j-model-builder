package com.agile4j.model.builder.inJoin

import com.agile4j.model.builder.BaseTest
import com.agile4j.model.builder.mapSingle
import com.agile4j.model.builder.mock.MovieView
import com.agile4j.model.builder.mock.accessTimes
import org.junit.Assert
import org.junit.Test

/**
 * @author liurenpeng
 * Created on 2020-08-03
 */

class TestAToIjiToIja: BaseTest() {

    @Test
    fun test() {
        val movieView = movieId1 mapSingle MovieView::class
        Assert.assertEquals(1, accessTimes.get())
        val author = movieView?.author
        Assert.assertEquals(2, accessTimes.get())
        Assert.assertEquals(movieId1, author?.id)
    }
}