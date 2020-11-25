# 高级用法

# 目录
   * [过滤器](#过滤器)
   * [剪枝器](#剪枝器)
   * [异常处理](#异常处理)
   * [在kts文件中声明Relation](#在kts文件中声明relation)
   * [复用modelBuilder的缓存](#复用modelbuilder的缓存)
   * [集成agile4j-feed-builder](#集成agile4j-feed-builder)


## 过滤器
* 过滤是指对target的过滤。在工具中内置过滤器的好处是：因为agile4j-model-builder[聚合批量构建](README_FEATURE.md#聚合批量构建)的特性，即使在外层对views集合进行了过滤，工具内部仍然会持有被过滤view的引用，对字段求值时的聚合过程，仍会带上已被过滤的view，因为工具对外部过滤逻辑无感知。
* 解决的方案是：在构建api中，支持传入过滤器参数，在返回结果之前，进行过滤，并把被过滤的view在工具内部的引用移除，以切实"滤掉"，不影响后续字段求值时的性能。
* 相关API例如：
```Kotlin
fun <T : Any, IXA: Any> buildMulti(clazz: KClass<T>, sources: Collection<IXA?>, filter: (T) -> Boolean): List<T>

fun <T : Any, IXA: Any> buildMapOfI(clazz: KClass<T>, sources: Collection<IXA?>, filter: (T) -> Boolean) : LinkedHashMap<Any, T>

fun <T : Any, IXA: Any> buildMapOfA(clazz: KClass<T>, sources: Collection<IXA?>, filter: (T) -> Boolean) : LinkedHashMap<Any, T>
```

## 剪枝器
* 剪枝是指对target中通过agile4j-model-builder进行关联构建的字段，求值时的短路操作。
* 举一个场景：一个新上线的http接口，需要在XXXView中新增一个字段。但该字段除了在该接口中用到，其他接口都不会使用。如果不进行剪枝操作，随着此类业务的迭代，view对象的构建会越来越重，每次构建接口调用方的有效字段占比越来越少，造成性能的浪费。
* 解决的方案是：在[inJoin](README_API.md#injoin)、[exJoin](README_API.md#exjoin)接口中，支持传入剪枝器(类型：() → Boolean)。
* 用法例如：
```Kotlin
data class MovieView (val movie: Movie) {
    val videos: Collection<Video>? by exJoin(::getVideosByMovieIds) { isFetchVideos() }
}

object Scopes {
    private val fetchVideos: ScopeKey<Boolean> = ScopeKey.withDefaultValue(false)
    fun setFetchVideos(isFetchVideos: Boolean) = fetchVideos.set(isFetchVideos)
    fun isFetchVideos() = fetchVideos.get() ?: false
}
```
* 被剪枝器短路掉的字段，会直接返回空值：
|字段类型|空值|
|---|---|
|Set|emptySet()|
|List/Collection|emptyList()|
|其他|null|

## 异常处理

## 在kts文件中声明Relation

## 复用modelBuilder的缓存

## 集成agile4j-feed-builder
* [agile4j-feed-builder](https://github.com/agile4j/agile4j-feed-builder)是用Kotlin语言实现的feed流构建器。
* agile4j-feed-builder与agile4j-model-builder集成后有性能优势。因为一次feed构建过程中的多次资源构建，会共用agile4j-model-builder的缓存。
* 具体用法，详见[集成agile4j-model-builder](https://github.com/agile4j/agile4j-feed-builder#%E9%9B%86%E6%88%90agile4j-model-builder)。

