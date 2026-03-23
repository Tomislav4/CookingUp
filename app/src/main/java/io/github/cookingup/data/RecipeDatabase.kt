package io.github.cookingup.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import io.github.cookingup.model.Recipe
import io.github.cookingup.model.RecipeConverter

@Database(entities = [Recipe::class], version = 1, exportSchema = false)
@TypeConverters(RecipeConverter::class)
abstract class RecipeDatabase : RoomDatabase() {
    abstract fun recipeDao(): RecipeDao
}
