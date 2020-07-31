package com.agile4j.model.builder

import com.agile4j.model.builder.delegate.ModelBuilderDelegate.Companion.weakMapSize
import com.agile4j.model.builder.mock.MovieView
import org.junit.Assert
import org.junit.Test

/**
 * 测试内存泄露
 * @author liurenpeng
 * Created on 2020-07-23
 */

class TestMemoryLeak: BaseTest() {

    /**
     * 刚初始化还未对字段求值的view，是否存在内存泄露
     */
    @Test
    fun testInitViewMemoryLeak() {
        1L mapSingle MovieView::class
        Assert.assertEquals(1, weakMapSize.get())
        1L mapSingle MovieView::class
        Assert.assertEquals(2, weakMapSize.get())

        val movieView = 1L mapSingle MovieView::class
        Assert.assertEquals(3, weakMapSize.get())

        gcAndSleepAndRefreshWeakMapSize(movieView)
        Assert.assertEquals(1, weakMapSize.get())

        listOf(1L, 2L) mapMulti MovieView::class
        Assert.assertEquals(3, weakMapSize.get())

        gcAndSleepAndRefreshWeakMapSize(movieView)
        Assert.assertEquals(1, weakMapSize.get())
    }

    /**
     * 已对字段求值过的view，是否存在内存泄露
     */
    @Test
    fun testLazyBuiltViewMemoryLeak() {
        /*var movieView = 1L mapSingle MovieView::class
        Assert.assertEquals(1, weakMapSize.get())
        printMovieView(movieView)
        var nowWeakMapSize = weakMapSize.get()
        Assert.assert*/
    }
}