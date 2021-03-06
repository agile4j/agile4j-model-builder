package com.agile4j.model.builder

import com.agile4j.model.builder.delegate.ModelBuilderDelegate
import com.agile4j.model.builder.mock.MovieView
import org.junit.Assert
import org.junit.Test

/**
 * 测试内存泄露
 * Created on 2020-07-23
 */

class TestMemoryLeak: BaseTest() {

    /**
     * 仅初始化还未对字段求值的view，是否存在内存泄露
     */
    @Test
    fun testInitView() {
        movieId1 mapSingle MovieView::class
        Assert.assertEquals(1, ModelBuilderDelegate.mapSize)

        movieId1 mapSingle MovieView::class
        Assert.assertEquals(2, ModelBuilderDelegate.mapSize)

        val movieView = movieId1 mapSingle MovieView::class
        Assert.assertEquals(3, ModelBuilderDelegate.mapSize)

        gcAndSleepAndRefreshWeakMapSize(movieView)
        Assert.assertEquals(1, ModelBuilderDelegate.mapSize)

        movieIds1A2 mapMulti MovieView::class
        Assert.assertEquals(3, ModelBuilderDelegate.mapSize)

        gcAndSleepAndRefreshWeakMapSize(movieView)
        Assert.assertEquals(1, ModelBuilderDelegate.mapSize)
    }

    /**
     * 初始化并已对字段求值的view，是否存在内存泄露
     */
    @Test
    fun testInitAndBuildView() {
        val movieView = movieId1 mapSingle MovieView::class
        printSingleMovieView(movieView, false)
        Assert.assertTrue(ModelBuilderDelegate.mapSize > 1)

        gcAndSleepAndRefreshWeakMapSize(movieView)
        Assert.assertTrue(ModelBuilderDelegate.mapSize == 1)


        val movieViews = movieIds1A2 mapMulti MovieView::class
        printMultiMovieView(movieViews, false)
        Assert.assertTrue(ModelBuilderDelegate.mapSize > 3)

        gcAndSleepAndRefreshWeakMapSize(movieViews)
        Assert.assertTrue(ModelBuilderDelegate.mapSize == 3)
    }

}