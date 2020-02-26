package pl.wojtek.ask

import androidx.lifecycle.ViewModel
import com.poccofinance.core.rx.SchedulerUtils
import io.reactivex.Flowable
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.processors.BehaviorProcessor
import io.reactivex.processors.PublishProcessor
import io.reactivex.rxkotlin.subscribeBy
import pl.wojtek.ask.data.Repository
import pl.wojtek.ask.network.SearchResult
import pl.wojtek.pagination.PaginModel
import java.util.concurrent.TimeUnit


interface SearchForReposVM {
    fun query(text: String)
    fun loadMore()

    fun listOfRepos(): Flowable<List<Repository>>
    fun errors(): Flowable<String>
    fun loading(): Flowable<Boolean>
}

const val TIMEOUT_BEFORE_SEARCH = 300L

class SearchForReposViewModel(private val schedulerUtils: SchedulerUtils,
                              private val paginModel: PaginModel<String, Repository, SearchResult>,
                              private val debounceScheduler: Scheduler,
                              private val timeout: Long = TIMEOUT_BEFORE_SEARCH) : SearchForReposVM, ViewModel() {

    private val loadingProcessor: BehaviorProcessor<Boolean> = BehaviorProcessor.create()
    private val listProcessor: BehaviorProcessor<List<Repository>> = BehaviorProcessor.create()
    private val queryStream: PublishProcessor<String> = PublishProcessor.create()
    private val errorStream: PublishProcessor<String> = PublishProcessor.create()
    private val compositeDisposable: CompositeDisposable = CompositeDisposable()
    private var modelDispose: Disposable? = null

    init {
        compositeDisposable.add(queryStream
            .debounce(timeout, TimeUnit.MILLISECONDS, debounceScheduler)
            .distinctUntilChanged()
            .subscribeBy(
                onNext = { query: String ->
                    modelDispose?.dispose()
                    paginModel.setQuery(query)
                    loadMore()
                }
            ))
        compositeDisposable.add(
            paginModel.loadingState()
                .distinctUntilChanged()
                .observeOn(schedulerUtils.observScheduler)
                .subscribeBy {
                    loadingProcessor.onNext(it)
                }
        )
    }

    override fun query(text: String) {
        queryStream.onNext(text)
    }

    override fun loadMore() {
        modelDispose = paginModel.askForMore()
            .subscribeOn(schedulerUtils.subscribeScheduler)
            .observeOn(schedulerUtils.observScheduler)
            .subscribeBy(
                onSuccess = { list: List<Repository> ->
                    listProcessor.onNext(list)
                },
                onError = { error: Throwable ->
                    errorStream.onNext(error.message ?: "Error happens")
                })
    }

    override fun listOfRepos(): Flowable<List<Repository>> = listProcessor

    override fun errors(): Flowable<String> = errorStream

    override fun loading(): Flowable<Boolean> = loadingProcessor

    override fun onCleared() {
        super.onCleared()
        compositeDisposable.dispose()
        modelDispose?.dispose()
    }
}
