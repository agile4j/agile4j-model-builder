package com.agile4j.model.builder

import com.agile4j.model.builder.delegate.ExJoinDelegate.Companion.exJoin
import com.agile4j.model.builder.delegate.InJoinDelegate.Companion.inJoin
import com.agile4j.model.builder.relation.accompanyBy
import com.agile4j.model.builder.relation.buildBy
import com.agile4j.model.builder.relation.indexBy
import org.junit.Before
import org.junit.Test

/**
 * 测试参数和响应对不齐的情况，例如：
 * 1. 参数传入了集合1 2 3，但因2对应的实体不存在，响应的map的key只有1 3
 * 2. 参数传入了集合1 2 3，但因3对应的实体不存在，响应的map对应的value为null
 * @author liurenpeng
 * Created on 2020-08-03
 */

data class BookView (val book: Book) {
    val author: AuthorView? by inJoin(Book::authorId)
    val introduction: Introduction? by exJoin(::getIntroductionsByBookIds)
}

data class AuthorView (val author: Author)

data class Book(
    val id: Long,
    val authorId: Long
)

data class Author(val id: Long)

data class Introduction(val id: Long)

fun getIntroductionsByBookIds(ids: Collection<Long>): Map<Long, Long?> {
    println("===getIntroductionsByBookIds ids:$ids")
    val result = mutableMapOf<Long, Long?>()
    for (id in ids) {
        if (id % 2 == 0L) {
            continue
        }
        if (id % 3 == 0L) {
            result[id] = null
        } else {
            result[id] = id
        }
    }
    return result
}

val allBooks = (1..12L).toList().map { it to Book(it, it) }.toMap()
val allAuthors = (1..12L).toList().map { it to Author(it) }.toMap()
val allIntroductions = (1..12L).toList().map { it to Introduction(it) }.toMap()

fun getBookByIds(ids: Collection<Long>): Map<Long, Book> {
    println("===getBookByIds ids:$ids")
    return allBooks.filter { ids.contains(it.key) }
}

fun getAuthorByIds(ids: Collection<Long>): Map<Long, Author> {
    println("===getAuthorByIds ids:$ids")
    return allAuthors.filter { ids.contains(it.key) }
}

fun getIntroductionByIds(ids: Collection<Long>): Map<Long, Introduction> {
    println("===getIntroductionByIds ids:$ids")
    return allIntroductions.filter { ids.contains(it.key) }
}

class TestEmpty: BaseTest() {

    @Before
    fun testEmptyBefore() {
        Book::class indexBy Book::id
        Book::class buildBy ::getBookByIds

        Author::class indexBy Author::id
        Author::class buildBy ::getAuthorByIds

        Introduction::class indexBy Introduction::id
        Introduction::class buildBy ::getIntroductionByIds

        BookView::class accompanyBy Book::class
        AuthorView::class accompanyBy Author::class
    }

    @Test
    fun test() {
        val list = mutableListOf(1L, 2L, 3L)
        val bookViews = list mapMulti BookView::class
        println("---bookViews:$bookViews")
        bookViews.forEach { println("---author:" + it.author) }
        bookViews.forEach { println("---introduction:" + it.introduction) }
    }
}