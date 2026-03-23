package io.github.cookingup

import android.app.Application
import androidx.room.Room
import io.github.cookingup.data.RecipeDatabase

class CookingUpApp : Application() {
    val database: RecipeDatabase by lazy {
        Room.databaseBuilder(
            this,
            RecipeDatabase::class.java,
            "cookingup_database"
        )
        .fallbackToDestructiveMigration() // For development, consider migrations later
        .build()
    }
}
