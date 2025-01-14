package com.example.news_app.repository

import com.example.news_app.api.RetrofitInstance
import com.example.news_app.db.ArticleDatabase
import com.example.news_app.models.Article
import java.util.Locale.IsoCountryCode

class NewsRepository(val db: ArticleDatabase) {



    suspend fun getHeadLines(countryCode: String, pageNumber: Int) =
        RetrofitInstance.api.getHeadlines(countryCode, pageNumber)

    suspend fun searchNews(searchQuery: String, pageNumber: Int) =
        RetrofitInstance.api.searchForNews(searchQuery, pageNumber)

    suspend fun upsert(article: Article) = db.getArticleDao().upsert(article)

    fun getFavouriteNews() = db.getArticleDao().getAllArticles()

    suspend fun deleteArticle(article: Article) = db.getArticleDao().deleteArticle(article)
}


