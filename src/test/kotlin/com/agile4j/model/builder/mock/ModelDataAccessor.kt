package com.agile4j.model.builder.mock

import com.agile4j.model.builder.CurrentScope

/**
 * @author liurenpeng
 * Created on 2020-07-09
 */

fun getMovieByIds(ids: Collection<Long>): Map<Long, Movie> {
    println("===getMovieByIds")
    return allMovies.filter { ids.contains(it.key) }
}

fun getVideosByMovieIds(ids: Collection<Long>): Map<Long, Collection<Video>> {
    println("===getVideosByMovieIds")
    return movieIdToVideoIdsMap.filter { ids.contains(it.key) }
        .mapValues { allVideos.filter { ids.contains(it.key) }.values }
}

fun getCountsByMovieIds(ids: Collection<Long>): Map<Long, MovieCount> {
    println("===getCountsByMovieIds")
    return movieIdToCountMap.filter { ids.contains(it.key) }
}

fun getInteractionsByMovieIds(ids: Collection<Long>): Map<Long, MovieInteraction> {
    println("===getInteractionsByMovieIds")
    return movieIdToInteractionMap.filter { ids.contains(it.key) }
}

fun getSourcesByVideoIds(ids: Collection<Long>): Map<Long, Source> {
    println("===getSourcesByVideoIds")
    val idMap = videoIdToSourceIdMap.filter { ids.contains(it.key) }
    val sourceMap = allSources.filter { idMap.values.contains(it.key) }
    return idMap.mapValues { sourceMap[it.value] }
        .filter { it.value != null }.mapValues { it.value!! }
}

fun getVideoByIds(ids: Collection<Long>): Map<Long, Video> {
    println("===getVideoByIds")
    return allVideos.filter { ids.contains(it.key) }
}

fun getSourceByIds(ids: Collection<Long>): Map<Long, Source> {
    println("===getSourceByIds")
    return allSources.filter { ids.contains(it.key) }
}

fun getUserByIds(ids: Collection<Long>): Map<Long, User> {
    println("===getUserByIds")
    return allUsers.filter { ids.contains(it.key) }
}

/**
 * 这里mock的逻辑是：id相同时为true
 */
fun isShared(ids: Collection<Long>): Map<Long, Boolean> {
    println("===isShared")
    val userId = CurrentScope.visitor()
    return ids.toSet().map { it to (userId == it) }.toMap()
}

/**
 * 这里mock的逻辑是：id相同时为true
 */
fun isViewed(ids: Collection<Long>): Map<Long, Boolean> {
    println("===isViewed")
    val userId = CurrentScope.visitor()
    return ids.toSet().map { it to (userId == it) }.toMap()
}