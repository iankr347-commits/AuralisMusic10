// Private Test Build  Not for Redistribution
package com.auralis.innertube.models.response
 
 import com.auralis.innertube.models.MusicShelfRenderer
 import kotlinx.serialization.Serializable
 
 @Serializable
 data class ContinuationResponse(
     val onResponseReceivedActions: List<ResponseAction>?,
 ) {
 
     @Serializable
     data class ResponseAction(
         val appendContinuationItemsAction: ContinuationItems?,
     )
 
     @Serializable
     data class ContinuationItems(
         val continuationItems: List<MusicShelfRenderer.Content>?,
     )
 }
