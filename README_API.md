# API
* agile4j-model-builder的使用过程中，接入方需要了解的API有：indexBy、buildBy、targets、inJoin、exJoin、mapMulti、mapSingle。

## indexBy
* indexBy用来声明accompany的indexer，以便agile4j-model-builder通过accompany抽取index。
* indexBy接收一个类型为`(A) -> I`的indexer：
```Kotlin
infix fun <A: Any, I> KClass<A>.indexBy(indexer: (A) -> I)
```
* indexBy声明在JVM生命周期中只需进行一次，且必须在mapMulti/mapSingle调用之前执行。
* indexBy使用示例：
```Kotlin
Article::class {
    indexBy(Article::id)
}
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
Article::class {
    buildBy(::getUserByIds)
}
```

## targets
* targets用来声明target和accompany之间的对应关系，以便agile4j-model-builder通过accompany构建target。
* targets代码定义：
```Kotlin
fun <A: Any> KClass<A>.targets(vararg targets: KClass<*>)
```
* targets声明在JVM生命周期中只需进行一次，且必须在mapMulti/mapSingle调用之前执行。
* 一个accompany可以与多个target对应，但一个target有且只有一个accompany与之对应。
* targets使用示例：
```Kotlin
Article::class {
    targets(ArticleDTO::class, ArticleView::class)
}
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
