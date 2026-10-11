package com.whattocook.app.data.remote

import com.whattocook.app.data.remote.dto.RecipeDto
import com.whattocook.app.data.remote.dto.RecipeRequestDto
import com.whattocook.app.domain.model.Recipe
import com.whattocook.app.domain.model.RecipeIngredient
import com.whattocook.app.domain.model.RecipeRequest
import com.whattocook.app.domain.model.Substitution
import java.util.UUID

fun RecipeRequest.toDto() = RecipeRequestDto(
    ingredients = ingredients,
    diet = diet,
    timeMinutes = timeMinutes,
    equipment = equipment,
    cuisine = cuisine,
    allowMissing = allowMissing,
)

/**
 * Local ids are generated on the device so a backend that reuses ids between
 * requests can never overwrite earlier recipes in the history.
 */
fun RecipeDto.toDomain(createdAt: Long) = Recipe(
    id = UUID.randomUUID().toString(),
    title = title,
    summary = summary,
    timeMinutes = timeMinutes,
    servings = servings.coerceAtLeast(1),
    ingredients = ingredients.map { RecipeIngredient(it.name, it.quantity, it.unit, it.inPantry) },
    steps = steps,
    missing = missing,
    substitutions = substitutions.map { Substitution(it.missing, it.swap) },
    createdAt = createdAt,
    isSaved = false,
)
