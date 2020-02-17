package pl.wojtek.ask.data

import pl.wojtek.ask.network.SearchResult
import pl.wojtek.pagination.DataMapper
import pl.wojtek.pagination.MappedData
import pl.wojtek.pagination.QueryParams

/**
 *
 */


class ReposDataMapper : DataMapper<SearchResult, Repository, String> {
    override fun map(a: SearchResult, q: QueryParams<String>): MappedData<String, Repository> =
        MappedData(q.query ?: "", a.items.map { Repository(it.name, it.htmlUrl, it.stargazersCount, it.owner.login,it.id) }, a.totalCount)

}