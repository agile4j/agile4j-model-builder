package com.agile4j.model.builder.mock

import com.agile4j.model.builder.relation.accompanyBy
import com.agile4j.model.builder.relation.buildBy
import com.agile4j.model.builder.relation.by
import com.agile4j.model.builder.relation.indexBy
import com.agile4j.model.builder.relation.join
import com.agile4j.model.builder.relation.outJoin

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