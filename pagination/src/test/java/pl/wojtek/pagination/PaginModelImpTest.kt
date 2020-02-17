package pl.wojtek.pagination

import com.nhaarman.mockitokotlin2.*
import io.kotlintest.TestCase
import io.kotlintest.specs.StringSpec
import io.reactivex.Maybe
import io.reactivex.subjects.MaybeSubject

/**
 *
 */
internal class DataFromSource(val data: String)

internal class DataResult(val data: String)
class PaginModelImpTest : StringSpec() {

    private lateinit var dataSource: DataSource<String, DataFromSource>
    private lateinit var mapper: DataMapper<DataFromSource, DataResult,String>
    private lateinit var dataHolder: DataHolder<String, DataResult>
    private lateinit var queryDataHolder: QueryDataHolder<String>

    private lateinit var paginModel:PaginModel<String,DataResult,DataFromSource>
    override fun beforeTest(testCase: TestCase) {
        dataSource = mock()
        mapper = mock()
        dataHolder = mock()
        queryDataHolder = mock()

        paginModel = PaginModelImp(dataSource,mapper,dataHolder,queryDataHolder)
    }
    init {
        "test query data holder cannot as for another one returns empty"{
            //given
            whenever(queryDataHolder.canAskForAnotherOne()).thenReturn(false)

            //when
            paginModel.askForMore().test().assertNoValues().assertComplete()
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
            //given
            val query = "tralala"
            val dataFromSource = DataFromSource("data from hipotetical api")
            val dataResult = DataResult("data after mapping")
            val queryParams = QueryParams(query, 0, 20)

            val mappedData = MappedData(query, listOf(dataResult))
            whenever(dataSource.askForData(queryParams)).thenReturn(Maybe.just(dataFromSource))
            whenever(mapper.map(dataFromSource,queryParams)).thenReturn(mappedData)
            whenever(dataHolder.provideData(query,mappedData.list)).thenReturn(mappedData.list)
            whenever(queryDataHolder.canAskForAnotherOne()).thenReturn(true)

            whenever(queryDataHolder.provideQueryParams()).thenReturn(queryParams)

            //when then
            paginModel.setQuery(query)
            paginModel.askForMore().test().assertValue(mappedData.list)
        }

        "test after one data source call, before it ends, second one returns empty result"{
            //given
            val query = "tralala"
            val dataFromSource = DataFromSource("data from hipotetical api")
            val dataResult = DataResult("data after mapping")
            val singleSubject:MaybeSubject<DataFromSource> = MaybeSubject.create()
            val queryParams = QueryParams(query, 0, 20)
            val mappedData = MappedData(query, listOf(dataResult))
            whenever(dataSource.askForData(queryParams)).thenReturn(singleSubject)
            whenever(mapper.map(dataFromSource,queryParams)).thenReturn(mappedData)
            whenever(dataHolder.provideData(query,mappedData.list)).thenReturn(mappedData.list)
            whenever(queryDataHolder.canAskForAnotherOne()).thenReturn(true)

            whenever(queryDataHolder.provideQueryParams()).thenReturn(queryParams)

            //when
            paginModel.setQuery(query)
            val testFirstAsking = paginModel.askForMore().test()
            //then
            testFirstAsking.assertNotComplete()

            //when then
            paginModel.askForMore().test().assertComplete().assertNoValues()
            singleSubject.onSuccess(dataFromSource)

            //then
            testFirstAsking.assertValue(mappedData.list).assertComplete()

        }

        "test proper triggering of objects in good scenario"{
            //given
            val query = "tralala"
            val dataFromSource = DataFromSource("data from hipotetical api")
            val dataResult = DataResult("data after mapping")
            val queryParams = QueryParams(query, 0, 20)
            val mappedData = MappedData(query, listOf(dataResult),123)
            whenever(dataSource.askForData(queryParams)).thenReturn(Maybe.just(dataFromSource))
            whenever(mapper.map(dataFromSource,queryParams)).thenReturn(mappedData)
            whenever(dataHolder.provideData(query,mappedData.list)).thenReturn(mappedData.list)
            whenever(queryDataHolder.canAskForAnotherOne()).thenReturn(true)

            whenever(queryDataHolder.provideQueryParams()).thenReturn(queryParams)

            //when
            paginModel.setQuery(query)
            paginModel.askForMore().test()

            //then
            with(inOrder(dataSource,mapper,dataHolder,queryDataHolder)){
                verify(queryDataHolder).canAskForAnotherOne()
                verify(queryDataHolder).provideQueryParams()
                verify(dataSource).askForData(queryParams)
                verify(mapper).map(dataFromSource,queryParams)
                verify(queryDataHolder).setMax(query,mappedData.max)
                verify(dataHolder).provideData(query,mappedData.list)
                verify(queryDataHolder).turnToNextPage()

            }
        }

        "test when exception no turning the page on"{
            //given
            val query = "tralala"
            val queryParams = QueryParams(query, 0, 20)

            whenever(dataSource.askForData(queryParams)).thenReturn(Maybe.error(NullPointerException()))

            whenever(queryDataHolder.canAskForAnotherOne()).thenReturn(true)

            whenever(queryDataHolder.provideQueryParams()).thenReturn(queryParams)

            //when
            paginModel.setQuery(query)
            paginModel.askForMore().test()

            //then
            with(inOrder(dataSource,queryDataHolder)){
                verify(queryDataHolder).setQuery(query)
                verify(queryDataHolder).canAskForAnotherOne()
                verify(queryDataHolder).provideQueryParams()
                verify(dataSource).askForData(queryParams)
            }
            verifyNoMoreInteractions(dataHolder)
            verifyNoMoreInteractions(mapper)
            verifyNoMoreInteractions(queryDataHolder)
        }

        "test loading stream"{
            //given
            val query = "tralala"
            val dataFromSource = DataFromSource("data from hipotetical api")
            val dataResult = DataResult("data after mapping")
            val singleSubject: MaybeSubject<DataFromSource> = MaybeSubject.create()
            val queryParams = QueryParams(query, 0, 20)
            val mappedData = MappedData(query, listOf(dataResult))

            whenever(dataSource.askForData(queryParams)).thenReturn(singleSubject)
            whenever(mapper.map(dataFromSource,queryParams)).thenReturn(mappedData)
            whenever(dataHolder.provideData(query,mappedData.list)).thenReturn(mappedData.list)
            whenever(queryDataHolder.canAskForAnotherOne()).thenReturn(true)

            whenever(queryDataHolder.provideQueryParams()).thenReturn(queryParams)


            //when
            paginModel.setQuery(query)
            val testingStream = paginModel.loadingState().test()
            //then
            testingStream.assertValue(false)
            //when
            paginModel.askForMore().test()

            //then
            testingStream.assertValueAt(1,true)

            //when
            singleSubject.onSuccess(dataFromSource)

            //then
            testingStream.assertValueAt(2,false)
        }

    }
}