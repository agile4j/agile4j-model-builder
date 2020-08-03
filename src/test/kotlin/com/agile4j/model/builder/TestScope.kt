package com.agile4j.model.builder

import com.agile4j.model.builder.mock.MockScopes
import com.agile4j.model.builder.mock.MovieView
import com.agile4j.model.builder.mock.isShared
import org.junit.Assert
import org.junit.Test

/**
 * @author liurenpeng
 * Created on 2020-08-03
 */

class TestScope: BaseTest() {

    /**
     * [MockScopes.visitor]的值在[before]中被设置为1
     * [isShared]的判断逻辑为movieId与visitor值是否相等，相同则为true
     */
    @Test
    fun test() {
        val movieView1 = movieId1 mapSingle MovieView::class
        Assert.assertTrue(movieView1?.shared!!)
        val movieView3 = movieId3 mapSingle MovieView::class
        Assert.assertFalse(movieView3?.shared!!)
    }
}