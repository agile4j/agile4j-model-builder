package com.agile4j.model.builder

import com.agile4j.model.builder.mock.MovieView
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.junit.Assert
import org.junit.Test


/**
 * @author liurenpeng
 * Created on 2020-08-03
 */

class TestJSON: BaseTest() {

    private val jsonStr = "{\"movie\":{\"id\":1,\"authorId\":1,\"subscriberIds\":[3,4]," +
            "\"checker\":{\"id\":1,\"movie1Id\":1,\"movie2Id\":1},\"visitors\":" +
            "[{\"id\":1,\"movie1Id\":1,\"movie2Id\":1},{\"id\":2,\"movie1Id\":2," +
            "\"movie2Id\":2}]},\"id\":1,\"pub\":0,\"idInJoin\":1,\"subscriberIdsInJoin" +
            "\":[3,4],\"author\":{\"id\":1,\"movie1Id\":1,\"movie2Id\":1}," +
            "\"subscribers\":[{\"id\":3,\"movie1Id\":3,\"movie2Id\":3},{\"id\":4," +
            "\"movie1Id\":4,\"movie2Id\":4}],\"shared\":true,\"interaction\":" +
            "{\"movieInteractions\":{\"UNKNOWN\":0,\"LIKE\":1,\"REWARD\":2}}," +
            "\"videos\":[{\"id\":1},{\"id\":2},{\"id\":3}],\"trailerView\":" +
            "{\"video\":{\"id\":1},\"source\":{\"id\":1}},\"videoDTOs\":[{" +
            "\"video\":{\"id\":1},\"source\":{\"id\":1}},{\"video\":{\"id\":2}," +
            "\"source\":{\"id\":2}},{\"video\":{\"id\":3},\"source\":{\"id\":3}}]," +
            "\"trailer\":{\"id\":1},\"byIVideos\":[{\"id\":1},{\"id\":2},{\"id\":3}]," +
            "\"byITrailerView\":{\"video\":{\"id\":1},\"source\":{\"id\":1}}," +
            "\"byIVideoDTOs\":[{\"id\":1},{\"id\":2},{\"id\":3}],\"count\":{\"" +
            "counts\":{\"UNKNOWN\":0,\"COMMENT\":1,\"PLAY\":2}}}"

    @Test
    fun test() {
        val movieView = movieId1 mapSingle MovieView::class
        val mapper = ObjectMapper().registerKotlinModule()
        val currJsonStr = mapper.writeValueAsString(movieView)
        Assert.assertEquals(jsonStr.length, currJsonStr.length)
    }
}