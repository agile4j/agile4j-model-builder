package com.agile4j.model.builder.mock

/**
 * @author liurenpeng
 * Created on 2020-07-09
 */

val allUsers = (1..12L).toList().map { it to User(it, it, it) }.toMap()

val allSources = (1..12L).toList().map { it to Source(it) }.toMap()

val allVideos = (1..12L).toList().map { it to Video(it) }.toMap()

val videoIdToSourceIdMap = (1..12L).toList().map { it to it }.toMap()

val allMovies = (1..4L).toList().map { it to
        Movie(it, 2 * it - 1, 2 * it) }.toMap()

val movieIdToVideoIdsMap = (1..4L).toList().map { it to
        (3 * it - 2 .. 3 * it).toList() }.toMap()

val movieIdToCountMap = (1..4L).toList().map { it to
        (MovieCount(
            MovieCountType.values()
                .toList().map { type -> type to (it * type.value).toInt() }.toMap()))}.toMap()

val movieIdToInteractionMap = (1..4L).toList().map { id -> id to
        (MovieInteraction(
            MovieInteractionType.values()
                .toList().map { type -> type to if(id != 1L) 0
                else (id * type.value).toInt() }.toMap()))}.toMap()