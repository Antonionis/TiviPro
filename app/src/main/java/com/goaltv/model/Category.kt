package com.goaltv.model

enum class CategoryType {
    LIVE,
    VOD,
    SERIES
}

data class Category(
    val id: String,
    val name: String,
    val type: CategoryType = CategoryType.LIVE
)
