# agile4j-model-builder

ModelBuilder是用Kotlin语言实现的model构建器，可在Kotlin/Java工程中使用。

# 目录
   * [如何引入](#如何引入)
   * [使用场景](#使用场景)
   * [代码演示](#代码演示)
   * [名词定义](#名词定义)
   * [特性](#特性)
      * [自动映射](#自动映射)
      * [增量lazy式构建](#增量lazy式构建)
      * [聚合批量构建](#聚合批量构建)
      * [不会重复构建](#不会重复构建)
      * [代码零侵入](#代码零侵入)

# 如何引入

>Kotlin
```groovy
dependencies {
    compile "com.agile4j:agile4j-model-builder:1.0.18"
}
```
>Java
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
    * Article→User是通过userId字段在model内部持有。
    * Article→Comment是通过getCommentIdsByArticleIds在model外部持有，例如DB中的关联表，或在第三方系统中持有，需要额外查询。
* model是否需要映射
    * Article→User是根据索引userId，得到元model，即User对象，需要映射。
    * Article→Comment是根据索引commentId，得到目标model，即CommentView对象，需要映射。
    * 还有其他可能的情况。例如：不需要映射；根据元model，得到目标model。
* 是否所有字段都要构建
    * 并非所有字段都要构建。例如：某个业务场景需要构建的ArticleView对象，只会用到user，不会用到commentViews。但希望复用构建逻辑和ArticleView的定义，且不希望浪费性能去构建commentViews。
    * 所有字段都要构建。例如：http接口把view对象json化后响应给客户端，几乎所有的字段都需要构建。

如果每次构建ArticleView，都需要区分处理以上各种情况，那么代码的可复用性和可维护性很低。

可以使用ModelBuilder解决这个问题。

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


# 特性

## 自动映射

* 自动进行index→accompany、index→target、accompany→target之间的映射
* 增量lazy式构建
* 聚合批量构建
* 不会重复构建
* 代码零侵入

![ModelBuilder.svg](https://raw.githubusercontent.com/agile4j/agile4j-model-builder/master/src/test/resources/ModelBuilder.svg)
