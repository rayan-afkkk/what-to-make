package com.whattocook.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.whattocook.app.R
import com.whattocook.app.domain.model.IngredientCategory

@Composable
fun categoryLabel(category: IngredientCategory): String = stringResource(
    when (category) {
        IngredientCategory.PRODUCE -> R.string.category_produce
        IngredientCategory.HERBS -> R.string.category_herbs
        IngredientCategory.DAIRY -> R.string.category_dairy
        IngredientCategory.MEAT -> R.string.category_meat
        IngredientCategory.GRAINS -> R.string.category_grains
        IngredientCategory.LEGUMES -> R.string.category_legumes
        IngredientCategory.SPICES -> R.string.category_spices
        IngredientCategory.CONDIMENTS -> R.string.category_condiments
        IngredientCategory.BAKING -> R.string.category_baking
        IngredientCategory.OILS -> R.string.category_oils
        IngredientCategory.NUTS -> R.string.category_nuts
        IngredientCategory.FROZEN -> R.string.category_frozen
        IngredientCategory.OTHER -> R.string.category_other
    },
)
