package com.agile4j.model.builder.mock

import com.agile4j.model.builder.delegate.ExJoinDelegate.Companion.exJoin
import com.agile4j.model.builder.delegate.InJoinDelegate.Companion.inJoin
import com.fasterxml.jackson.annotation.JsonIgnore

/**
 * @author liurenpeng
 * Created on 2020-07-09
 */

data class MovieView (val movie: Movie) {

    val id: Long = movie.id

    // A->IJM
    val idInJoin: Long? by inJoin(Movie::id)

    // A->C[IJM]
    val subscriberIdsInJoin: Collection<Long>? by inJoin(Movie::subscriberIds)

    // A->IJA->IJT
    val checkerView: UserView? by inJoin(Movie::checker)

    // A->C[IJA]->C[IJT]
    val visitorViews: Collection<UserView>? by inJoin(Movie::visitors)

    // A->IJI->IJA
    val author: User? by inJoin(Movie::authorId)

    // A->C[IJI]->C[IJA]
    val subscribers: Collection<User>? by inJoin(Movie::subscriberIds)

    // A->IJI->IJA->IJT
    @get:JsonIgnore
    val authorView: UserView? by inJoin(Movie::authorId)

    // A->C[IJI]->C[IJA]->C[IJT]
    val subscriberViews: Collection<UserView>? by inJoin(Movie::subscriberIds)

    // C[I]->M[I,EJM]
    val shared: Boolean? by exJoin(::isShared)
    val count: Count? by exJoin(::getCountsByMovieIds)
    val interaction: MovieInteraction? by exJoin(::getInteractionsByMovieIds)

    // C[I]->M[I,C[EJM]]
    val videos: Collection<Video>? by exJoin(::getVideosByMovieIds)

    // C[I]->M[I,EJA]->M[I,EJT]
    val trailerView: VideoDTO? by exJoin(::getTrailersByMovieIds)

    // C[I]->M[I,C[EJA]]->M[I,C[EJT]]
    val videoDTOs: Collection<VideoDTO>? by exJoin(::getVideosByMovieIds)

    // C[I]->M[I,EJI]->M[I,EJA]
    val trailer: Video? by exJoin(::getTrailerIdsByMovieIds)

    // C[I]->M[I,C[EJI]]->M[I,C[EJA]]
    val byIVideos: Collection<Video>? by exJoin(::getVideoIdsByMovieIds)

    // C[I]->M[I,EJI]->M[I,EJA]->M[I,EJT]
    val byITrailerView: VideoDTO? by exJoin(::getTrailerIdsByMovieIds)

    // C[I]->M[I,C[EJI]]->M[I,C[EJA]]->M[I,C[EJT]]
    val byIVideoDTOs: Collection<Video>? by exJoin(::getVideoIdsByMovieIds)

    private val pri: Int = 0
    val pub: Int = 0
}

data class VideoDTO (val video: Video) {
    val source: Source? by exJoin(::getSourcesByVideoIds)
}

data class Movie(
    val id: Long,
    val authorId: Long,
    val subscriberIds: Collection<Long>) {
    val checker: User = User(id, id, id)
    val visitors: Collection<User> = setOf(User(id, id, id),
        User(id + 1, id + 1, id + 1))
}

data class UserView (val user: User) {
    val movie: Movie? by inJoin(User::movie1Id)
    val movieView: MovieView? by inJoin(User::movie2Id)
}

data class User(val id: Long, val movie1Id: Long, val movie2Id: Long)

data class Video(val id: Long)

data class Source(val id: Long)

data class Count(val counts: Map<CountType, Int>) {
    fun getByType(type: CountType) : Int = counts[type] ?: 0
}

/**
 * count类型枚举
 * 指"点"的状态，例如当前movie自身的状态
 */
enum class CountType(val value: Int) {
    UNKNOWN(0),
    COMMENT(1), // 评论数
    PLAY(2), // 播放数
}

data class MovieInteraction(var movieInteractions: Map<MovieInteractionType, Int>) {
    fun getByType(type: MovieInteractionType) : Int = movieInteractions[type] ?: 0
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
