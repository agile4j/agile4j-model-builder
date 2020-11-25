# Java如何接入
* 如果组内成员对Kotlin语法不了解，如何使用agile4j-model-builder？
* 为解决该问题，可以将[step1.定义Target](../README.md#step1定义Target)和[step3.构建Target](../README.md#step3构建Target)进行变动。
* 思路是：
 1. [step1.定义Target](../README.md#step1定义Target)中对字段的业务逻辑处理，需要Kotlin的语法，例如
    `val isLikeShowMsg:String get() = if (isLiked == true) "是" else "否"`，
    为了避免业务逻辑使用Kotlin，可以把数据和逻辑分离，将View拆分成只包含数据的DTO和只包含逻辑的VO两部分。
    其中DTO仍然使用Kotlin表达，因为对Kotlin语法的依赖极少，像配置文件一样Ctrl+C、Ctrl+V即可，不会对工具的使用造成阻碍。
    VO中可以用Java处理业务逻辑。
 2. [step2.声明Relation](../README.md#step2声明Relation)部分作为"世界开始之初"需要执行的部分，较为独立，可放在单独的Kotlin文件中。且对Kotlin语法的依赖极少，像配置文件一样Ctrl+C、Ctrl+V即可，不需要改动。
 3. [step3.构建Target](../README.md#step3构建Target)中的API mapMulti、mapSingle是中缀函数，在Java中无法调用，换成工具提供的对Java友好的API buildMulti、buildSingle即可。
 
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

## step4.构建Target
```Java
Collection<ArticleVO> articleVOs = buildMulti(ArticleVO.class, articleIds);
Collection<ArticleVO> articleVOs = buildMulti(ArticleVO.class, articles);
ArticleVO articleVO = buildSingle(ArticleVO.class, articleId);
ArticleVO articleVO = buildSingle(ArticleVO.class, article);
```