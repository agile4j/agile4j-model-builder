package com.agile4j.model.builder.mock

import com.agile4j.model.builder.mock.MockScopes.isThrowException
import java.util.concurrent.atomic.AtomicInteger


/**
 * @author liurenpeng
 * Created on 2020-07-09
 */

fun getMovieByIds(ids: Collection<Long>): Map<Long, Movie> {
    accessTimes.incrementAndGet()
    println("===getMovieByIds ids:$ids")
    return allMovies.filter { ids.contains(it.key) }
}

fun getVideosByMovieIdsForTestEmpty(ids: Collection<Long>): Map<Long, Collection<Video>> {
    accessTimes.incrementAndGet()
    println("===getVideosByMovieIdsForTestEmpty ids:$ids")
    return emptyMap()
}

fun getVideosByMovieIds(ids: Collection<Long>): Map<Long, Collection<Video>> {
    accessTimes.incrementAndGet()
    println("===getVideosByMovieIds ids:$ids")
    return movieIdToVideoIdsMap.filter { ids.contains(it.key) }
        .mapValues { e -> allVideos.filter { e.value.contains(it.key) }.values }
}

fun getTrailersByMovieIds(ids: Collection<Long>): Map<Long, Video> {
    accessTimes.incrementAndGet()
    println("===getTrailersByMovieIds ids:$ids")
    if (isThrowException()) {
        throw IllegalStateException()
    }
    return ids.map { it to allVideos[it]!! }.toMap()
}

fun getVideoIdsByMovieIds(ids: Collection<Long>): Map<Long, Collection<Long>> {
    accessTimes.incrementAndGet()
    println("===getVideoIdsByMovieIds ids:$ids")
    return movieIdToVideoIdsMap.filter { ids.contains(it.key) }
}

fun getVideoIdsByMovieIdsForTestEmpty(ids: Collection<Long>): Map<Long, Collection<Long>> {
    accessTimes.incrementAndGet()
    println("===getVideoIdsByMovieIdsForTestEmpty ids:$ids")
    return emptyMap()
}

fun getTrailerIdsByMovieIds(ids: Collection<Long>): Map<Long, Long> {
    accessTimes.incrementAndGet()
    println("===getTrailerIdsByMovieIds ids:$ids")
    return ids.map { it to it }.toMap()
}

fun getCountsByMovieIds(ids: Collection<Long>): Map<Long, Count> {
    accessTimes.incrementAndGet()
    println("===getCountsByMovieIds ids:$ids")
    return movieIdToCountMap.filter { ids.contains(it.key) }
}

fun getInteractionsByMovieIds(ids: Collection<Long>): Map<Long, MovieInteraction> {
    accessTimes.incrementAndGet()
    println("===getInteractionsByMovieIds ids:$ids")
    return movieIdToInteractionMap.filter { ids.contains(it.key) }
}

fun getSourcesByVideoIds(ids: Collection<Long>): Map<Long, Source> {
    accessTimes.incrementAndGet()
    println("===getSourcesByVideoIds ids:$ids")
    val idMap = videoIdToSourceIdMap.filter { ids.contains(it.key) }
    val sourceMap = allSources.filter { idMap.values.contains(it.key) }
    return idMap.mapValues { sourceMap[it.value] }
        .filter { it.value != null }.mapValues { it.value!! }
}

fun getVideoByIds(ids: Collection<Long>): Map<Long, Video> {
    accessTimes.incrementAndGet()
    println("===getVideoByIds ids:$ids")
    return allVideos.filter { ids.contains(it.key) }
}

fun getSourceByIds(ids: Collection<Long>): Map<Long, Source> {
    accessTimes.incrementAndGet()
    println("===getSourceByIds ids:$ids")
    return allSources.filter { ids.contains(it.key) }
}

fun getUserByIds(ids: Collection<Long>): Map<Long, User> {
    accessTimes.incrementAndGet()
    println("===getUserByIds ids:$ids")
    return allUsers.filter { ids.contains(it.key) }
}

/**
 * 这里mock的逻辑是：id相同时为true
 */
fun isShared(ids: Collection<Long>): Map<Long, Boolean> {
    accessTimes.incrementAndGet()
    println("===isShared ids:$ids")
    val userId = MockScopes.visitor()
    return ids.toSet().map { it to (userId == it) }.toMap()
}

val accessTimes = AtomicInteger(0)
