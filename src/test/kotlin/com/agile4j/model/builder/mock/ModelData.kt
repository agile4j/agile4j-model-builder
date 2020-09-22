package com.agile4j.model.builder.mock

/**
 * @author liurenpeng
 * Created on 2020-07-09
 */

const val idBorder = 1000L

val allUsers = (1..idBorder).toList().map { it to User(it, it, it) }.toMap()

val allSources = (1..idBorder).toList().map { it to Source(it) }.toMap()

val allVideos = (1..idBorder).toList().map { it to Video(it) }.toMap()

val videoIdToSourceIdMap = (1..idBorder).toList().map { it to it }.toMap()

val allMovies = (1..idBorder).toList().map { it to
        Movie(it, 2 * it - 1,
            listOf(2 * it + 1, 2 * it + 2)) }.toMap()

val movieIdToVideoIdsMap = (1..idBorder).toList().map { it to
        (3 * it - 2 .. 3 * it).toList() }.toMap()

val movieIdToCountMap = (1..idBorder).toList().map { it to
        (Count(
            CountType.values()
                .toList().map { type -> type to
                        (it * type.value).toInt() }.toMap()))}.toMap()

val movieIdToInteractionMap = (1..idBorder).toList().map { id -> id to
        (MovieInteraction(
            MovieInteractionType.values()
                .toList().map { type -> type to
                        (id * type.value).toInt() }.toMap()))}.toMap()