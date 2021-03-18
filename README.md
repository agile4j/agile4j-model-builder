# agile4j-model-builder

agile4j-model-builder是用Kotlin语言实现的model构建器，可在Kotlin/Java工程中使用。

# 如何引入

>Gradle
```groovy
dependencies {
    compile "com.agile4j:agile4j-model-builder:1.2.2"
}
```
>Maven
```xml
<dependency>
    <groupId>com.agile4j</groupId>
    <artifactId>agile4j-model-builder</artifactId>
    <version>1.2.2</version>
</dependency>
```

# 如何使用

## step1.定义Target
* 通过API [inJoin](readme/README_API.md#injoin)、[exJoin](readme/README_API.md#exJoin)，在target中进行关联关系的声明，例如：
```Kotlin
data class ArticleView (val article: Article) {
    // 数据
    val user: User? by inJoin(Article::userId)
    val commentViews: Collection<CommentView>? by exJoin(::getCommentIdsByArticleIds)

    // 逻辑
    val userName: String? get() = user?.userName
}

data class CommentView(val comment: Comment) {
    // 数据
    val isLiked: Boolean? by exJoin(::isLikedComment)

    // 逻辑
    val isLikeShowMsg:String get() = if (isLiked == true) "是" else "否"
}
```

## step2.声明Relation
* 通过API [indexBy](readme/README_API.md#indexBy)、[buildBy](readme/README_API.md#buildBy)、[targets](readme/README_API.md#targets)，声明Relation，例如：
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
* 通过API [mapMulti](readme/README_API.md#mapMulti)、[mapSingle](readme/README_API.md#mapSingle)，构建target对象，例如：
```Kotlin
// 可以通过id构建，也可通过article对象构建
val articleViews = articleIds mapMulti ArticleView::class
```

# 了解更多

[使用场景](readme/README_SCENARIO.md)

[名词定义](readme/README_TERM.md)

[特性](readme/README_FEATURE.md)

[API](readme/README_API.md)

[高级用法](readme/README_PRO.md)

[JAVA如何接入](readme/README_JAVA.md)