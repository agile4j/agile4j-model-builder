# agile4j-model-builder

ModelBuilder是用Kotlin语言实现的model构建器，可在Kotlin/Java工程中使用。

# 目录
   * [如何引入](#如何引入)
   * [使用场景](#使用场景)
   * [代码演示](#代码演示)
   * [名词定义](#名词定义)
      * [Accompany](#Accompany)
      * [Index](#Index)
      * [Target](#Target)
      * [InJoin](#InJoin)
      * [ExJoin](#ExJoin)
      * [Map](#Map)
   * [特性](#特性)
      * [自动映射](#自动映射)
      * [增量lazy式构建](#增量lazy式构建)
      * [聚合批量构建](#聚合批量构建)
      * [不会重复构建](#不会重复构建)
      * [代码零侵入](#代码零侵入)
   * [Java如何接入](#Java如何接入)


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
fun getArticleByIds(ids: Collection<Long>): Map<Long, Article> 

// 查询用户信息
fun getUserByIds(ids: Collection<Long>): Map<Long, User> 

// 查询评论信息
fun getCommentByIds(ids: Collection<Long>): Map<Long, Comment> 

// 查询文章下所有的评论
fun getCommentIdsByArticleIds(ids: Collection<Long>): Map<Long, Collection<Long>>

// 查询当前登录者是否已对该评论点赞（当前登录者userId存在线程上下文中，因此未在参数中体现）
fun isLikedComment(ids: Collection<Long>): Map<Long, Boolean>
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

可以使用ModelBuilder解决上述场景。

# 代码演示

为了对ModelBuilder的使用有一个直观的感受，针对上述示例中的业务场景，给出解决方案的代码。如果对代码中有不理解的地方，可以先跳过"代码演示"部分，继续浏览下文。

>目标model定义
```Kotlin
data class ArticleView (val article: Article) {
    // inJoin表示关联关系是在model内部持有，即internalJoin
    // Article::userId拿到的是索引(userId)，最终要得到的是元model(User对象)，工具会自动识别并进行映射
    val user: User? by inJoin(Article::userId),
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
// 即Article是ArticleView的伴生对象
ArticleView::class accompanyBy Article::class
// 声明CommentView是以Comment为基础的目标model
// 即Comment是CommentView的伴生对象
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

## Accompany
* 元model：可以从外部系统（例如DAO、RPC）中根据索引字段（一般是主键），直接查询的model。
* 元model，记作Accompany，简称A。
* 记做Accompany，是因为目标model必须有一个元model类型的单参构造函数，所以元model就像是目标model的伴生一样。
* A并不是必须要有对应的目标model。例如[代码演示](#代码演示)中的User，虽然没有对应的目标model，但也是A。
* A一般是业务中现成已有的，可脱离ModelBuilder单独存在。
* A必须进行indexBy&buildBy声明，声明在JVM生命周期中只需进行一次，且必须在mapMulti/mapSingle调用之前执行。例如：
```Kotlin
// indexBy function类型必须为 (元model)->索引 即(A)->I
User::class indexBy User::id
// buildBy function类型必须为 (Collection<索引>)->Map<索引,元model> 即(Collection<I>)->Map<I,A>
User::class buildBy ::getUserByIds
```

## Index
* 索引：能够唯一标识Accompany的字段的类型。如果Accompany是DB model，则对应数据库主键的类型。
* 索引，记作Index，简称I。
* 在对Accompany进行indexBy/buildBy声明时，function的类型，必须与index的类型对应。
```Kotlin
// indexBy function类型必须为 (元model)->索引 即(A)->I
User::class indexBy User::id
// buildBy function类型必须为 (Collection<索引>)->Map<索引,元model> 即(Collection<I>)->Map<I,A>
User::class buildBy ::getUserByIds
```

## Target
* 目标model：组装其他model以得到含有当前业务需要的完整数据的model，即构建的目标。
* 目标model，记作Target，简称T。
* T必须有一个A的单参构造函数，例如：
```Kotlin
data class CommentView(val comment: Comment) {
    val isLiked: Boolean? by exJoin(::isLikedComment)
}
```
* T必须进行accompanyBy声明，声明在JVM生命周期中只需进行一次，且必须在mapMulti/mapSingle调用之前执行。例如：
```Kotlin
CommentView::class accompanyBy Comment::class
```

## InJoin
* 内关联：model和model之间的关联关系由model字段值持有，即model内部持有，不需要额外查询。
* 内关联，记作InternalJoin，简称InJoin，或IJ。
* 例如[使用场景](#使用场景)中Article和User之间的关联关系，由Article字段值userId持有。

## ExJoin
* 外关联：model和model之间的关联关系在model外部的第三方（例如DB中的关联表、第三方RPC服务）持有，需要额外查询。
* 外关联，记作ExternalJoin，简称ExJoin，或EJ。
* 例如[使用场景](#使用场景)中Article和Comment之间的关联关系，在第三方持有，通过getCommentIdsByArticleIds查询。

## Map
* 通过I/A得到T的构建过程，即映射的过程，记为Map。
* 本文中的 构建、映射、Map，同义。
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
    1. InJoin/ExJoin，共2种情况
    2. 1对1/1对多，共2种情况
    3. 映射类型：I→A、I→T、A→T、M→M(即同类型)，共4种情况
* ModelBuilder会对这2\*2\*4=16种情况自动识别并映射，这16种情况即所有情况，不应出现其他情况，如果出现其他情况说明代码存在逻辑问题，build时会抛出异常。
* 综上，自动映射机制所能解决的所有问题域为：
![ModelBuilder.svg](https://raw.githubusercontent.com/agile4j/agile4j-model-builder/master/src/test/resources/ModelBuilder.svg)

* 完整问题域代码演示，代码位置：agile4j-model-builder/src/test/kotlin/com/agile4j/model/builder/mock

<details>
<summary>Model.kt</summary>

```Kotlin
data class MovieView (val movie: Movie) {

    // A->IJM
    val idInJoin: Long? by inJoin(Movie::id)

    // A->C[IJM]
    val subscriberIdsInJoin: Collection<Long>? by inJoin(Movie::subscriberIds)

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
fun getMovieByIds(ids: Collection<Long>): Map<Long, Movie>
fun getVideosByMovieIds(ids: Collection<Long>): Map<Long, Collection<Video>>
fun getTrailersByMovieIds(ids: Collection<Long>): Map<Long, Video>
fun getVideoIdsByMovieIds(ids: Collection<Long>): Map<Long, Collection<Long>>
fun getTrailerIdsByMovieIds(ids: Collection<Long>): Map<Long, Long>
fun getCountsByMovieIds(ids: Collection<Long>): Map<Long, Count>
fun getInteractionsByMovieIds(ids: Collection<Long>): Map<Long, MovieInteraction>
fun getSourcesByVideoIds(ids: Collection<Long>): Map<Long, Source>
fun getVideoByIds(ids: Collection<Long>): Map<Long, Video>
fun getSourceByIds(ids: Collection<Long>): Map<Long, Source>
fun getUserByIds(ids: Collection<Long>): Map<Long, User>
fun isShared(ids: Collection<Long>): Map<Long, Boolean>
```
</details>

## 增量lazy式构建
* 举一个场景：某个业务需要构建的ArticleView对象，只会用到user，不会用到commentViews。但希望复用构建逻辑和ArticleView的定义，且不希望浪费性能去构建commentViews。
* 为了满足这个需求，ModelBuilder采用的是增量lazy式构建。即通过mapMulti/mapSingle构建结束时，仅会真实取到A的值，而所有的关联model的值都不会取，所以构建速度极快。
    1. 如果通过I构建，仅需一次function调用，(批量)获得A的值后，构建过程就已结束。
    2. 如果通过A构建，则一次function调用都没有，直接结束。
* 如果对构建完后T调用取值方法，则仅会对取值方法涉及到的关联model进行取值，无关model仍然不会构建，实现 `所用即所取`。

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
    val user: User? by inJoin(Article::userId),
    val checkerViews: Collection<UserView>? by inJoin(Article::checkerIds)
}

// User、UserView定义略
```
* 上述代码中，Article的字段userId、checkerIds的值都是User的索引。 在ArticleView中分别映射成了ijA(User)和ijT(UserView)。
* 假设通过`val articleViews = articles mapMulti ArticleView::class`来构建ArticleView，并将构建结果articleViews进行JSON化(以使所有字段都进行取值)。整个过程中，ModelBuilder只会调用一次getUserByIds方法。因为ModelBuilder会先将所有Article中的userId和checkerIds的值`聚合`成一个Collection，然后一次性`批量`查询，以减少对第三方function的调用频率(网络IO等往往会使调用过程耗时很长)，以提高性能。

## 不会重复构建
* 所有的构建结果ModelBuilder都会缓存，对同一字段的多次访问，不会重复构建。


## 代码零侵入
* ModelBuilder的使用过程中，接入方需要了解的全部内容只有API：indexBy、buildBy、accompanyBy、inJoin、exJoin、mapMulti、mapSingle。除此之外没有任何概念和类需要了解，且对Accompany的代码没有任何侵入，可读性和语义化强。

# Java如何接入
* 如果组内成员对Kotlin语法不了解，如何使用ModelBuilder？
* ModelBuilder的使用过程分为4部分：
    1. relation声明：indexBy、buildBy、accompanyBy的使用
        * 该部分作为"世界开始之初"需要执行的部分，较为独立，可放在单独的Kotlin文件中。且对Kotlin语法的依赖极少，像配置文件一样Ctrl+C、Ctrl+V即可。例如：
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
    2. Target中关联关系的声明：inJoin、exJoin的使用
        * 该部分可拆分成独立的数据Model，只保留关联关系的声明。即可几乎不依赖Kotlin的语法知识，例如：
        ```Kotlin
        data class ArticleDTO (val article: Article) {
            val user: User? by inJoin(Article::userId),
            val commentViews: Collection<CommentView>? by exJoin(::getCommentIdsByArticleIds)
        }
        ```
    3. 对Target中字段的处理过程：例如字符串的截取、数字格式化等
        * 该部分有大量的业务逻辑，需要在java环境中处理，可声明java类ArticleVO，继承ArticleDTO，例如：
        ```Java
        public class ArticleVO extends ArticleDTO {
           public String getAuthorName() {
               User user = getUser(); // 取自ArticleDTO
               return user == null ? "" : user.getName();
           }
        }
        ```
    4. 构建过程：mapMulti、mapSingle的使用
        * 因为mapMulti、mapSingle是Kotlin的中缀函数，无法在Java环境调用，因此ModelBuilder提供了Java友好的API：
        ```Java
        // I→T，批量构建
        Collection<ArticleVO> articleVOs = buildMulti(ArticleVO.class, articleIds);
        // A→T，批量构建
        Collection<ArticleVO> articleVOs = buildMulti(ArticleVO.class, articles);
        // I→T，单一构建
        ArticleVO articleVO = buildSingle(ArticleVO.class, articleId);
        // A→T，单一构建
        ArticleVO articleVO = buildSingle(ArticleVO.class, article);
        ```
