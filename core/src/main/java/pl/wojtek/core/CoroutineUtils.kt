package pl.wojtek.core

import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

/**
 *
 */


interface CoroutineUtils{
    val main:CoroutineContext
    val io:CoroutineContext
    val computation:CoroutineContext
    val globalScope:CoroutineScope
}
@UseExperimental(ObsoleteCoroutinesApi::class)
object CoroutineUtilsObj:CoroutineUtils{
    override val main: CoroutineContext= Dispatchers.Main
    override val io: CoroutineContext = Dispatchers.IO
    override val computation: CoroutineContext = newFixedThreadPoolContext(4,"Computation dispatcher")
    override val globalScope: CoroutineScope = GlobalScope
}