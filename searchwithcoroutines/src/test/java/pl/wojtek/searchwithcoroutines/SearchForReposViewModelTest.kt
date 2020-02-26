package pl.wojtek.searchwithcoroutines

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.*
import io.kotlintest.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import pl.wojtek.pagination.coroutine.CoroutinePaginModel
import pl.wojtek.searchwithcoroutines.data.Repository
import pl.wojtek.searchwithcoroutines.network.SearchResult


/**
 *
 */
@ExperimentalCoroutinesApi
class SearchForReposViewModelTest {
    @get:Rule
    val coroutineScope = MainCoroutineScopeRule()
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()
    lateinit var paginModel: CoroutinePaginModel<String, Repository, SearchResult>

    lateinit var viewModel: SearchForReposViewModel
    private lateinit var queryStream: ConflatedBroadcastChannel<Boolean>
    val TIME_DEBOUNCE = 100L

    @Before
    fun setup() {

        queryStream = ConflatedBroadcastChannel()
        paginModel = mock()
        whenever(paginModel.loadingState()).thenReturn(queryStream.asFlow())
        viewModel = SearchForReposViewModel(paginModel, TIME_DEBOUNCE)
    }

    @Test
    fun testSetQueryInPaginModelAfterDebounceTime() {
        runBlockingTest {
            //given
            val query = "raz dwa trzy"


            //when
            viewModel.query(query)

            coroutineScope.advanceTimeBy(TIME_DEBOUNCE + 1)

            //then
            verify(paginModel).setQuery(query)
        }
    }

    @Test
    fun test_that_query_before_debounce_are_not_pass_to_model() {
        runBlockingTest {
            //given
            val query = "raz dwa trzy"
            val query2 = "raz dwa trzy cztery"


            //when
            viewModel.query(query)

            coroutineScope.advanceTimeBy(TIME_DEBOUNCE / 2)

            viewModel.query(query2)
            coroutineScope.advanceTimeBy(TIME_DEBOUNCE + 1)

            //then
            verify(paginModel, never()).setQuery(query)
            verify(paginModel).setQuery(query2)
        }
    }

    @Test
    fun test_that_after_query_is_set_ask_for_more_method_of_model_is_triggered() {
        runBlockingTest {
            //given
            val query = "raz dwa trzy"


            //when
            viewModel.query(query)

            coroutineScope.advanceTimeBy(TIME_DEBOUNCE + 1)

            //then
            with(inOrder(paginModel)) {
                verify(paginModel).setQuery(query)
                verify(paginModel).askForMore()
            }
        }
    }

    @Test
    fun test_loading_state_from_model_is_passed_to_view_model_properly() {
        runBlockingTest {
            //given

            val mutableListOfLoadingState = mutableListOf<Boolean>()

            //when
            viewModel.loading.observeForever { mutableListOfLoadingState.add(it) }

            queryStream.send(true)
            queryStream.send(true)
            queryStream.send(false)

            //then
            mutableListOfLoadingState.toList() shouldBe listOf(true, false)
        }
    }

    @Test
    fun test_that_value_return_from_pagin_model_is_passed_from_viewModel() {
        runBlockingTest {
            //given
            val returnedList = listOf(Repository("repoName", "url", 123, "owner"))

            whenever(paginModel.askForMore()).thenReturn(returnedList)

            //when
            viewModel.loadMore()
            viewModel.listOfRepos.observeForever { }


            //then
            viewModel.listOfRepos.value shouldBe returnedList
        }
    }

    @Test
    fun test_do_not_pass_twice_the_same_query() {
        runBlockingTest {
            //given
            val query = "raz dwa trzy"


            //when
            viewModel.query(query)

            coroutineScope.advanceTimeBy(TIME_DEBOUNCE)

            viewModel.query(query)
            coroutineScope.advanceTimeBy(TIME_DEBOUNCE)

            //then
            verify(paginModel, times(1)).setQuery(query)

        }
    }

}









