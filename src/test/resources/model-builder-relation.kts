import com.agile4j.model.builder.mock.Movie
import com.agile4j.model.builder.mock.MovieDTO
import com.agile4j.model.builder.mock.MovieView
import com.agile4j.model.builder.mock.Source
import com.agile4j.model.builder.mock.User
import com.agile4j.model.builder.mock.UserView
import com.agile4j.model.builder.mock.Video
import com.agile4j.model.builder.mock.VideoDTO
import com.agile4j.model.builder.mock.getMovieByIds
import com.agile4j.model.builder.mock.getSourceByIds
import com.agile4j.model.builder.mock.getUserByIds
import com.agile4j.model.builder.mock.getVideoByIds
import com.agile4j.model.builder.relation.buildBy
import com.agile4j.model.builder.relation.indexBy
import com.agile4j.model.builder.relation.invoke
import com.agile4j.model.builder.relation.targets

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
