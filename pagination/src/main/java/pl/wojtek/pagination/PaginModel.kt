package pl.wojtek.pagination

import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.processors.BehaviorProcessor

/**
 *
 */


interface PaginModel<Q, R, A> {
    fun setQuery(q: Q)
    fun askForMore(): Maybe<List<R>>
    fun loadingState(): Flowable<Boolean>
}


interface DataSource<Q, A> {
    fun askForData(query: QueryParams<Q>): Maybe<A>
}

data class MappedData<Q, R>(val query: Q, val list: List<R>, val max: Int = 0)
interface DataMapper<A, R, Q> {
    fun map(a: A,q:QueryParams<Q>): MappedData<Q, R>
}


class PaginModelImp<Q, R, A>(private val dataSource: DataSource<Q, A>,
                                      private val mapper: DataMapper<A, R, Q>,
                                      private val dataHolder: DataHolder<Q, R> = DataHolderImp(),
                                      private val queryDataHolder: QueryDataHolder<Q> = QueryDataHolderImp()) : PaginModel<Q, R, A> {
    private val loadingSubject: BehaviorProcessor<Boolean> = BehaviorProcessor.createDefault(false)
    override fun setQuery(q: Q) {
        queryDataHolder.setQuery(q)
    }

    override fun askForMore(): Maybe<List<R>> {
        return Single.fromCallable { queryDataHolder.canAskForAnotherOne() }
            .filter { it && !(loadingSubject.value?:false) }
            .map {
                loadingSubject.onNext(true)
                queryDataHolder.provideQueryParams()
            }
            .flatMap { dataSource.askForData(it) }
            .map { mapper.map(it,queryDataHolder.provideQueryParams()) }
            .doOnSuccess { queryDataHolder.setMax(it.query, it.max) }
            .map { dataHolder.provideData(it.query, it.list) }
            .doOnSuccess {
                queryDataHolder.turnToNextPage()
                loadingSubject.onNext(false)
            }.doOnDispose { loadingSubject.onNext(false)  }
            .doOnError { loadingSubject.onNext(false) }

    }

    override fun loadingState(): Flowable<Boolean> = loadingSubject
}