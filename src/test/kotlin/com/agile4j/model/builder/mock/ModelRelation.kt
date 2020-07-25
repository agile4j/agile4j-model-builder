package com.agile4j.model.builder.mock

import com.agile4j.model.builder.relation.accompanyBy
import com.agile4j.model.builder.relation.buildBy
import com.agile4j.model.builder.relation.by
import com.agile4j.model.builder.relation.exJoin
import com.agile4j.model.builder.relation.inJoin
import com.agile4j.model.builder.relation.indexBy

/**
 * @author liurenpeng
 * Created on 2020-07-09
 */

const val SHARED = "shared"
const val VIEWED = "viewed"
const val COUNT = "count"
const val INTERACTION = "interaction"
const val VIDEOS = "videos"
const val SOURCE = "source"

fun initModelRelation() {
    Movie::class indexBy Movie::id
    Movie::class buildBy ::getMovieByIds
    Movie::class inJoin User::class by Movie::authorId
    Movie::class inJoin User::class by Movie::checkerId
    Movie::class exJoin SHARED by ::isShared
    Movie::class exJoin VIEWED by ::isViewed
    Movie::class exJoin COUNT by ::getCountsByMovieIds
    Movie::class exJoin INTERACTION by ::getInteractionsByMovieIds
    Movie::class exJoin VIDEOS by ::getVideosByMovieIds
    // Movie::class outJoin VIDEOS by ::getVideoIdsByMovieIds 能自动支持吗

    User::class indexBy User::id
    User::class buildBy ::getUserByIds
    User::class inJoin Movie::class by User::movie1Id
    User::class inJoin Movie::class by User::movie2Id

    Video::class indexBy Video::id
    Video::class buildBy ::getVideoByIds
    Video::class exJoin SOURCE by ::getSourcesByVideoIds

    Source::class indexBy Source::id
    Source::class buildBy ::getSourceByIds

    MovieView::class accompanyBy Movie::class
    VideoDTO::class accompanyBy Video::class
    UserView::class accompanyBy User::class
}