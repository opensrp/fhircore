package org.smartregister.fhircore.engine.data.remote.fhir.resource


import androidx.paging.PagingSource
import androidx.paging.PagingState
import org.hl7.fhir.r4.model.Bundle
class FhirResourcePagingSource (private val basePath: String, private val gatewayModeHeaderValue: String, private val fhirResourceDataSource: FhirResourceDataSource)
    : PagingSource<Int,Bundle.BundleEntryComponent>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Bundle.BundleEntryComponent> {
        return try {
            val currentPage = params.key ?: 1
            val response = fhirResourceDataSource.getResourceWithGatewayModeHeader(
                gatewayModeHeaderValue,
                ("$basePath&_page=$currentPage&_count=200")
            )
            val data = response.entry
            val prevKey = if (currentPage > 1) currentPage - 1 else null
            val nextKey = if (data.isNotEmpty()) currentPage + 1 else null
            LoadResult.Page(data = data, prevKey = prevKey, nextKey = nextKey)
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Bundle.BundleEntryComponent>): Int? {
        TODO("Not yet implemented")
    }
}