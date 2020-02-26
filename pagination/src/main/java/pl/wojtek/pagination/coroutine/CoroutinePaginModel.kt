package pl.wojtek.pagination.coroutine

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import pl.wojtek.pagination.*

/**
 *

 */
interface CoroutinePaginModelFactory {
    fun <Q, A, R> createPaginModel(dataSource: CoroutineDataSource<Q, A>, dataMapper: DataMapper<A, R, Q>): CoroutinePaginModel<Q, R, A>
}

internal class CoroutinePaginModelFactoryImp : CoroutinePaginModelFactory {
    override fun <Q, A, R> createPaginModel(dataSource: CoroutineDataSource<Q, A>, dataMapper: DataMapper<A, R, Q>): CoroutinePaginModel<Q, R, A> {
        return CoroutinPaginModelImp(dataSource, dataMapper)
    }
}

interface CoroutinePaginModel<Q, R, A> {
    suspend fun setQuery(q: Q)
    suspend fun askForMore(): List<R>?
    fun loadingState(): Flow<Boolean>
    fun clear()
}


interface CoroutineDataSource<Q, A> {
    suspend fun askForData(query: QueryParams<Q>): A
}


@ExperimentalCoroutinesApi
internal class CoroutinPaginModelImp<Q, R, A>(private val dataSource: CoroutineDataSource<Q, A>,
                                              private val mapper: DataMapper<A, R, Q>,
                                              private val dataHolder: DataHolder<Q, R> = DataHolderImp(),
                                              private val queryDataHolder: QueryDataHolder<Q> = QueryDataHolderImp()) : CoroutinePaginModel<Q, R, A> {
    private val loadingChannel: ConflatedBroadcastChannel<Boolean> = ConflatedBroadcastChannel(false)
    override suspend fun setQuery(q: Q) {
        loadingChannel.send(false)
        queryDataHolder.setQuery(q)
    }

    override suspend fun askForMore(): List<R>? {
        return try {
            queryDataHolder.canAskForAnotherOne()
                .takeIf { it && !loadingChannel.value }
                ?.let {
                    loadingChannel.send(true)
                    queryDataHolder.provideQueryParams()
                }
                ?.let { dataSource.askForData(it) }
                ?.let { mapper.map(it, queryDataHolder.provideQueryParams()) }
                ?.let {
                    queryDataHolder.turnToNextPage()
                    loadingChannel.send(false)
                    queryDataHolder.setMax(it.query, it.max)
                    dataHolder.provideData(it.query, it.list)
                }
        } catch (ex: Throwable) {
            loadingChannel.send(false)
            throw ex
        }
    }


    override fun loadingState(): Flow<Boolean> = loadingChannel.asFlow()

    override fun clear() {
        loadingChannel.close()
    }


}