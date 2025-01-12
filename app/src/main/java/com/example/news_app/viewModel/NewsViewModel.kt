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

    val headline: MutableLiveData<Resources<NewsResponse>> = MutableLiveData()
    var headlinesPage = 1
    var headlineResponse: NewsResponse? = null

    val searchNews: MutableLiveData<Resources<NewsResponse>> = MutableLiveData()
    var searchNewsPage = 1
    var searchNewssResponse: NewsResponse? = null
    var newSearchQuery: String? = null
    var oldSearchQuery: String? = null


    init {
        getHeadline("us")
    }

    fun getHeadline(countryCode: String) = viewModelScope.launch {
        headlineInternate(countryCode)
    }

    fun searchNews(searchQuery: String) = viewModelScope.launch {
        searchNewsInternate(searchQuery)
    }

    private fun handleHeadlinesResponse(response: Response<NewsResponse>): Resources<NewsResponse> {
        if (response.isSuccessful) {
            response.body()?.let { resultResponse ->
                headlinesPage++
                if (headlineResponse == null) {
                    headlineResponse == resultResponse
                } else {
                    val oldArticles = headlineResponse?.articles
                    val newArticles = resultResponse.articles
                    oldArticles?.addAll(newArticles)
                }
                return Resources.Success(headlineResponse ?: resultResponse)
            }
        }
        return Resources.Error(response.message())
    }


    private fun handleSearchNewsResponse(response: Response<NewsResponse>): Resources<NewsResponse> {
        if (response.isSuccessful) {
            response.body()?.let { resultResponse ->
                if (searchNewssResponse == null || newSearchQuery != oldSearchQuery) {
                    searchNewsPage = 1
                    oldSearchQuery = newSearchQuery
                    searchNewssResponse = resultResponse
                } else {
                    searchNewsPage++
                    val oldArticles = searchNewssResponse?.articles
                    val newArticles = resultResponse.articles
                    oldArticles?.addAll(newArticles)
                }
                return Resources.Success(searchNewssResponse ?: resultResponse)
            }
        }
        return Resources.Error(response.message())
    }

    fun addToFavourites(article: Article) = viewModelScope.launch {
        newsRepository.upsert(article)
    }

    fun getFavouriteNews() = newsRepository.getFavouriteNews()

    fun deleteArticles(article: Article) = viewModelScope.launch {
        newsRepository.deleteArticle(article)
    }

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


    private suspend fun headlineInternate(countryCode: String) {
        headline.postValue(Resources.Loading())
        try {
            if (internateConnections(this.getApplication())) {
                val response = newsRepository.getHeadLines(countryCode, headlinesPage)
                headline.postValue(handleHeadlinesResponse(response))
            } else {
                headline.postValue(Resources.Error("No internate connection "))
            }
        } catch (t: Throwable) {
            when (t) {
                is IOException -> headline.postValue(Resources.Error("Unable to connect "))
                else -> headline.postValue(Resources.Error("No signal"))
            }

        }
    }


    private suspend fun searchNewsInternate(searchQuery: String) {
        newSearchQuery = searchQuery
        searchNews.postValue(Resources.Loading())
        try {
            if (internateConnections(this.getApplication())) {
                val response = newsRepository.searchNews(searchQuery, searchNewsPage)
                searchNews.postValue(handleSearchNewsResponse(response))
            } else {
                searchNews.postValue(Resources.Error("No internate connection"))
            }
        } catch (t: Throwable) {
            when (t) {
                is IOException -> searchNews.postValue(Resources.Error("Unable to connect"))
                else -> searchNews.postValue(Resources.Error("No signal"))
            }
        }
    }

}