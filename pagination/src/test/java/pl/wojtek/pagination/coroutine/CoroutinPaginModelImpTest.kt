package pl.wojtek.pagination.coroutine

import com.nhaarman.mockitokotlin2.*
import io.kotlintest.TestCase
import io.kotlintest.matchers.collections.shouldHaveSize
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import io.kotlintest.specs.StringSpec
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
import pl.wojtek.pagination.*

/**
 *
 */
@InternalCoroutinesApi
class CoroutinPaginModelImpTest : StringSpec() {
    private lateinit var dataSource: CoroutineDataSource<String, DataFromSource>
    private lateinit var mapper: DataMapper<DataFromSource, DataResult, String>
    private lateinit var dataHolder: DataHolder<String, DataResult>
    private lateinit var queryDataHolder: QueryDataHolder<String>


    private lateinit var paginModel: CoroutinePaginModel<String, DataResult, DataFromSource>


    override fun beforeTest(testCase: TestCase) {
        dataSource = mock()
        mapper = mock()
        dataHolder = mock()
        queryDataHolder = mock()

        paginModel = CoroutinPaginModelImp(dataSource, mapper, dataHolder, queryDataHolder)
    }

    init {
        "test query data holder cannot as for another one returns empty"{
            runBlockingTest {
                //given
                whenever(queryDataHolder.canAskForAnotherOne()).thenReturn(false)

                //when

                paginModel.askForMore() shouldBe null
            }

        }

        "test setting query triggers method of queryDataHolder"{
            //given
            val query = "tralala"

            //when
            paginModel.setQuery(query)

            //then
            verify(queryDataHolder).setQuery(query)
        }

        "test all is good scenario"{
            runBlockingTest {
                //given
                val query = "tralala"
                val dataFromSource = DataFromSource("data from hipotetical api")
                val dataResult = DataResult("data after mapping")
                val queryParams = QueryParams(query, 0, 20)

                val mappedData = MappedData(query, listOf(dataResult))
                whenever(dataSource.askForData(queryParams)).thenReturn(dataFromSource)
                whenever(mapper.map(dataFromSource, queryParams)).thenReturn(mappedData)
                whenever(dataHolder.provideData(query, mappedData.list)).thenReturn(mappedData.list)
                whenever(queryDataHolder.canAskForAnotherOne()).thenReturn(true)

                whenever(queryDataHolder.provideQueryParams()).thenReturn(queryParams)

                //when then

                paginModel.setQuery(query)
                val askForMore = paginModel.askForMore()
                askForMore shouldBe mappedData.list

            }

        }

        "test proper triggering of objects in good scenario"{
            runBlockingTest {
                //given
                val query = "tralala"
                val dataFromSource = DataFromSource("data from hipotetical api")
                val dataResult = DataResult("data after mapping")
                val queryParams = QueryParams(query, 0, 20)
                val mappedData = MappedData(query, listOf(dataResult), 123)
                whenever(dataSource.askForData(queryParams)).thenReturn(dataFromSource)
                whenever(mapper.map(dataFromSource, queryParams)).thenReturn(mappedData)
                whenever(dataHolder.provideData(query, mappedData.list)).thenReturn(mappedData.list)
                whenever(queryDataHolder.canAskForAnotherOne()).thenReturn(true)

                whenever(queryDataHolder.provideQueryParams()).thenReturn(queryParams)

                //when
                paginModel.setQuery(query)
                val tested = paginModel.askForMore()

                //then
                with(inOrder(dataSource, mapper, dataHolder, queryDataHolder)) {
                    verify(queryDataHolder).canAskForAnotherOne()
                    verify(queryDataHolder).provideQueryParams()
                    verify(dataSource).askForData(queryParams)
                    verify(mapper).map(dataFromSource, queryParams)
                    verify(queryDataHolder).turnToNextPage()
                    verify(queryDataHolder).setMax(query, mappedData.max)
                    verify(dataHolder).provideData(query, mappedData.list)

                }
            }
        }

        "test when exception no turning the page on"{
            runBlockingTest {
                //given
                val query = "tralala"
                val queryParams = QueryParams(query, 0, 20)

                whenever(dataSource.askForData(queryParams)).thenThrow(NullPointerException())

                whenever(queryDataHolder.canAskForAnotherOne()).thenReturn(true)

                whenever(queryDataHolder.provideQueryParams()).thenReturn(queryParams)

                //when
                paginModel.setQuery(query)
                try {
                    paginModel.askForMore()
                } catch (ex: java.lang.NullPointerException) {

                } finally {
                    //then
                    with(inOrder(dataSource, queryDataHolder)) {
                        verify(queryDataHolder).setQuery(query)
                        verify(queryDataHolder).canAskForAnotherOne()
                        verify(queryDataHolder).provideQueryParams()
                        verify(dataSource).askForData(queryParams)
                    }
                    verifyNoMoreInteractions(dataHolder)
                    verifyNoMoreInteractions(mapper)
                    verifyNoMoreInteractions(queryDataHolder)
                }
            }
        }

        "test loading stream"{
            runBlockingTest {
                //given
                val query = "tralala"
                val dataFromSource = DataFromSource("data from hipotetical api")
                val dataResult = DataResult("data after mapping")

                val queryParams = QueryParams(query, 0, 20)
                val mappedData = MappedData(query, listOf(dataResult))

                whenever(dataSource.askForData(queryParams)).thenReturn(dataFromSource)
                whenever(mapper.map(dataFromSource, queryParams)).thenReturn(mappedData)
                whenever(dataHolder.provideData(query, mappedData.list)).thenReturn(mappedData.list)
                whenever(queryDataHolder.canAskForAnotherOne()).thenReturn(true)

                whenever(queryDataHolder.provideQueryParams()).thenReturn(queryParams)

                val values = mutableListOf<Boolean>()
                //when
                paginModel.setQuery(query)
                val job = launch {
                    paginModel.loadingState().collect {
                        values.add(it)
                    }
                }
                //then
                values shouldHaveSize 1
                values[0] shouldBe false
                //when
                paginModel.askForMore()

                //then
                values shouldHaveSize 3
                values[1] shouldBe true
                values[2] shouldBe false


                job.cancel()
            }
        }

        "test on api exception throw exception"{
            runBlockingTest {
                //given
                val query = "tralala"
                val dataFromSource = DataFromSource("data from hipotetical api")

                val queryParams = QueryParams(query, 0, 20)
                whenever(dataSource.askForData(queryParams)).thenThrow(java.lang.NullPointerException())

                whenever(queryDataHolder.canAskForAnotherOne()).thenReturn(true)

                whenever(queryDataHolder.provideQueryParams()).thenReturn(queryParams)

                //when then
                paginModel.setQuery(query)
                shouldThrow<java.lang.NullPointerException> {
                    paginModel.askForMore()
                }


                //then
                with(inOrder(dataSource, mapper, dataHolder, queryDataHolder)) {
                    verify(queryDataHolder).canAskForAnotherOne()
                    verify(queryDataHolder).provideQueryParams()
                    verify(dataSource).askForData(queryParams)
                }
            }
        }

        "test that after exception loading stream send false"{
            runBlockingTest {

                //given
                val query = "tralala"


                val queryParams = QueryParams(query, 0, 20)
                whenever(dataSource.askForData(queryParams)).thenThrow(java.lang.NullPointerException())

                whenever(queryDataHolder.canAskForAnotherOne()).thenReturn(true)

                whenever(queryDataHolder.provideQueryParams()).thenReturn(queryParams)

                val values = mutableListOf<Boolean>()

                //when then
                paginModel.setQuery(query)
                val job = launch {
                    paginModel.loadingState().collect {
                        values.add(it)
                    }
                }
                //then
                values shouldHaveSize 1
                values[0] shouldBe false
                //when
                shouldThrow<java.lang.NullPointerException> {
                    paginModel.askForMore()
                }
                //then
                values shouldHaveSize 3
                values[1] shouldBe true
                values[2] shouldBe false


                job.cancel()
            }
        }

    }


}