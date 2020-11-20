package com.agile4j.model.builder

import com.agile4j.model.builder.mock.MovieView
import com.agile4j.utils.util.CollectionUtil
import org.junit.Test

/**
 * 耗时
 * @author liurenpeng
 * Created on 2020-09-22
 */

class TestPruner: BaseTest() {
    @Test
    fun testPruner() {
        val viewPruner: (MovieView) -> Boolean = { v -> CollectionUtil.isNotEmpty(v.visitorViews)}
        val movieViews = movieIds1A2 mapMulti MovieView::class pruneBy viewPruner
    }
}