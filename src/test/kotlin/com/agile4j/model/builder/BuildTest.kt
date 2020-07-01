package com.agile4j.model.builder

import com.agile4j.model.builder.CurrentScope.visitor
import com.agile4j.model.builder.delegate.support.Join
import com.agile4j.model.builder.delegate.support.OutJoin
import com.agile4j.model.builder.relation.accompanyBy
import com.agile4j.model.builder.relation.buildBy
import com.agile4j.model.builder.relation.by
import com.agile4j.model.builder.relation.indexBy
import com.agile4j.model.builder.relation.join
import com.agile4j.model.builder.relation.outJoin
import com.agile4j.utils.scope.Scope.ScopeUtils.beginScope
import com.agile4j.utils.scope.ScopeKey
import java.lang.System.gc

/**
 * 特性
 * 1. 支持批量构建
 * 2. 全部lazy式构建
 * 3. 语义性强的api
 * 4. 业务代码侵入性低
 * 5. 待构建类可嵌套
 * 6. * 支持异步化 回调
 * @author liurenpeng
 * Created on 2020-05-26
 */

fun main() {
    initScope()
    initModelBuilder()
    handle(1L, listOf(1L, 2L))
    gc()
    handle(3L, listOf(3L, 4L))
}

fun handle(movieId : Long, movieIds: Collection<Long>) {

    val movieView = ModelBuilder() buildSingle MovieView::class by movieId
    val movieViews = ModelBuilder() buildMulti MovieView::class by movieIds

    println()
    println("---movieView:$movieView")
    println()

    println("---author:${movieView!!.author}")
    println("---author:${movieView.author}")
    println("---checker:${movieView.checker}")
    println()

    println("---movieViews:$movieViews.")
    println()

    println("---0.shared:${movieViews.elementAt(0).shared}")
    println("---0.viewed:${movieViews.elementAt(0).viewed}")
    println("---0.count:${movieViews.elementAt(0).count}")
    println("---0.interaction:${movieViews.elementAt(0).interaction}")
    println("---0.videos:${movieViews.elementAt(0).videos}")
    println("---0.author:${movieViews.elementAt(0).author}")
    println("---0.checker:${movieViews.elementAt(0).checker}")
    println("---0.checkerView:${movieViews.elementAt(0).checkerView}")
    println("---0.videoDTOs:${movieViews.elementAt(0).videoDTOs}")
    println()

    println("---1.shared:${movieViews.elementAt(1).shared}")
    println("---1.viewed:${movieViews.elementAt(1).viewed}")
    println("---1.count:${movieViews.elementAt(1).count}")
    println("---1.interaction:${movieViews.elementAt(1).interaction}")
    println("---1.videos:${movieViews.elementAt(1).videos}")
    println("---1.author:${movieViews.elementAt(1).author}")
    println("---1.checker:${movieViews.elementAt(1).checker}")
    println("---1.checkerView:${movieViews.elementAt(1).checkerView}")
    println("---1.videoDTOs:${movieViews.elementAt(1).videoDTOs}")
    println()

    movieViews.elementAt(0).videoDTOs.forEach{dto -> println(dto.source)}
    println()
    movieViews.elementAt(1).videoDTOs.forEach{dto -> println(dto.source)}
    println()

    println("**********")
    movieView.buildInModelBuilder
    println()
}

const val SHARED = "shared"
const val VIEWED = "viewed"
const val COUNT = "count"
const val INTERACTION = "interaction"
const val VIDEOS = "videos"
const val SOURCE = "source"

fun initModelBuilder() {
    Movie::class indexBy Movie::id
    Movie::class buildBy ::getMovieByIds
    Movie::class join User::class by Movie::authorId
    Movie::class join User::class by Movie::checkerId
    Movie::class outJoin SHARED by ::isShared
    Movie::class outJoin VIEWED by ::isViewed
    Movie::class outJoin COUNT by ::getCountsByMovieIds
    Movie::class outJoin INTERACTION by ::getInteractionsByMovieIds
    Movie::class outJoin VIDEOS by ::getVideosByMovieIds

    User::class indexBy User::id
    User::class buildBy ::getUserByIds

    Video::class indexBy Video::id
    Video::class buildBy ::getVideoByIds
    Video::class outJoin SOURCE by ::getSourcesByVideoIds

    Source::class indexBy Source::id
    Source::class buildBy ::getSourceByIds

    MovieView::class accompanyBy Movie::class
    VideoDTO::class accompanyBy Video::class
    UserView::class accompanyBy User::class
}

object CurrentScope{
    val visitor: ScopeKey<Long> = ScopeKey.withDefaultValue(0)
    fun visitor() = visitor.get()
}

fun initScope() {
    beginScope()
    visitor.set(1)
}

data class MovieView (val movie: Movie) {

    val videoDTOs: Collection<VideoDTO> by OutJoin(VIDEOS)

    val videos: Collection<Video> by OutJoin(VIDEOS)

    var count: MovieCount by OutJoin(COUNT)

    val interaction: MovieInteraction by OutJoin(INTERACTION)

    var author: User by Join("authorId")

    var checker: User? by Join("checkerId")

    var checkerView: UserView? by Join("checkerId")

    var shared: Boolean by OutJoin(SHARED)

    var viewed: Boolean by OutJoin(VIEWED)

    var groupId: String = ""
}

data class VideoDTO (val video: Video) {
    var source: Source by OutJoin(SOURCE)
}

data class Movie(val id: Long, val authorId: Long, val checkerId: Long)

data class UserView (val user: User)

data class User(val id: Long)

data class Video(val id: Long)

data class Source(val id: Long)

data class MovieCount(var movieCounts: Map<MovieCountType, Int>) {
    fun getByType(type: MovieCountType) : Int = movieCounts[type] ?: 0
}

/**
 * count类型枚举
 * 指"点"的状态，例如当前movie自身的状态
 */
enum class MovieCountType(val value: Int) {
    UNKNOWN(0),
    COMMENT(1), // 评论数
    PLAY(2), // 播放数
}

data class MovieInteraction(var movieCounts: Map<MovieInteractionType, Int>) {
    fun getByType(type: MovieInteractionType) : Int = movieCounts[type] ?: 0
}

/**
 * 交互类型枚举
 * 指"边"的状态，例如当前登录者和movie之间的状态
 * 可以有"权重"，例如打赏的数量，边不存在时，权重为0，边存在时权重默认为1，也可自定义权重值
 */
enum class MovieInteractionType(val value: Int) {
    UNKNOWN(0),
    LIKE(1), // 点赞
    REWARD(2), // 打赏
}

// mock
val allUsers = (1..12L).toList().map { it to User(it) }.toMap()

val allSources = (1..12L).toList().map { it to Source(it) }.toMap()

val allVideos = (1..12L).toList().map { it to Video(it) }.toMap()

val videoIdToSourceIdMap = (1..12L).toList().map { it to it }.toMap()

val allMovies = (1..4L).toList().map { it to
        Movie(it, 2 * it - 1, 2 * it) }.toMap()

val movieIdToVideoIdsMap = (1..4L).toList().map { it to
        (3 * it - 2 .. 3 * it).toList() }.toMap()

val movieIdToCountMap = (1..4L).toList().map { it to
        (MovieCount(MovieCountType.values()
    .toList().map { type -> type to (it * type.value).toInt() }.toMap()))}.toMap()

val movieIdToInteractionMap = (1..4L).toList().map { id -> id to
        (MovieInteraction(MovieInteractionType.values()
    .toList().map { type -> type to if(id != 1L) 0
            else (id * type.value).toInt() }.toMap()))}.toMap()

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
    val userId = visitor()
    return ids.toSet().map { it to (userId == it) }.toMap()
}

/**
 * 这里mock的逻辑是：id相同时为true
 */
fun isViewed(ids: Collection<Long>): Map<Long, Boolean> {
    println("===isViewed")
    val userId = visitor()
    return ids.toSet().map { it to (userId == it) }.toMap()
}
