# agile4j-model-builder

agile4j-model-builder是用Kotlin语言实现的model构建器，可在Kotlin/Java工程中使用。

# 如何引入

>Gradle
```groovy
dependencies {
    compile "com.agile4j:agile4j-model-builder:1.1.3"
}
```
>Maven
```xml
<dependency>
    <groupId>com.agile4j</groupId>
    <artifactId>agile4j-model-builder</artifactId>
    <version>1.1.3</version>
</dependency>
```

# 如何使用

## step1.定义Target
* 通过API `inJoin`、`exJoin`，在target中进行关联关系的声明，例如：
```Kotlin
data class ArticleView (val article: Article) {
    val user: User? by inJoin(Article::userId)
    val commentViews: Collection<CommentView>? by exJoin(::getCommentIdsByArticleIds)
}

data class CommentView(val comment: Comment) {
    val isLiked: Boolean? by exJoin(::isLikedComment)
}
```

## step2.声明Relation
* 通过API `indexBy`、`buildBy`、`targets`，声明Relation，例如：
```Kotlin
fun initModelBuilder() {
    Article::class {
        indexBy(Article::id)
        buildBy(::getArticleByIds)
        targets(ArticleView::class)
    }
    User::class {
        indexBy(User::id)
        buildBy(::getUserByIds)
    }
    Comment::class {
        indexBy(Comment::id)
        buildBy(::getCommentByIds)
        targets(CommentView::class)
    }
}
```

## step3.构建Target
* 通过API `mapMulti`、`mapSingle`，构建target对象，例如：
```Kotlin
val articleViews = articleIds mapMulti ArticleView::class
```

# 了解更多

[使用场景](README_SCENARIO.md)

[名词定义](README_TERM.md)

[特性](README_FEATURE.md)

[API](README_API.md)