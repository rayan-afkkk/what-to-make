package com.whattocook.app.data.repository

import com.whattocook.app.domain.IngredientMatcher
import com.whattocook.app.domain.model.Recipe
import com.whattocook.app.domain.model.RecipeIngredient
import com.whattocook.app.domain.model.RecipeRequest
import com.whattocook.app.domain.model.Substitution

/** Believable sample recipes so every screen can be exercised before the backend exists. */
internal object FakeRecipes {

    private data class Template(
        val title: String,
        val summary: String,
        val timeMinutes: Int,
        val servings: Int,
        val cuisine: String,
        val tags: Set<String>,
        val ingredients: List<Triple<String, String, String>>,
        val steps: List<String>,
        val substitutions: List<Substitution>,
    )

    /** Things most kitchens have; never reported as missing. */
    private val STAPLES = setOf("salt", "water")

    private val templates = listOf(
        Template(
            title = "Egg Fried Rice",
            summary = "Day-old rice, scrambled egg and soy, tossed hot and fast in one pan.",
            timeMinutes = 20, servings = 2, cuisine = "East Asian",
            tags = setOf("vegetarian", "budget-friendly", "one pan", "quick", "kid-friendly"),
            ingredients = listOf(
                Triple("Cooked rice", "2", "cups"),
                Triple("Eggs", "3", ""),
                Triple("Onion", "1", "small"),
                Triple("Garlic", "2", "cloves"),
                Triple("Frozen peas", "1/2", "cup"),
                Triple("Soy sauce", "2", "tbsp"),
                Triple("Spring onions", "2", ""),
                Triple("Vegetable oil", "2", "tbsp"),
            ),
            steps = listOf(
                "Heat the oil in a wide pan or wok over high heat.",
                "Fry the chopped onion and garlic for 2 minutes until fragrant.",
                "Push everything to the side, crack in the eggs and scramble for 1 minute.",
                "Add the rice and peas and stir-fry for 4 minutes, breaking up any clumps.",
                "Splash in the soy sauce, toss well and finish with sliced spring onions.",
            ),
            substitutions = listOf(
                Substitution("Spring onions", "Finely sliced onion or chives"),
                Substitution("Frozen peas", "Any chopped vegetables you have"),
            ),
        ),
        Template(
            title = "Masoor Dal Tadka",
            summary = "Silky red lentils finished with a sizzling garlic and cumin tadka.",
            timeMinutes = 35, servings = 4, cuisine = "Desi (Pakistani / North Indian)",
            tags = setOf("vegetarian", "budget-friendly", "comfort food", "light and healthy"),
            ingredients = listOf(
                Triple("Masoor dal (red lentils)", "1", "cup"),
                Triple("Onion", "1", ""),
                Triple("Tomatoes", "2", ""),
                Triple("Garlic", "4", "cloves"),
                Triple("Ginger", "1", "inch"),
                Triple("Turmeric", "1/2", "tsp"),
                Triple("Cumin seeds", "1", "tsp"),
                Triple("Red chili powder", "1", "tsp"),
                Triple("Ghee", "2", "tbsp"),
                Triple("Coriander leaves", "1", "handful"),
                Triple("Salt", "1", "tsp"),
            ),
            steps = listOf(
                "Rinse the lentils until the water runs clear.",
                "Simmer the lentils with 3 cups of water, turmeric and salt for 20 minutes until soft.",
                "Meanwhile fry the onion in half the ghee for 6 minutes until golden, then add ginger and tomatoes.",
                "Cook the masala for 5 minutes until the tomatoes break down, then stir it into the dal.",
                "Heat the rest of the ghee, add cumin seeds, sliced garlic and chili powder for 30 seconds.",
                "Pour the sizzling tadka over the dal and finish with coriander.",
            ),
            substitutions = listOf(
                Substitution("Ghee", "Butter or any neutral oil"),
                Substitution("Masoor dal (red lentils)", "Moong dal, cooking time is similar"),
            ),
        ),
        Template(
            title = "Weeknight Shakshuka",
            summary = "Eggs gently poached in a smoky, spiced tomato and pepper sauce.",
            timeMinutes = 25, servings = 2, cuisine = "Mediterranean",
            tags = setOf("vegetarian", "one pan", "breakfast", "high-protein"),
            ingredients = listOf(
                Triple("Eggs", "4", ""),
                Triple("Tomatoes", "4", ""),
                Triple("Onion", "1", ""),
                Triple("Bell pepper", "1", ""),
                Triple("Garlic", "3", "cloves"),
                Triple("Ground cumin", "1", "tsp"),
                Triple("Paprika", "1", "tsp"),
                Triple("Olive oil", "2", "tbsp"),
                Triple("Feta cheese", "50", "g"),
                Triple("Parsley", "1", "handful"),
            ),
            steps = listOf(
                "Warm the olive oil in a deep pan and soften the onion and pepper for 6 minutes.",
                "Add garlic, cumin and paprika and stir for 1 minute.",
                "Add the chopped tomatoes and simmer for 10 minutes until thick and jammy.",
                "Make four wells, crack in the eggs, cover and cook for 6 minutes.",
                "Scatter over feta and parsley and serve straight from the pan.",
            ),
            substitutions = listOf(
                Substitution("Feta cheese", "Crumbled paneer or a little yogurt"),
                Substitution("Bell pepper", "A green chili for heat instead"),
            ),
        ),
        Template(
            title = "Chana Masala",
            summary = "Chickpeas in a tangy onion-tomato gravy with warm spices.",
            timeMinutes = 30, servings = 3, cuisine = "Desi (Pakistani / North Indian)",
            tags = setOf("vegetarian", "vegan", "high-protein", "budget-friendly", "spicy", "comfort food"),
            ingredients = listOf(
                Triple("Chickpeas", "800", "g"),
                Triple("Onions", "2", ""),
                Triple("Tomatoes", "3", ""),
                Triple("Ginger garlic paste", "1", "tbsp"),
                Triple("Garam masala", "1 1/2", "tsp"),
                Triple("Coriander powder", "1", "tsp"),
                Triple("Turmeric", "1/2", "tsp"),
                Triple("Red chili powder", "1", "tsp"),
                Triple("Vegetable oil", "3", "tbsp"),
                Triple("Lemon", "1/2", ""),
                Triple("Coriander leaves", "1", "handful"),
            ),
            steps = listOf(
                "Fry the finely chopped onions in oil for 10 minutes until deep golden.",
                "Add ginger garlic paste and cook for 1 minute.",
                "Stir in the tomatoes and all the ground spices; cook for 6 minutes until the oil separates.",
                "Add the drained chickpeas and a cup of water, mash a few, and simmer for 10 minutes.",
                "Finish with lemon juice and coriander.",
            ),
            substitutions = listOf(
                Substitution("Lemon", "1/2 tsp amchur (dry mango powder)"),
                Substitution("Ginger garlic paste", "Grated fresh ginger and garlic"),
            ),
        ),
        Template(
            title = "Creamy Tomato Pasta",
            summary = "A quick, silky tomato sauce with garlic, cream and plenty of parmesan.",
            timeMinutes = 20, servings = 2, cuisine = "Italian",
            tags = setOf("vegetarian", "quick", "kid-friendly", "comfort food"),
            ingredients = listOf(
                Triple("Pasta", "200", "g"),
                Triple("Tomatoes", "3", ""),
                Triple("Garlic", "3", "cloves"),
                Triple("Cream", "1/2", "cup"),
                Triple("Parmesan", "30", "g"),
                Triple("Olive oil", "1", "tbsp"),
                Triple("Basil", "1", "handful"),
                Triple("Chili flakes", "1/2", "tsp"),
                Triple("Salt", "1", "tsp"),
            ),
            steps = listOf(
                "Boil the pasta in well-salted water for 10 minutes, saving a mug of pasta water.",
                "Meanwhile gently fry the garlic and chili flakes in olive oil for 1 minute.",
                "Add the grated tomatoes and simmer for 5 minutes.",
                "Stir in the cream and parmesan, then toss through the pasta with a splash of pasta water.",
                "Tear over the basil and serve.",
            ),
            substitutions = listOf(
                Substitution("Cream", "Milk with 1 tsp flour whisked in"),
                Substitution("Parmesan", "Any hard cheese, such as cheddar"),
            ),
        ),
        Template(
            title = "Chicken Karahi",
            summary = "Restaurant-style karahi: tomatoes, ginger and black pepper cooked down until glossy.",
            timeMinutes = 40, servings = 4, cuisine = "Desi (Pakistani / North Indian)",
            tags = setOf("high-protein", "spicy", "one pan", "comfort food"),
            ingredients = listOf(
                Triple("Chicken", "750", "g"),
                Triple("Tomatoes", "5", ""),
                Triple("Ginger", "2", "inch"),
                Triple("Garlic", "6", "cloves"),
                Triple("Green chilies", "4", ""),
                Triple("Yogurt", "1/2", "cup"),
                Triple("Vegetable oil", "1/4", "cup"),
                Triple("Black pepper", "1", "tsp"),
                Triple("Cumin seeds", "1", "tsp"),
                Triple("Salt", "1 1/2", "tsp"),
                Triple("Coriander leaves", "1", "handful"),
            ),
            steps = listOf(
                "Heat the oil in a karahi or wok and sear the chicken for 5 minutes.",
                "Add crushed garlic and half the ginger and cook for 2 minutes.",
                "Add the halved tomatoes, cover and cook for 8 minutes, then peel away the skins.",
                "Cook uncovered on high heat for 10 minutes, mashing the tomatoes into a thick masala.",
                "Stir in yogurt, black pepper and cumin and cook for 5 minutes until the oil rises.",
                "Finish with green chilies, the remaining ginger in thin strips, and coriander.",
            ),
            substitutions = listOf(
                Substitution("Yogurt", "A squeeze of lemon juice"),
                Substitution("Green chilies", "1/2 tsp chili flakes"),
            ),
        ),
        Template(
            title = "Lemon Garlic Chickpea Salad",
            summary = "Crunchy, bright and filling, ready in ten minutes with no cooking.",
            timeMinutes = 10, servings = 2, cuisine = "Mediterranean",
            tags = setOf("vegetarian", "vegan", "quick", "light and healthy", "high-protein"),
            ingredients = listOf(
                Triple("Chickpeas", "400", "g"),
                Triple("Cucumber", "1", ""),
                Triple("Tomatoes", "2", ""),
                Triple("Red onion", "1/2", ""),
                Triple("Lemon", "1", ""),
                Triple("Olive oil", "3", "tbsp"),
                Triple("Garlic", "1", "clove"),
                Triple("Parsley", "1", "handful"),
                Triple("Salt", "1/2", "tsp"),
            ),
            steps = listOf(
                "Whisk the lemon juice, olive oil, grated garlic and salt in a large bowl.",
                "Add the drained chickpeas and toss to coat.",
                "Dice the cucumber, tomatoes and red onion and add them to the bowl.",
                "Fold through the chopped parsley and let it sit for 5 minutes before serving.",
            ),
            substitutions = listOf(
                Substitution("Parsley", "Coriander or mint"),
                Substitution("Red onion", "Spring onions"),
            ),
        ),
        Template(
            title = "Aloo Anday",
            summary = "Homestyle potato and boiled egg curry in a simple, spiced tomato gravy.",
            timeMinutes = 30, servings = 3, cuisine = "Desi (Pakistani / North Indian)",
            tags = setOf("vegetarian", "budget-friendly", "comfort food", "high-protein"),
            ingredients = listOf(
                Triple("Eggs", "4", ""),
                Triple("Potatoes", "3", ""),
                Triple("Onion", "1", ""),
                Triple("Tomatoes", "2", ""),
                Triple("Ginger garlic paste", "1", "tbsp"),
                Triple("Turmeric", "1/2", "tsp"),
                Triple("Red chili powder", "1", "tsp"),
                Triple("Garam masala", "1/2", "tsp"),
                Triple("Vegetable oil", "3", "tbsp"),
                Triple("Coriander leaves", "1", "handful"),
            ),
            steps = listOf(
                "Boil the eggs for 10 minutes, then cool and peel them.",
                "Fry the onion in oil for 6 minutes, then add ginger garlic paste for 1 minute.",
                "Add tomatoes and the ground spices and cook for 5 minutes into a thick masala.",
                "Add the cubed potatoes and 1 1/2 cups of water; cover and simmer for 15 minutes.",
                "Add the eggs, simmer for 3 minutes more and finish with garam masala and coriander.",
            ),
            substitutions = listOf(
                Substitution("Ginger garlic paste", "Grated ginger and garlic"),
                Substitution("Tomatoes", "3 tbsp yogurt for a tangy gravy"),
            ),
        ),
        Template(
            title = "Veggie Stir-fry Noodles",
            summary = "Crisp vegetables and noodles in a sticky soy, garlic and ginger glaze.",
            timeMinutes = 15, servings = 2, cuisine = "East Asian",
            tags = setOf("vegetarian", "vegan", "quick", "one pan", "budget-friendly"),
            ingredients = listOf(
                Triple("Noodles", "200", "g"),
                Triple("Carrot", "1", ""),
                Triple("Cabbage", "2", "cups"),
                Triple("Bell pepper", "1", ""),
                Triple("Garlic", "3", "cloves"),
                Triple("Ginger", "1", "tsp"),
                Triple("Soy sauce", "3", "tbsp"),
                Triple("Sesame oil", "1", "tbsp"),
                Triple("Spring onions", "2", ""),
            ),
            steps = listOf(
                "Cook the noodles for 4 minutes, then drain and rinse with cold water.",
                "Stir-fry the garlic and ginger in sesame oil for 30 seconds.",
                "Add the sliced vegetables and toss on high heat for 3 minutes.",
                "Add noodles and soy sauce and toss for 2 minutes until glossy.",
                "Top with spring onions.",
            ),
            substitutions = listOf(
                Substitution("Sesame oil", "Any oil plus a few toasted sesame seeds"),
                Substitution("Noodles", "Spaghetti, cooked a little past al dente"),
            ),
        ),
        Template(
            title = "Masala Omelette",
            summary = "A fluffy omelette packed with onion, tomato, chili and coriander.",
            timeMinutes = 10, servings = 1, cuisine = "Desi (Pakistani / North Indian)",
            tags = setOf("vegetarian", "quick", "breakfast", "high-protein", "budget-friendly"),
            ingredients = listOf(
                Triple("Eggs", "3", ""),
                Triple("Onion", "1/2", ""),
                Triple("Tomato", "1/2", ""),
                Triple("Green chili", "1", ""),
                Triple("Coriander leaves", "1", "tbsp"),
                Triple("Turmeric", "1", "pinch"),
                Triple("Butter", "1", "tbsp"),
                Triple("Salt", "1", "pinch"),
            ),
            steps = listOf(
                "Beat the eggs with salt and turmeric.",
                "Stir in the finely chopped onion, tomato, chili and coriander.",
                "Melt the butter in a pan over medium heat and pour in the eggs.",
                "Cook for 2 minutes, fold, and cook 1 minute more.",
            ),
            substitutions = listOf(
                Substitution("Butter", "Any oil or ghee"),
                Substitution("Green chili", "A pinch of chili flakes"),
            ),
        ),
        Template(
            title = "Peanut Butter Banana Oats",
            summary = "Creamy stovetop oats with banana, peanut butter and a hint of cinnamon.",
            timeMinutes = 10, servings = 1, cuisine = "breakfast",
            tags = setOf("vegetarian", "quick", "breakfast", "kid-friendly", "budget-friendly"),
            ingredients = listOf(
                Triple("Rolled oats", "1/2", "cup"),
                Triple("Milk", "1", "cup"),
                Triple("Banana", "1", ""),
                Triple("Peanut butter", "1", "tbsp"),
                Triple("Honey", "1", "tsp"),
                Triple("Cinnamon", "1", "pinch"),
            ),
            steps = listOf(
                "Simmer the oats and milk for 5 minutes, stirring often.",
                "Mash in half the banana and the cinnamon.",
                "Spoon into a bowl and top with sliced banana, peanut butter and honey.",
            ),
            substitutions = listOf(
                Substitution("Milk", "Water or any plant milk"),
                Substitution("Honey", "Sugar or jaggery"),
            ),
        ),
        Template(
            title = "Keema Matar",
            summary = "Spiced minced beef with peas, the classic weeknight keema.",
            timeMinutes = 35, servings = 4, cuisine = "Desi (Pakistani / North Indian)",
            tags = setOf("high-protein", "comfort food", "spicy", "one pan"),
            ingredients = listOf(
                Triple("Beef mince", "500", "g"),
                Triple("Onions", "2", ""),
                Triple("Tomatoes", "2", ""),
                Triple("Frozen peas", "1", "cup"),
                Triple("Ginger garlic paste", "1", "tbsp"),
                Triple("Garam masala", "1", "tsp"),
                Triple("Cumin seeds", "1", "tsp"),
                Triple("Red chili powder", "1", "tsp"),
                Triple("Yogurt", "1/4", "cup"),
                Triple("Vegetable oil", "3", "tbsp"),
            ),
            steps = listOf(
                "Fry cumin seeds and onions in oil for 8 minutes until golden.",
                "Add ginger garlic paste and the mince; brown for 6 minutes, breaking it up.",
                "Add tomatoes, chili powder and yogurt and cook for 8 minutes.",
                "Add peas and a splash of water, cover and simmer for 10 minutes.",
                "Finish with garam masala.",
            ),
            substitutions = listOf(
                Substitution("Beef mince", "Chicken or lamb mince"),
                Substitution("Yogurt", "One more tomato"),
            ),
        ),
    )

    /** Picks [count] recipes that best fit the request, marking what the pantry covers. */
    fun pick(request: RecipeRequest, matcher: IngredientMatcher, count: Int = 3): List<Recipe> {
        val pantry = request.ingredients
        val diet = request.diet?.lowercase()

        var candidates = templates.filter { template ->
            diet == null || diet == "halal" || diet in template.tags
        }.ifEmpty { templates }

        request.timeMinutes?.let { max ->
            candidates = candidates.filter { it.timeMinutes <= max }.ifEmpty { candidates }
        }

        // Scores are computed once up front: a random tiebreaker inside a comparator would break sorting.
        val ranked = candidates.map { template ->
            var score = template.ingredients.count { matcher.isInPantry(it.first, pantry) } * 10
            val cuisine = request.cuisine?.lowercase()
            if (cuisine != null && (template.cuisine.lowercase() == cuisine || cuisine in template.tags)) {
                score += 25
            }
            request.equipment?.forEach { if (it.lowercase() in template.tags) score += 15 }
            // A little variety so "Surprise me" does not always return the same three.
            template to score + (0..9).random()
        }.sortedByDescending { it.second }.map { it.first }

        return ranked.take(count).map { template -> template.toRecipe(pantry, matcher) }
    }

    private fun Template.toRecipe(pantry: List<String>, matcher: IngredientMatcher): Recipe {
        val ingredients = ingredients.map { (name, quantity, unit) ->
            val have = name.lowercase() in STAPLES || matcher.isInPantry(name, pantry)
            RecipeIngredient(name = name, quantity = quantity, unit = unit, inPantry = have)
        }
        val missing = ingredients.filterNot { it.inPantry }.map { it.name }
        return Recipe(
            id = "",
            title = title,
            summary = summary,
            timeMinutes = timeMinutes,
            servings = servings,
            ingredients = ingredients,
            steps = steps,
            missing = missing,
            substitutions = substitutions.filter { it.missing in missing },
        )
    }
}
