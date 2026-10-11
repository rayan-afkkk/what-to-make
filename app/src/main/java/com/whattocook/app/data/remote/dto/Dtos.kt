package com.whattocook.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class RecipeRequestDto(
    val ingredients: List<String>,
    val diet: String? = null,
    val timeMinutes: Int? = null,
    val equipment: List<String>? = null,
    val cuisine: String? = null,
    val allowMissing: Int,
)

@Serializable
data class RecipeResponseDto(
    val recipes: List<RecipeDto> = emptyList(),
    val usage: UsageDto? = null,
)

@Serializable
data class RecipeDto(
    val id: String,
    val title: String,
    val summary: String = "",
    val timeMinutes: Int = 0,
    val servings: Int = 2,
    val ingredients: List<IngredientDto> = emptyList(),
    val steps: List<String> = emptyList(),
    val missing: List<String> = emptyList(),
    val substitutions: List<SubstitutionDto> = emptyList(),
)

@Serializable
data class IngredientDto(
    val name: String,
    val quantity: String = "",
    val unit: String = "",
    val inPantry: Boolean = false,
)

@Serializable
data class SubstitutionDto(
    val missing: String,
    val swap: String,
)

@Serializable
data class UsageDto(
    val used: Int,
    val limit: Int,
    val resetsAt: Long,
)

@Serializable
data class UsageResponseDto(
    val plan: String = "free",
    val used: Int,
    val limit: Int,
    val resetsAt: Long,
)

@Serializable
data class LimitReachedDto(
    val error: String = "",
    val resetsAt: Long = 0L,
)

@Serializable
data class VerifyPurchaseRequestDto(
    val purchaseToken: String,
    val productId: String,
)

@Serializable
data class VerifyPurchaseResponseDto(
    val plan: String,
    val expiresAt: Long = 0L,
)
