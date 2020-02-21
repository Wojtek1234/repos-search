package pl.wojtek.searchwithcoroutines.data.api


import pl.wojtek.searchwithcoroutines.network.SearchResult
import retrofit2.http.GET
import retrofit2.http.Query

/**
 *
 */


interface SearchReposApi {

    @GET("/search/repositories")
    suspend fun searchRepos(@Query("q") query: String, @Query("page") page: Int,  @Query("per_page") pageSize: Int): SearchResult
}