# 特性

## 自动映射
* 自动映射机制由三个维度组成：
    1. inJoin/exJoin，共2种情况
    2. 1对1/1对多，共2种情况
    3. 映射类型：I→A、I→T、A→T、M→M(即同类型)，共4种情况
* agile4j-model-builder会对这2\*2\*4=16种情况自动识别并映射，这16种情况即所有情况，不应出现其他情况，如果出现其他情况说明代码存在逻辑问题，build时会抛出异常。
* 综上，自动映射机制所能解决的所有问题域为：

<details>
<summary>缩写说明</summary>

```
A: accompany
I: index
T: target
M: model(泛指所有model，A、I、T...)
IJ: inJoin
EJ: exJoin
C[E]: Collection<E>
M[K,V]: Map<K,V>
->: 表示转化过程
```
</details>

![ModelBuilder.svg](https://raw.githubusercontent.com/agile4j/agile4j-model-builder/master/src/test/resources/ModelBuilder.svg)

* 完整问题域代码演示，代码位置：agile4j-model-builder/src/test/kotlin/com/agile4j/model/builder/mock

<details>
<summary>Model.kt</summary>

```Kotlin
data class MovieView (val movie: Movie) {

    // A->IJM
    val idinJoin: Long? by inJoin(Movie::id)

    // A->C[IJM]
    val subscriberIdsinJoin: Collection<Long>? by inJoin(Movie::subscriberIds)

    // A->IJA->IJT
    @get:JsonIgnore
    val checkerView: UserView? by inJoin(Movie::checker)

    // A->C[IJA]->C[IJT]
    @get:JsonIgnore
    val visitorViews: Collection<UserView>? by inJoin(Movie::visitors)

    // A->IJI->IJA
    val author: User? by inJoin(Movie::authorId)

    // A->C[IJI]->C[IJA]
    val subscribers: Collection<User>? by inJoin(Movie::subscriberIds)

    // A->IJI->IJA->IJT
    @get:JsonIgnore
    val authorView: UserView? by inJoin(Movie::authorId)
    val movieDTO: MovieDTO? by inJoin(Movie::id)

    // A->C[IJI]->C[IJA]->C[IJT]
    @get:JsonIgnore
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
    val byIVideoDTOs: Collection<VideoDTO>? by exJoin(::getVideoIdsByMovieIds)
}

data class MovieDTO (val movie: Movie) {
    val author: User? by inJoin(Movie::authorId)
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

data class Count(val counts: map<CountType, Int>) {
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

data class MovieInteraction(var movieInteractions: map<MovieInteractionType, Int>) {
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
```
</details>

<details>
<summary>ModelRelation.kt</summary>

```Kotlin
fun initModelRelation() {
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
```
</details>

<details>
<summary>ModelFunction.kt</summary>

```Kotlin
fun getMovieByIds(ids: Collection<Long>): map<Long, Movie>
fun getVideosByMovieIds(ids: Collection<Long>): map<Long, Collection<Video>>
fun getTrailersByMovieIds(ids: Collection<Long>): map<Long, Video>
fun getVideoIdsByMovieIds(ids: Collection<Long>): map<Long, Collection<Long>>
fun getTrailerIdsByMovieIds(ids: Collection<Long>): map<Long, Long>
fun getCountsByMovieIds(ids: Collection<Long>): map<Long, Count>
fun getInteractionsByMovieIds(ids: Collection<Long>): map<Long, MovieInteraction>
fun getSourcesByVideoIds(ids: Collection<Long>): map<Long, Source>
fun getVideoByIds(ids: Collection<Long>): map<Long, Video>
fun getSourceByIds(ids: Collection<Long>): map<Long, Source>
fun getUserByIds(ids: Collection<Long>): map<Long, User>
fun isShared(ids: Collection<Long>): map<Long, Boolean>
```
</details>

## 增量lazy式构建
* 举一个场景：某个业务需要构建的ArticleView对象，只会用到user，不会用到commentViews。但希望复用构建逻辑和ArticleView的定义，且不希望浪费性能去构建commentViews。
* 为了满足这个需求，agile4j-model-builder采用的是增量lazy式构建。即通过mapMulti/mapSingle构建结束时，仅会真实取到A的值，而所有的关联model的值都不会取，所以构建速度极快。
    1. 如果通过I构建，仅需一次function调用，(批量)获得A的值后，构建过程就已结束。
    2. 如果通过A构建，则一次function调用都没有，直接结束。
* 如果构建完后调用T的取值方法，仅会对取值方法涉及到的关联model取值，无关model不会构建，所用即所取。

## 聚合批量构建
* 一个model和另一个model之间的关联关系，可能存在于多个字段中。例如：
```Kotlin
// A
data class Article(
    val id: Long,
    val userId: Long, // 作者id
    val checkerIds: Collection<Long> // 审批者id
)

// T
data class ArticleView (val article: Article) {
    val user: User? by inJoin(Article::userId)
    val checkerViews: Collection<UserView>? by inJoin(Article::checkerIds)
}

// User、UserView定义略
```
* 上述代码中，Article的字段userId、checkerIds的值都是User的索引。 在ArticleView中分别映射成了ijA(User)和ijT(UserView)。
* 假设通过`val articleViews = articles mapMulti ArticleView::class`来构建ArticleView，并将构建结果articleViews进行JSON化(以使所有字段都进行取值)。整个过程中，agile4j-model-builder只会调用一次getUserByIds方法。因为agile4j-model-builder会先将所有Article中的userId和checkerIds的值`聚合`成一个Collection，然后一次性`批量`查询，以减少对第三方function的调用频率(网络IO等往往会使调用过程耗时很长)，以提高性能。

## 不会重复构建
* 所有的构建结果agile4j-model-builder都会缓存，对同一字段的多次访问，不会重复构建。


## 代码零侵入
* agile4j-model-builder的使用过程中，接入方需要了解的全部API只有：indexBy、buildBy、accompanyBy、inJoin、exJoin、mapMulti、mapSingle。除此之外没有任何概念和类需要了解，且对accompany的代码没有任何侵入，可读性和语义化强。
