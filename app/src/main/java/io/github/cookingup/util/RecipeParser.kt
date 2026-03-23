package io.github.cookingup.util

import io.github.cookingup.model.Ingredient
import io.github.cookingup.model.Recipe

object RecipeParser {
    fun isRecipe(text: String): Boolean {
        val lower = text.lowercase()
        return lower.contains("ingredients") || lower.contains("instructions") || lower.contains("directions")
    }

    fun parse(text: String): Recipe {
        val lines = text.lines()
        var title = "Unknown Recipe"
        val ingredients = mutableListOf<Ingredient>()
        val instructions = StringBuilder()
        
        var currentSection = ""

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            val lower = trimmed.lowercase()
            when {
                lower.startsWith("ingredients") -> currentSection = "ingredients"
                lower.startsWith("instructions") || lower.startsWith("directions") -> currentSection = "instructions"
                title == "Unknown Recipe" && trimmed.length > 3 -> title = trimmed
                currentSection == "ingredients" -> {
                    parseIngredient(trimmed)?.let { ingredients.add(it) }
                }
                currentSection == "instructions" -> {
                    instructions.append(trimmed).append("\n")
                }
            }
        }

        return Recipe(
            title = title,
            description = "Imported from scan",
            ingredients = ingredients,
            instructions = instructions.toString().trim(),
            servings = 1
        )
    }

    private fun parseIngredient(line: String): Ingredient? {
        // Simple parser: "1 cup sugar" -> amount: 1.0, unit: "cup", name: "sugar"
        val regex = Regex("""^([\d\./]+)\s*([a-zA-Z]*)\s*(.*)$""")
        val match = regex.find(line)
        return if (match != null) {
            val (amountStr, unit, name) = match.destructured
            val amount = evalFraction(amountStr)
            Ingredient(name = name.trim(), amount = amount, unit = unit.trim())
        } else {
            Ingredient(name = line, amount = 0.0, unit = "")
        }
    }

    private fun evalFraction(str: String): Double {
        return try {
            if (str.contains("/")) {
                val parts = str.split("/")
                parts[0].toDouble() / parts[1].toDouble()
            } else {
                str.toDouble()
            }
        } catch (e: Exception) {
            1.0
        }
    }
}
