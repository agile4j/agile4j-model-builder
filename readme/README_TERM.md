# 名词定义

## accompany
* 元model：可以从外部系统（例如DAO、RPC）中根据索引字段（一般是主键），直接查询的model。
* 元model，记作accompany，简称A。
* 记做accompany，是因为目标model必须有一个元model类型的单参构造函数，所以元model就像是目标model的伴生一样。
* A并不是必须要有对应的目标model。例如[代码演示](README_SCENARIO.md#代码演示)中的User，虽然没有对应的目标model，但也是A。
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
* 一个target有且只有一个accompany与之对应。
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
* 例如[使用场景](README_SCENARIO.md)中Article和User之间的关联关系，由Article字段值userId持有。
```Kotlin
data class ArticleView (val article: Article) {
    val user: User? by inJoin(Article::userId)
}
```

## exJoin
* 外关联：model和model之间的关联关系在model外部的第三方（例如DB中的关联表、第三方RPC服务）持有，需要额外查询。
* 外关联，记作ExternalJoin，简称exJoin，或EJ。
* 例如[使用场景](README_SCENARIO.md)中Article和Comment之间的关联关系，在第三方持有，通过getCommentIdsByArticleIds查询。
```Kotlin
data class ArticleView (val article: Article) {
    val commentViews: Collection<CommentView>? by exJoin(::getCommentIdsByArticleIds)
}
```

## map
* 通过I/A得到T的构建过程，即映射的过程，记为map。
* 本文中的 构建、映射、map，同义。
* 构建分为 批量构建、单一构建，还分为 通过I的构建、通过A的构建。总共4种用法：
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
