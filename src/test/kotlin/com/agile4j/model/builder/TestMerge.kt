package com.agile4j.model.builder

import com.agile4j.model.builder.build.ModelBuilder
import com.agile4j.model.builder.mock.MovieView
import com.agile4j.model.builder.mock.accessTimes
import org.junit.Assert
import org.junit.Test

/**
 * 验证ModelBuilder的Merge[ModelBuilder.copyBy]是否正常
 * @author liurenpeng
 * Created on 2020-11-20
 */

class TestMerge: BaseTest() {
    @Test
    fun testMerge() {
        var target = movieId1 mapSingle MovieView::class
        Assert.assertNotNull(target)
        val mb = extractModelBuilder(target!!)
        Assert.assertNotNull(mb)
        Assert.assertEquals(1, accessTimes.get())

        target = buildSingleWithExistModelBuilder(mb!!, MovieView::class, movieId1)
        Assert.assertNotNull(target)
        Assert.assertEquals(1, accessTimes.get())

        val target3 = buildSingleWithExistModelBuilder(
            mb, MovieView::class, movieId3)
        Assert.assertNotNull(target3)
        val mb3 = extractModelBuilder(target3!!)
        Assert.assertNotNull(mb3)
        Assert.assertNotEquals(mb, mb3)
        Assert.assertEquals(2, accessTimes.get())
    }
}