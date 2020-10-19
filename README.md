# agile4j-model-builder

agile4j-model-builder是用Kotlin语言实现的model构建器，可在Kotlin/Java工程中使用。

# 目录
   * [如何引入](#如何引入)
   * [使用场景](#使用场景)
   * [代码演示](#代码演示)
   * [名词定义](#名词定义)
      * [accompany](#accompany)
      * [index](#index)
      * [target](#target)
      * [inJoin](#inJoin)
      * [exJoin](#exJoin)
      * [map](#map)
   * [特性](#特性)
      * [自动映射](#自动映射)
      * [增量lazy式构建](#增量lazy式构建)
      * [聚合批量构建](#聚合批量构建)
      * [不会重复构建](#不会重复构建)
      * [代码零侵入](#代码零侵入)
   * [API](#API)
      * [indexBy](#indexBy)
      * [buildBy](#buildBy)
      * [accompanyBy](#accompanyBy)
      * [inJoin](#inJoin-1)
      * [exJoin](#exJoin-1)
      * [mapMulti](#mapMulti)
      * [mapSingle](#mapSingle)
   * [如何接入](#如何接入)
      * [step1.定义Target](#step1定义Target)
      * [step2.声明Relation](#step2声明Relation)
      * [step3.构建Target](#step3构建Target)
   * [Java如何接入](#Java如何接入)
      * [step1.定义Target-DTO](#step1定义Target-DTO)
      * [step2.定义Target-VO](#step2定义Target-VO)
      * [step3.声明Relation](#step3声明Relation)
      * [step4.构建Target](#step4构建Target)

# 如何引入

>Gradle
```groovy
dependencies {
    compile "com.agile4j:agile4j-model-builder:1.0.18"
}
```
>Maven
```xml
<dependency>
    <groupId>com.agile4j</groupId>
    <artifactId>agile4j-model-builder</artifactId>
    <version>1.0.18</version>
</dependency>
```

# 使用场景

用于将互相之间有关联关系的"元model"（例如DBModel），组装成含有完整数据的"目标model"（例如VO、DTO）。

元model与目标model的区别可以理解为：
* 元model：可以从外部系统（例如DB）中根据索引字段（一般是主键），直接查询的model。
* 目标model：组装其他model以得到含有当前业务需要的完整数据的model，即构建的目标。

场景例如：

>已有的元model
```Kotlin
// 文章
data class Article(
    val id: Long,
    val userId: Long // 作者id
)

// 用户
data class User(
    val id: Long,
    val name: String, // 用户名
    val age: Int // 年龄
    // 其他属性...
)

// 评论
data class Comment(
    val id: Long,
    val content: String, // 评论内容
    val createTime: Long // 评论时间
    // 其他属性...
)
```

>已有的接口
```Kotlin
// 查询文章信息
fun getArticleByIds(ids: Collection<Long>): map<Long, Article> 

// 查询用户信息
fun getUserByIds(ids: Collection<Long>): map<Long, User> 

// 查询评论信息
fun getCommentByIds(ids: Collection<Long>): map<Long, Comment> 

// 查询文章下所有的评论
fun getCommentIdsByArticleIds(ids: Collection<Long>): map<Long, Collection<Long>>

// 查询当前登录者是否已对该评论点赞（当前登录者userId存在线程上下文中，因此未在参数中体现）
fun isLikedComment(ids: Collection<Long>): map<Long, Boolean>
```

>希望得到的目标model数据结构（只表明数据结构，并非最终代码）
```Kotlin
data class ArticleView(
    val article: Article,
    val user: User,
    val commentViews: Collection<CommentView>
)

data class CommentView(
    val comment: Comment,
    val isLiked: Boolean // 当前登录者是否已对该评论点赞
)
```

从上面的case我们可以了解到，通过元model得到目标model，可能存在以下情况：

* 数量对应关系：1对1，还是1对多
    * Article→User是1对1。
    * Article→Comment是1对多。
* 关联关系在哪里持有：在model内部持有，还是外部持有
    * Article→User是通过Article字段值userId持有，即model内部持有，不需要额外查询。
    * Article→Comment是通过getCommentIdsByArticleIds在model外部持有，例如DB中的关联表，或在第三方系统中持有，需要额外查询。
* model是否需要映射
    * Article→User是根据索引userId，得到元model，即User对象，需要映射。
    * Article→Comment是根据索引commentId，得到目标model，即CommentView对象，需要映射。
    * 还有其他可能的情况。例如：不需要映射；根据元model，得到目标model，需要映射。
* 是否所有字段都要构建
    * 并非所有字段都要构建。例如：某个业务场景需要构建的ArticleView对象，只会用到user，不会用到commentViews。但希望复用构建逻辑和ArticleView的定义，且不希望浪费性能去构建commentViews。
    * 所有字段都要构建。例如：http接口把view对象json化后响应给客户端，几乎所有的字段都需要构建。

如果每次构建ArticleView，都需要区分处理以上各种情况，那么代码的可复用性和可维护性很低。

可以使用agile4j-model-builder解决上述场景。

# 代码演示

为了对agile4j-model-builder的使用有一个直观的感受，针对上述示例中的业务场景，给出解决方案的代码。如果对代码中有不理解的地方，可以先跳过"代码演示"部分，继续浏览下文。

>目标model定义
```Kotlin
data class ArticleView (val article: Article) {
    // inJoin表示关联关系是在model内部持有，即internalJoin
    // Article::userId拿到的是索引(userId)，最终要得到的是元model(User对象)，工具会自动识别并进行映射
    val user: User? by inJoin(Article::userId)
    // exJoin表示关联关系是在model外部持有，即externalJoin
    // getCommentIdsByArticleIds拿到的是索引(commentId)，最终要得到的是目标model(CommentView对象)，且为1对多的数量关系，工具会自动识别并进行映射
    val commentViews: Collection<CommentView>? by exJoin(::getCommentIdsByArticleIds)
}

data class CommentView(val comment: Comment) {
    // exJoin表示关联关系是在model外部持有，即externalJoin
    // isLikedComment拿到的是Boolean，最终要得到的也是Boolean，工具会自动识别不进行映射
    val isLiked: Boolean? by exJoin(::isLikedComment)
}
```

>relation声明
```Kotlin
// 声明Article对象通过id字段索引
// 声明Article对象通过getArticleByIds方法构建
Article::class indexBy Article::id
Article::class buildBy ::getArticleByIds

User::class indexBy User::id
User::class buildBy ::getUserByIds

Comment::class indexBy Comment::id
Comment::class buildBy ::getCommentByIds

// 声明ArticleView是以Article为基础的目标model
// 即Article是ArticleView的伴生
ArticleView::class accompanyBy Article::class
// 声明CommentView是以Comment为基础的目标model
// 即Comment是CommentView的伴生
CommentView::class accompanyBy Comment::class
```

>目标model的获取
```Kotlin
// 索引→目标model，批量构建
val articleViews = articleIds mapMulti ArticleView::class
// 元model→目标model，批量构建
val articleViews = articles mapMulti ArticleView::class
// 索引→目标model，单一构建
val articleViews = articleIds mapSingle ArticleView::class
// 元model→目标model，单一构建
val articleViews = articleIds mapSingle ArticleView::class
```

# 名词定义

## accompany
* 元model：可以从外部系统（例如DAO、RPC）中根据索引字段（一般是主键），直接查询的model。
* 元model，记作accompany，简称A。
* 记做accompany，是因为目标model必须有一个元model类型的单参构造函数，所以元model就像是目标model的伴生一样。
* A并不是必须要有对应的目标model。例如[代码演示](#代码演示)中的User，虽然没有对应的目标model，但也是A。
* A一般是业务中现成已有的，可脱离agile4j-model-builder单独存在。
* A必须进行indexBy&buildBy声明，声明在JVM生命周期中只需进行一次，且必须在mapMulti/mapSingle调用之前执行。例如：
```Kotlin
// indexBy function类型必须为 (元model)->索引 即(A)->I
User::class indexBy User::id
// buildBy function类型必须为 (Collection<索引>)->map<索引,元model> 即(Collection<I>)->map<I,A>
User::class buildBy ::getUserByIds
```

## index
* 索引：能够唯一标识accompany的字段的类型。如果accompany是DB model，则对应数据库主键的类型。
* 索引，记作index，简称I。
* 在对accompany进行indexBy/buildBy声明时，function的类型，必须与index的类型对应。
```Kotlin
// indexBy function类型必须为 (元model)->索引 即(A)->I
User::class indexBy User::id
// buildBy function类型必须为 (Collection<索引>)->map<索引,元model> 即(Collection<I>)->map<I,A>
User::class buildBy ::getUserByIds
```

## target
* 目标model：组装其他model以得到含有当前业务需要的完整数据的model，即构建的目标。
* 目标model，记作target，简称T。
* T必须有一个A的单参构造函数，可通过inJoin/exJoin声明与其他model的关联关系，例如：
```Kotlin
data class ArticleView (val article: Article) {
    val user: User? by inJoin(Article::userId)
    val commentViews: Collection<CommentView>? by exJoin(::getCommentIdsByArticleIds)
}
```
* T必须进行accompanyBy声明，声明在JVM生命周期中只需进行一次，且必须在mapMulti/mapSingle调用之前执行。例如：
```Kotlin
CommentView::class accompanyBy Comment::class
```

## inJoin
* 内关联：model和model之间的关联关系由model字段值持有，即model内部持有，不需要额外查询。
* 内关联，记作InternalJoin，简称inJoin，或IJ。
* 例如[使用场景](#使用场景)中Article和User之间的关联关系，由Article字段值userId持有。
```Kotlin
data class ArticleView (val article: Article) {
    val user: User? by inJoin(Article::userId)
}
```

## exJoin
* 外关联：model和model之间的关联关系在model外部的第三方（例如DB中的关联表、第三方RPC服务）持有，需要额外查询。
* 外关联，记作ExternalJoin，简称exJoin，或EJ。
* 例如[使用场景](#使用场景)中Article和Comment之间的关联关系，在第三方持有，通过getCommentIdsByArticleIds查询。
```Kotlin
data class ArticleView (val article: Article) {
    val commentViews: Collection<CommentView>? by exJoin(::getCommentIdsByArticleIds)
}
```

## map
* 通过I/A得到T的构建过程，即映射的过程，记为map。
* 本文中的 构建、映射、map，同义。
* 构建分为 批量构建、单一构建，还分为 通过I的构建、通过A的构建。总共4中用法：
```Kotlin
// I→T，批量构建
val articleViews = articleIds mapMulti ArticleView::class
// A→T，批量构建
val articleViews = articles mapMulti ArticleView::class
// I→T，单一构建
val articleViews = articleIds mapSingle ArticleView::class
// A→T，单一构建
val articleViews = articleIds mapSingle ArticleView::class
```

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

# API
* agile4j-model-builder的使用过程中，接入方需要了解的全部API只有：indexBy、buildBy、accompanyBy、inJoin、exJoin、mapMulti、mapSingle。

## indexBy
* indexBy用来声明accompany的indexer，以便agile4j-model-builder通过accompany抽取index。
* indexBy接收一个类型为`(A) -> I`的indexer：
```Kotlin
infix fun <A: Any, I> KClass<A>.indexBy(indexer: (A) -> I)
```
* indexBy声明在JVM生命周期中只需进行一次，且必须在mapMulti/mapSingle调用之前执行。
* indexBy使用示例：
```Kotlin
Article::class indexBy Article::id
```

## buildBy
* buildBy用来声明accompany的builder，以便agile4j-model-builder通过index构建accompany。
* buildBy接收一个类型为`(Collection<I>) -> Map<I, A>`的builder：
```Kotlin
infix fun <A: Any, I> KClass<A>.buildBy(builder: (Collection<I>) -> Map<I, A>)
```
* buildBy声明在JVM生命周期中只需进行一次，且必须在mapMulti/mapSingle调用之前执行。
* buildBy使用示例：
```Kotlin
Article::class buildBy ::getArticleByIds
```

## accompanyBy
* accompanyBy用来声明target和accompany之间的对应关系，以便agile4j-model-builder通过accompany构建target。
* accompanyBy代码定义：
```Kotlin
infix fun <T: Any, A: Any> KClass<T>.accompanyBy(aClazz: KClass<A>)
```
* accompanyBy声明在JVM生命周期中只需进行一次，且必须在mapMulti/mapSingle调用之前执行。
* 一个accompany可以与多个target对应，但一个target有且只有一个accompany与之对应。
* accompanyBy使用示例：
```Kotlin
ArticleView::class accompanyBy Article::class
```

## inJoin
* inJoin用于声明accompany和其他model之间的内关联关系，以便agile4j-model-builder通过accompany构建其他model。
* inJoin接收一个类型为`(A) -> IJP?`的mapper，返回一个可委托对象，通过Kotlin的by关键字，指明被委托属性。代码定义：
```Kotlin
// IJP:inJoinProvide，inJoin的mapper响应的类型
// IJR:inJoinRequire，inJoin对应的委托属性的类型
fun <A: Any, IJP: Any, IJR: Any> inJoin(mapper: (A) -> IJP?) =
    InJoinDelegate<A, IJP, IJR>(mapper)
```
* inJoinAPI在target的定义中使用。以IJP为Index，IJR为Accompany，即模式`A→IJI→IJA`（完整模式共16种，参照[自动映射](#自动映射)）。示例：
```Kotlin
// IJP: Long
// IJR: User
data class ArticleView (val article: Article) {
    val user: User? by inJoin(Article::userId)
}
```

## exJoin
* exJoin用于声明accompany和其他model之间的外关联关系，以便agile4j-model-builder通过accompany构建其他model。
* exJoin接收一个类型为`(Collection<I>) -> Map<I, EJP?>`的mapper，返回一个可委托对象，通过Kotlin的by关键字，指明被委托属性。代码定义：
```Kotlin
fun <I: Any, EJP: Any, EJR: Any> exJoin(mapper: (Collection<I>) -> Map<I, EJP?>) =
    ExJoinDelegate<I, Any, EJP, EJR>(mapper)
```
* inJoinAPI在target的定义中使用。以EJP为Index的集合，EJR为Target的集合，即模式`A→C[EJI]→C[EJT]`（完整模式共16种，参照[自动映射](#自动映射)）。示例：
```Kotlin
data class ArticleView (val article: Article) {
    val commentViews: Collection<CommentView>? by exJoin(::getCommentIdsByArticleIds)
}
```

## mapMulti
* mapMulti用来进行批量构建。
* mapMulti参数支持index/accompany的集合两种方式：
```Kotlin
// IXA:index or accompany
infix fun <T: Any, IXA: Any> Collection<IXA?>.mapMulti(clazz: KClass<T>): Collection<T>
```
* 使用示例：
```Kotlin
val articleViews = articleIds mapMulti ArticleView::class
val articleViews = articles mapMulti ArticleView::class
```
* mapMulti是中缀函数，只能在Kotlin环境下调用，为了方便Java的使用，定义了对应的Java友好的API：
```Kotlin
// IXA:index or accompany
fun <T : Any, IXA: Any> buildMulti(clazz: Class<T>, sources: Collection<IXA?>) : Collection<T>
```
* 使用示例：
```Kotlin
val articleViews = buildMulti(ArticleView::class, articleIds)
val articleViews = buildMulti(ArticleView::class, articles)
```

## mapSingle
* mapSingle用来进行单一构建。
* mapSingle参数支持index/accompany两种方式：
```Kotlin
// IXA:index or accompany
infix fun <T: Any, IXA: Any> IXA?.mapSingle(clazz: Class<T>): T?
```
* 使用示例：
```Kotlin
val articleView = articleId mapSingle ArticleView::class
val articleView = article mapSingle ArticleView::class
```
* mapSingle是中缀函数，只能在Kotlin环境下调用，为了方便Java的使用，定义了对应的Java友好的API：
```Kotlin
// IXA:index or accompany
fun <T : Any, IXA: Any> buildSingle(clazz: KClass<T>, source: IXA?): T?
```
* 使用示例：
```Kotlin
val articleView = buildSingle(ArticleView::class, articleId)
val articleView = buildSingle(ArticleView::class, article)
```

# 如何接入

## step1.定义Target
* 通过API `inJoin`、`exJoin`，在target中进行关联关系的声明，例如：
```Kotlin
data class ArticleView (val article: Article) {
    val user: User? by inJoin(Article::userId)
    val commentViews: Collection<CommentView>? by exJoin(::getCommentIdsByArticleIds)
}

data class CommentView(val comment: Comment) {
    val isLiked: Boolean? by exJoin(::isLikedComment)
    val isLikeShowMsg:String get() = if (isLiked == true) "是" else "否"
}
```

## step2.声明Relation
* 通过API `indexBy`、`buildBy`、`accompanyBy`，声明model之间的关系，例如：
```Kotlin
fun initModelBuilder() {
    Article::class indexBy Article::id
    Article::class buildBy ::getArticleByIds

    User::class indexBy User::id
    User::class buildBy ::getUserByIds

    Comment::class indexBy Comment::id
    Comment::class buildBy ::getCommentByIds


    ArticleView::class accompanyBy Article::class
    CommentView::class accompanyBy Comment::class
}
```
* 确保initModelBuilder在"世界开始之初"被执行。

## step3.构建Target
* 通过API `mapMulti`、`mapSingle`，构建target对象，例如：
```Kotlin
val articleViews = articleIds mapMulti ArticleView::class
val articleViews = articles mapMulti ArticleView::class
val articleViews = articleIds mapSingle ArticleView::class
val articleViews = articleIds mapSingle ArticleView::class
```

# Java如何接入
* 如果组内成员对Kotlin语法不了解，或当前业务代码为Java，无法通过中缀函数mapMulti、mapSingle构建target，如何使用agile4j-model-builder？
* 为解决该问题，可以将上一节[如何接入](#如何接入)中[step1.定义Target](#step1定义Target)和[step3.构建Target](#step3构建Target)进行变动。
* 思路是：
 1. [step1.定义Target](#step1定义Target)中对字段的业务逻辑处理，需要Kotlin的语法，例如
    `val isLikeShowMsg:String get() = if (isLiked == true) "是" else "否"`，
    为了避免业务逻辑使用Kotlin，可以把数据和逻辑分离，将View拆分成只包含数据的DTO和只包含逻辑的VO两部分。
    其中DTO仍然使用Kotlin表达，因为对Kotlin语法的依赖极少，像配置文件一样Ctrl+C、Ctrl+V即可，不会对工具的使用造成阻碍。
    VO中可以用Java处理业务逻辑。
 2. [step2.声明Relation](#step2声明Relation)部分作为"世界开始之初"需要执行的部分，较为独立，可放在单独的Kotlin文件中。且对Kotlin语法的依赖极少，像配置文件一样Ctrl+C、Ctrl+V即可，不需要改动。
 3. [step3.构建Target](#step3构建Target)中的API mapMulti、mapSingle是中缀函数，在Java中无法调用，换成工具提供的对Java友好的API buildMulti、buildSingle即可。
 
* 完整内容如下：
## step1.定义Target-DTO
```Kotlin
data class ArticleDTO (val article: Article) {
    val user: User? by inJoin(Article::userId)
    val commentViews: Collection<CommentView>? by exJoin(::getCommentIdsByArticleIds)
}
```

## step2.定义Target-VO
```Java
public class ArticleVO extends ArticleDTO {
   public String getAuthorName() {
       User user = getUser(); // 取自ArticleDTO
       return user == null ? "" : user.getName();
   }
}
```

## step3.声明Relation
```Kotlin
// 新建文件ModelBuilderRelations.kt，按如下格式配置自己的业务
fun initModelBuilder() {
   Article::class indexBy Article::id
   Article::class buildBy ::getArticleByIds

   User::class indexBy User::id
   User::class buildBy ::getUserByIds

   Comment::class indexBy Comment::id
   Comment::class buildBy ::getCommentByIds


   ArticleVO::class accompanyBy Article::class
   ArticleDTO::class accompanyBy Article::class
   CommentVO::class accompanyBy Comment::class
   CommentDTO::class accompanyBy Comment::class
}
```

## step4.构建Target
```Java
Collection<ArticleVO> articleVOs = buildMulti(ArticleVO.class, articleIds);
Collection<ArticleVO> articleVOs = buildMulti(ArticleVO.class, articles);
ArticleVO articleVO = buildSingle(ArticleVO.class, articleId);
ArticleVO articleVO = buildSingle(ArticleVO.class, article);
```
