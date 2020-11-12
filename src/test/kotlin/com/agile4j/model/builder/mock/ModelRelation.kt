package com.agile4j.model.builder.mock

import com.agile4j.kts.loader.eval
import com.agile4j.model.builder.relation.accompanyBy
import com.agile4j.model.builder.relation.buildBy
import com.agile4j.model.builder.relation.by
import com.agile4j.model.builder.relation.indexBy
import com.agile4j.model.builder.relation.invoke
import com.agile4j.model.builder.relation.multiInJoin
import com.agile4j.model.builder.relation.singleInJoin
import com.agile4j.model.builder.relation.targets

/**
 * @author liurenpeng
 * Created on 2020-07-09
 */

fun initModelRelation() {
    style1()
    // style2()
    // style3()
}

fun style1() {
    Movie::class {
        indexBy(Movie::id)
        buildBy(::getMovieByIds)
        targets(MovieDTO::class, MovieView::class)
    }
    User::class {
        indexBy(User::id)
        buildBy(::getUserByIds)
        targets(UserView::class)
    }
    Video::class {
        indexBy(Video::id)
        buildBy(::getVideoByIds)
        targets(VideoDTO::class)
    }
    Source::class {
        indexBy(Source::id)
        buildBy(::getSourceByIds)
    }
}

fun style2() {
    // model-builder-relation.kts文件内容与style1()代码内容相同
    // 不过是以脚本的方式放在resources目录下
    eval("src/test/resources/model-builder-relation.kts")
}


fun style3() {
    Movie::class indexBy Movie::id
    Movie::class buildBy ::getMovieByIds

    User::class indexBy User::id
    User::class buildBy ::getUserByIds

    Video::class indexBy Video::id
    Video::class buildBy ::getVideoByIds

    Source::class indexBy Source::id
    Source::class buildBy ::getSourceByIds

    MovieView::class accompanyBy Movie::class
    MovieDTO::class accompanyBy Movie::class
    VideoDTO::class accompanyBy Video::class
    UserView::class accompanyBy User::class
}

/**
 * 自己会注册，不写也可以，只有第一次不会聚合。写上相当于warm up
 */
fun warmUpModelRelation() {
    Movie::class singleInJoin User::class by Movie::authorId
    Movie::class multiInJoin User::class by Movie::subscriberIds

    User::class singleInJoin Movie::class by User::movie1Id
    User::class singleInJoin Movie::class by User::movie2Id
}
