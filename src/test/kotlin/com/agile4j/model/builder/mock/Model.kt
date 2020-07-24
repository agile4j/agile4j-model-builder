package com.agile4j.model.builder.mock

import com.agile4j.model.builder.delegate.support.Join
import com.agile4j.model.builder.delegate.support.OutJoin
import com.fasterxml.jackson.annotation.JsonIgnore

/**
 * @author liurenpeng
 * Created on 2020-07-09
 */

data class MovieView (val movie: Movie) {

    val videoDTOs: Collection<VideoDTO>? by OutJoin(VIDEOS)

    val videos: Collection<Video>? by OutJoin(VIDEOS)

    val count: MovieCount? by OutJoin(COUNT)

    val interaction: MovieInteraction? by OutJoin(INTERACTION)

    val author: User? by Join(Movie::authorId)

    @get:JsonIgnore
    val authorView: UserView? by Join(Movie::authorId)

    val checker: User? by Join(Movie::checkerId)

    val shared: Boolean? by OutJoin(SHARED)

    val viewed: Boolean? by OutJoin(VIEWED)

}

data class VideoDTO (val video: Video) {
    val source: Source? by OutJoin(SOURCE)
}

data class Movie(val id: Long, val authorId: Long, val checkerId: Long)

data class UserView (val user: User) {
    val movie: Movie? by Join(User::movie1Id)
    val movieView: MovieView? by Join(User::movie2Id)
}

data class User(val id: Long, val movie1Id: Long, val movie2Id: Long)

data class Video(val id: Long)

data class Source(val id: Long)

data class MovieCount(val movieCounts: Map<MovieCountType, Int>) {
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
