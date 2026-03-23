package io.github.cookingup.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Entity(tableName = "recipes")
data class Recipe(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val ingredients: List<Ingredient>,
    val instructions: String,
    val servings: Int,
    val imagePath: String? = null,
    val tags: List<String> = emptyList()
)

data class Ingredient(
    val name: String,
    val amount: Double,
    val unit: String
)

data class RecipeLibrary(
    val recipes: List<Recipe>,
    val backupDate: Long = System.currentTimeMillis()
)

class RecipeConverter {
    private val gson = Gson()

    @TypeConverter
    fun fromIngredientList(value: List<Ingredient>): String = gson.toJson(value)

    @TypeConverter
    fun toIngredientList(value: String): List<Ingredient> {
        val listType = object : TypeToken<List<Ingredient>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun fromTagList(value: List<String>): String = gson.toJson(value)

    @TypeConverter
    fun toTagList(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType)
    }
}
