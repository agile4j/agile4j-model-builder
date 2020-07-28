package com.agile4j.model.builder.mock

import com.agile4j.model.builder.relation.accompanyBy
import com.agile4j.model.builder.relation.buildBy
import com.agile4j.model.builder.relation.by
import com.agile4j.model.builder.relation.indexBy
import com.agile4j.model.builder.relation.multiInJoin
import com.agile4j.model.builder.relation.singleInJoin

/**
 * @author liurenpeng
 * Created on 2020-07-09
 */

fun initModelRelation() {
    Movie::class indexBy Movie::id
    Movie::class buildBy ::getMovieByIds

    User::class indexBy User::id
    User::class buildBy ::getUserByIds

    Video::class indexBy Video::id
    Video::class buildBy ::getVideoByIds

    Source::class indexBy Source::id
    Source::class buildBy ::getSourceByIds

    MovieView::class accompanyBy Movie::class
    VideoDTO::class accompanyBy Video::class
    UserView::class accompanyBy User::class
}

/**
 * 自己会注册，不写也可以，只有第一次不会聚合。写上相当于warm up
 */
fun warmUpModelRelation() {
    Movie::class singleInJoin User::class by Movie::authorId
    Movie::class singleInJoin User::class by Movie::checkerId
    Movie::class multiInJoin User::class by Movie::subscriberIds

    User::class singleInJoin Movie::class by User::movie1Id
    User::class singleInJoin Movie::class by User::movie2Id
}