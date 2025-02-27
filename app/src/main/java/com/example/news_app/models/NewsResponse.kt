package com.example.news_app.models

import androidx.room.Entity
import androidx.room.PrimaryKey

data class NewsResponse(
    val articles: MutableList<Article>,
    val status: String,
    val totalResults: Int
)

