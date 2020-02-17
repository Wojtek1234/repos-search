package pl.wojtek.ask.data.api

import io.reactivex.Maybe
import pl.wojtek.ask.network.SearchResult
import retrofit2.http.GET
import retrofit2.http.Query

/**
 *
 */


interface SearchReposApi {

    @GET("/search/repositories")
    fun searchRepos(@Query("q") query: String, @Query("page") page: Int,  @Query("per_page") pageSize: Int): Maybe<SearchResult>
}