package com.agile4j.model.builder.mock


/**
 * @author liurenpeng
 * Created on 2020-07-09
 */

fun getMovieById(id: Long): Movie {
    return allMovies.filter { it.key == id }[id] ?: error("movie not exist")
}

fun getMovieByIds(ids: Collection<Long>): Map<Long, Movie> {
    println("===getMovieByIds ids:$ids")
    return allMovies.filter { ids.contains(it.key) }
}

fun getVideosByMovieIds(ids: Collection<Long>): Map<Long, Collection<Video>> {
    println("===getVideosByMovieIds ids:$ids")
    return movieIdToVideoIdsMap.filter { ids.contains(it.key) }
        .mapValues { e -> allVideos.filter { e.value.contains(it.key) }.values }
}

fun getTrailersByMovieIds(ids: Collection<Long>): Map<Long, Video> {
    println("===getTrailersByMovieIds ids:$ids")
    return ids.map { it to allVideos[it]!! }.toMap()
}

fun getVideoIdsByMovieIds(ids: Collection<Long>): Map<Long, Collection<Long>> {
    println("===getVideoIdsByMovieIds ids:$ids")
    return movieIdToVideoIdsMap.filter { ids.contains(it.key) }
}

fun getTrailerIdsByMovieIds(ids: Collection<Long>): Map<Long, Long> {
    println("===getTrailerIdsByMovieIds ids:$ids")
    return ids.map { it to it }.toMap()
}

fun getCountsByMovieIds(ids: Collection<Long>): Map<Long, Count> {
    println("===getCountsByMovieIds ids:$ids")
    return movieIdToCountMap.filter { ids.contains(it.key) }
}

fun getInteractionsByMovieIds(ids: Collection<Long>): Map<Long, MovieInteraction> {
    println("===getInteractionsByMovieIds ids:$ids")
    return movieIdToInteractionMap.filter { ids.contains(it.key) }
}

fun getSourcesByVideoIds(ids: Collection<Long>): Map<Long, Source> {
    println("===getSourcesByVideoIds ids:$ids")
    val idMap = videoIdToSourceIdMap.filter { ids.contains(it.key) }
    val sourceMap = allSources.filter { idMap.values.contains(it.key) }
    return idMap.mapValues { sourceMap[it.value] }
        .filter { it.value != null }.mapValues { it.value!! }
}

fun getVideoByIds(ids: Collection<Long>): Map<Long, Video> {
    println("===getVideoByIds ids:$ids")
    return allVideos.filter { ids.contains(it.key) }
}

fun getSourceByIds(ids: Collection<Long>): Map<Long, Source> {
    println("===getSourceByIds ids:$ids")
    return allSources.filter { ids.contains(it.key) }
}

fun getUserByIds(ids: Collection<Long>): Map<Long, User> {
    println("===getUserByIds ids:$ids")
    return allUsers.filter { ids.contains(it.key) }
}

/**
 * 这里mock的逻辑是：id相同时为true
 */
fun isShared(ids: Collection<Long>): Map<Long, Boolean> {
    println("===isShared ids:$ids")
    val userId = MockScopes.visitor()
    return ids.toSet().map { it to (userId == it) }.toMap()
}