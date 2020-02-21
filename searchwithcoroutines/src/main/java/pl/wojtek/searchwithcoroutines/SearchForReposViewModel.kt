package pl.wojtek.searchwithcoroutines

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import pl.wojtek.core.CoroutineUtils
import pl.wojtek.pagination.coroutine.CoroutinePaginModel
import pl.wojtek.searchwithcoroutines.data.Repository
import pl.wojtek.searchwithcoroutines.network.SearchResult


interface SearchForReposVM {
    fun query(text: String)
    fun loadMore()

    val listOfRepos: LiveData<List<Repository>>
    val errors: LiveData<String>
    val loading: LiveData<Boolean>
}

const val TIMEOUT_BEFORE_SEARCH = 300L

@ExperimentalCoroutinesApi
class SearchForReposViewModel(private val coroutineUtils: CoroutineUtils,
                              private val paginModel: CoroutinePaginModel<String, Repository, SearchResult>,
                              private val timeout: Long = TIMEOUT_BEFORE_SEARCH) : SearchForReposVM, ViewModel() {

    private val loadingProcessor: MutableLiveData<Boolean> = MutableLiveData()
    private val listProcessor: MutableLiveData<List<Repository>> = MutableLiveData()
    private val queryStream: ConflatedBroadcastChannel<String> = ConflatedBroadcastChannel()
    private val errorStream: MutableLiveData<String> = MutableLiveData()

    private val errorHandler = CoroutineExceptionHandler { _, throwable ->
        errorStream.postValue(throwable.message ?: "Error happens")
    }
    private var askJob: Job? = null

    init {
        viewModelScope.launch {
            queryStream.asFlow()
                .debounce(timeout)
                .distinctUntilChanged()
                .filter {
                    it.isNotEmpty()
                }
                .collect {
                    askJob?.cancel()
                    paginModel.setQuery(it)
                    loadMore()
                }
        }
        viewModelScope.launch {
            paginModel.loadingState().collect {
                loadingProcessor.postValue(it)
            }
        }

    }


    override fun query(text: String) {
        viewModelScope.launch(errorHandler) {
            queryStream.send(text)
        }
    }

    override fun loadMore() {
        askJob = viewModelScope.launch(errorHandler) {
            paginModel.askForMore()?.let {
                listProcessor.postValue(it)
            }

        }
    }

    override val listOfRepos: LiveData<List<Repository>>
        get() = listProcessor

    override val errors: LiveData<String>
        get() = errorStream

    override val loading: LiveData<Boolean>
        get() = loadingProcessor

    override fun onCleared() {
        super.onCleared()
        queryStream.close()
        paginModel.clear()
    }
}
