package com.example.news_app.viewModel

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.news_app.models.Article
import com.example.news_app.models.NewsResponse
import com.example.news_app.repository.NewsRepository
import com.example.news_app.utils.Resources
import kotlinx.coroutines.launch
import retrofit2.Response
import java.io.IOException

class NewsViewModel(app: Application, val newsRepository: NewsRepository) : AndroidViewModel(app) {

    // LiveData to hold top headlines response
    val headline: MutableLiveData<Resources<NewsResponse>> = MutableLiveData()
    var headlinesPage = 1  // Tracks the current page for headlines
    var headlineResponse: NewsResponse? = null  // Cached headlines response for pagination

    // LiveData to hold search news response
    val searchNews: MutableLiveData<Resources<NewsResponse>> = MutableLiveData()
    var searchNewsPage = 1  // Tracks the current page for search results
    var searchNewssResponse: NewsResponse? = null  // Cached search results for pagination
    var newSearchQuery: String? = null  // Tracks the new search query
    var oldSearchQuery: String? = null  // Tracks the previous search query

    init {
        // Fetch initial headlines for the US on ViewModel initialization
        getHeadline("us")
    }

    // Fetches headlines for the specified country code
    fun getHeadline(countryCode: String) = viewModelScope.launch {
        headlineInternate(countryCode)
    }

    // Searches for news articles based on the provided search query
    fun searchNews(searchQuery: String) = viewModelScope.launch {
        searchNewsInternate(searchQuery)
    }

    // Handles the API response for top headlines
    private fun handleHeadlinesResponse(response: Response<NewsResponse>): Resources<NewsResponse> {
        if (response.isSuccessful) {
            response.body()?.let { resultResponse ->
                headlinesPage++  // Increment page number for pagination
                if (headlineResponse == null) {
                    headlineResponse = resultResponse  // Set initial response
                } else {
                    val oldArticles = headlineResponse?.articles
                    val newArticles = resultResponse.articles
                    oldArticles?.addAll(newArticles)  // Combine old and new articles
                }
                return Resources.Success(headlineResponse ?: resultResponse)
            }
        }
        return Resources.Error(response.message())  // Return error if response is not successful
    }

    // Handles the API response for searched news articles
    private fun handleSearchNewsResponse(response: Response<NewsResponse>): Resources<NewsResponse> {
        if (response.isSuccessful) {
            response.body()?.let { resultResponse ->
                if (searchNewssResponse == null || newSearchQuery != oldSearchQuery) {
                    searchNewsPage = 1  // Reset page number for a new search
                    oldSearchQuery = newSearchQuery  // Update old search query
                    searchNewssResponse = resultResponse  // Set initial search response
                } else {
                    searchNewsPage++  // Increment page number for pagination
                    val oldArticles = searchNewssResponse?.articles
                    val newArticles = resultResponse.articles
                    oldArticles?.addAll(newArticles)  // Combine old and new articles
                }
                return Resources.Success(searchNewssResponse ?: resultResponse)
            }
        }
        return Resources.Error(response.message())  // Return error if response is not successful
    }

    // Adds an article to the favorites list in the database
    fun addToFavourites(article: Article) = viewModelScope.launch {
        newsRepository.upsert(article)
    }

    // Retrieves all favorite articles from the database
    fun getFavouriteNews() = newsRepository.getFavouriteNews()

    // Deletes a specific article from the favorites list in the database
    fun deleteArticles(article: Article) = viewModelScope.launch {
        newsRepository.deleteArticle(article)
    }

    // Checks if there is an active internet connection
    fun internateConnections(context: Context): Boolean {
        (context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).apply {
            return getNetworkCapabilities(activeNetwork)?.run {
                when {
                    hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                    hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                    hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                    else -> false
                }
            } ?: false
        }
    }

    // Fetches top headlines from the API while handling internet connectivity and errors
    private suspend fun headlineInternate(countryCode: String) {
        headline.postValue(Resources.Loading())  // Set loading state
        try {
            if (internateConnections(this.getApplication())) {
                val response = newsRepository.getHeadLines(countryCode, headlinesPage)
                headline.postValue(handleHeadlinesResponse(response))  // Post successful or error response
            } else {
                headline.postValue(Resources.Error("No internet connection"))  // Post error for no connection
            }
        } catch (t: Throwable) {
            when (t) {
                is IOException -> headline.postValue(Resources.Error("Unable to connect"))
                else -> headline.postValue(Resources.Error("No signal"))
            }
        }
    }

    // Fetches search results from the API while handling internet connectivity and errors
    private suspend fun searchNewsInternate(searchQuery: String) {
        newSearchQuery = searchQuery  // Update the current search query
        searchNews.postValue(Resources.Loading())  // Set loading state
        try {
            if (internateConnections(this.getApplication())) {
                val response = newsRepository.searchNews(searchQuery, searchNewsPage)
                searchNews.postValue(handleSearchNewsResponse(response))  // Post successful or error response
            } else {
                searchNews.postValue(Resources.Error("No internet connection"))  // Post error for no connection
            }
        } catch (t: Throwable) {
            when (t) {
                is IOException -> searchNews.postValue(Resources.Error("Unable to connect"))
                else -> searchNews.postValue(Resources.Error("No signal"))
            }
        }
    }
}
