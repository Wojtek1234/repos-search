package pl.wojtek.ask

import com.nhaarman.mockitokotlin2.*
import com.poccofinance.core.rx.SchedulerUtils
import io.kotlintest.TestCase
import io.kotlintest.specs.StringSpec
import io.reactivex.Maybe
import io.reactivex.processors.BehaviorProcessor
import io.reactivex.schedulers.Schedulers
import io.reactivex.schedulers.TestScheduler
import pl.wojtek.ask.data.Repository
import pl.wojtek.ask.network.SearchResult
import pl.wojtek.pagination.PaginModel
import java.util.concurrent.TimeUnit

/**
 *
 */
class SearchForReposViewModelTest:StringSpec(){

    lateinit var schedulerUtils: SchedulerUtils
    lateinit var paginModel : PaginModel<String,Repository,SearchResult>

    lateinit var viewModel:SearchForReposViewModel


    lateinit var testScheduler :TestScheduler
    lateinit var debounceScheduler :TestScheduler
    lateinit var loadingProcessor :BehaviorProcessor<Boolean>
    override fun beforeTest(testCase: TestCase) {
        super.beforeTest(testCase)
        schedulerUtils = mock()
        paginModel = mock()
        testScheduler = TestScheduler()
        debounceScheduler = TestScheduler()
        loadingProcessor = BehaviorProcessor.createDefault(false)
        whenever(schedulerUtils.observScheduler).thenReturn(Schedulers.trampoline())
        whenever(schedulerUtils.subscribeScheduler).thenReturn(testScheduler)
        whenever(paginModel.loadingState()).thenReturn(loadingProcessor)

        viewModel = SearchForReposViewModel(schedulerUtils,paginModel,debounceScheduler = debounceScheduler)


    }
    init {
        "test change query in model after proper time"{
            //given
            val query = "raz dwa trzy"


            //when
            viewModel.query(query)

            debounceScheduler.advanceTimeBy(TIMEOUT_BEFORE_SEARCH,TimeUnit.MILLISECONDS)

            //then
            verify(paginModel).setQuery(query)
        }

        "test if query before debounce then only second one is set"{
            //given
            val query = "raz dwa trzy"
            val query2 = "raz dwa trzy cztery"


            //when
            viewModel.query(query)

            debounceScheduler.advanceTimeBy(TIMEOUT_BEFORE_SEARCH/2,TimeUnit.MILLISECONDS)

            viewModel.query(query2)
            debounceScheduler.advanceTimeBy(TIMEOUT_BEFORE_SEARCH,TimeUnit.MILLISECONDS)

            //then
            verify(paginModel).setQuery(query2)
        }

        "test after setting query ask for more is triggered"{
            //given
            val query = "raz dwa trzy"


            //when
            viewModel.query(query)

            debounceScheduler.advanceTimeBy(TIMEOUT_BEFORE_SEARCH,TimeUnit.MILLISECONDS)

            //then
            with(inOrder(paginModel)){
                verify(paginModel).setQuery(query)
                verify(paginModel).askForMore()
            }
        }

        "test that loading from model is passed from view model"{
            //given

            whenever(paginModel.loadingState()).thenReturn(loadingProcessor)

            //when
            val testStream = viewModel.loading().test()
            loadingProcessor.onNext(true)



            //then
            testStream.assertValues(false,true)

            //when
            loadingProcessor.onNext(false)
            testScheduler.triggerActions()
            //then

            testStream.assertValueAt(2,false)
        }

        "test that loading from view model not passing the same value twice"{
            //given

            whenever(paginModel.loadingState()).thenReturn(loadingProcessor)

            //when
            val testStream = viewModel.loading().test()
            loadingProcessor.onNext(true)


            //then
            testStream.assertValues(false,true)

            //when
            loadingProcessor.onNext(true)
            testScheduler.triggerActions()
            //then

            testStream.assertValueCount(2)
        }

        "test that ask for more from model is passed from viewModel"{
            //given
            val returnedList = listOf(Repository("repoName","url",123,"owner"))

            whenever(paginModel.askForMore()).thenReturn(Maybe.just(returnedList))

            //when
            viewModel.loadMore()
            val testStream = viewModel.listOfRepos().test()

            testScheduler.triggerActions()


            //then
            testStream.assertValue(returnedList)
        }

        "test do not pass twice the same query"{
            //given
            val query = "raz dwa trzy"


            //when
            viewModel.query(query)

            debounceScheduler.advanceTimeBy(TIMEOUT_BEFORE_SEARCH,TimeUnit.MILLISECONDS)

            viewModel.query(query)
            debounceScheduler.advanceTimeBy(TIMEOUT_BEFORE_SEARCH,TimeUnit.MILLISECONDS)

            //then
            verify(paginModel, times(1)).setQuery(query)
        }

        "test that when query changes dispose previous model call and it does not returns"{
            //given
            val returnedList = listOf(Repository("repoName","url",123,"owner"))
            val listNotToBeReturned = listOf(Repository("dsadassda","url",11111,"ownerdasd"))

            whenever(paginModel.askForMore())
                .thenReturn(Maybe.just(listNotToBeReturned))
                .thenReturn(Maybe.just(returnedList))
            val query1 = "tralala"
            val query2 = "tralala222"

            //when
            val testStream = viewModel.listOfRepos().test()

            viewModel.query(query1)
            debounceScheduler.advanceTimeBy(TIMEOUT_BEFORE_SEARCH,TimeUnit.MILLISECONDS)

            //then
            verify(paginModel).setQuery(query1)
            verify(paginModel).askForMore()



            //when
            viewModel.query(query2)
            debounceScheduler.advanceTimeBy(TIMEOUT_BEFORE_SEARCH,TimeUnit.MILLISECONDS)
            testScheduler.triggerActions()

            //then
            verify(paginModel).setQuery(query1)
            verify(paginModel, times(2)).askForMore()
            //then
            testStream.assertValue(returnedList)
        }
    }
}