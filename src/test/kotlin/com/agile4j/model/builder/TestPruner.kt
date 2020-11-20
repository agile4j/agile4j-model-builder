package com.agile4j.model.builder

import com.agile4j.model.builder.mock.MockScopes
import com.agile4j.model.builder.mock.MovieView
import com.agile4j.utils.util.CollectionUtil
import org.junit.Assert
import org.junit.Test

/**
 * 耗时
 * @author liurenpeng
 * Created on 2020-09-22
 */

class TestPruner: BaseTest() {

    @Test
    fun testFetchCount() {
        val target = movieId1 mapSingle MovieView::class
        Assert.assertNotNull(target?.count)
    }

    @Test
    fun testNotFetchCount() {
        MockScopes.setFetchCount(false)
        val target = movieId1 mapSingle MovieView::class
        Assert.assertNull(target?.count)
    }

    @Test
    fun testFetchVideos() {
        val target = movieId1 mapSingle MovieView::class
        Assert.assertNotNull(target?.videos)
        Assert.assertTrue(CollectionUtil.isNotEmpty(target?.videos))
    }

    @Test
    fun testNotFetchVideos() {
        MockScopes.setFetchVideos(false)
        val target = movieId1 mapSingle MovieView::class
        Assert.assertNotNull(target?.videos)
        Assert.assertTrue(CollectionUtil.isEmpty(target?.videos))
    }
}