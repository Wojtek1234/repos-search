package pl.wojtek.searchwithcoroutines.data

/**
 *
 */


data class Repository(val repositoryName:String,val urlToRepo:String,
                      val score:Int, val ownerName:String, val id:Int=0 )