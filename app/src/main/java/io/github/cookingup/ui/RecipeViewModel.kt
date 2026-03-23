package io.github.cookingup.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.cookingup.CookingUpApp
import io.github.cookingup.model.Recipe
import io.github.cookingup.model.RecipeLibrary
import io.github.cookingup.util.RecipeParser
import com.google.gson.Gson
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class RecipeViewModel(application: Application) : AndroidViewModel(application) {
    private val recipeDao = (application as CookingUpApp).database.recipeDao()
    private val gson = Gson()
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    val allRecipes: StateFlow<List<Recipe>> = recipeDao.getAllRecipes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _pendingRecipe = MutableStateFlow<Recipe?>(null)
    val pendingRecipe: StateFlow<Recipe?> = _pendingRecipe.asStateFlow()

    private val _pendingLibrary = MutableStateFlow<List<Recipe>>(emptyList())
    val pendingLibrary: StateFlow<List<Recipe>> = _pendingLibrary.asStateFlow()

    private val _backups = MutableStateFlow<List<File>>(emptyList())
    val backups: StateFlow<List<File>> = _backups.asStateFlow()

    private val _maxBackups = MutableStateFlow(5)
    val maxBackups: StateFlow<Int> = _maxBackups.asStateFlow()

    init {
        loadBackups()
    }

    fun setPendingRecipe(recipe: Recipe?) {
        _pendingRecipe.value = recipe
    }

    fun clearPendingLibrary() {
        _pendingLibrary.value = emptyList()
    }

    fun insertRecipe(recipe: Recipe) {
        viewModelScope.launch {
            recipeDao.insertRecipe(recipe)
        }
    }

    fun deleteRecipe(recipe: Recipe) {
        viewModelScope.launch {
            recipeDao.deleteRecipe(recipe)
        }
    }

    suspend fun getRecipeById(id: Long): Recipe? {
        return recipeDao.getRecipeById(id)
    }

    fun setMaxBackups(count: Int) {
        _maxBackups.value = count
    }

    fun loadBackups() {
        val backupDir = File(getApplication<Application>().filesDir, "backups")
        if (backupDir.exists()) {
            _backups.value = backupDir.listFiles()?.toList()?.sortedByDescending { it.lastModified() } ?: emptyList()
        }
    }

    private fun createBackup() {
        viewModelScope.launch {
            val recipes = allRecipes.value
            val library = RecipeLibrary(recipes)
            val json = gson.toJson(library)
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "backup_$timestamp.culbr"
            val backupDir = File(getApplication<Application>().filesDir, "backups")
            if (!backupDir.exists()) backupDir.mkdirs()

            val file = File(backupDir, fileName)
            FileOutputStream(file).use { it.write(json.toByteArray()) }
            
            // Rolling backups
            val currentBackups = backupDir.listFiles()?.sortedBy { it.lastModified() } ?: emptyList()
            if (currentBackups.size > _maxBackups.value) {
                currentBackups.take(currentBackups.size - _maxBackups.value).forEach { it.delete() }
            }
            loadBackups()
        }
    }

    fun exportRecipe(context: Context, recipe: Recipe): Uri? {
        // Embed image as Base64 for .curcp if it exists
        var recipeToExport = recipe
        recipe.imagePath?.let { path ->
            try {
                val bitmap = BitmapFactory.decodeFile(path)
                val baos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos)
                val encodedImage = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)
                recipeToExport = recipe.copy(imagePath = "data:image/jpeg;base64,$encodedImage")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val json = gson.toJson(recipeToExport)
        val fileName = "${recipe.title.replace(" ", "_")}.curcp"
        val exportDir = File(context.filesDir, "exports")
        if (!exportDir.exists()) exportDir.mkdirs()
        
        val file = File(exportDir, fileName)
        return try {
            FileOutputStream(file).use { it.write(json.toByteArray()) }
            androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun exportLibrary(context: Context, recipes: List<Recipe>): Uri? {
        // Export only text data for library (.culbr) - explicitly clear imagePath
        val library = RecipeLibrary(recipes.map { it.copy(imagePath = null) })
        val json = gson.toJson(library)
        val fileName = "cookingup_library_${System.currentTimeMillis()}.culbr"
        val exportDir = File(context.filesDir, "exports")
        if (!exportDir.exists()) exportDir.mkdirs()

        val file = File(exportDir, fileName)
        return try {
            FileOutputStream(file).use { it.write(json.toByteArray()) }
            androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun importRecipe(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val json = inputStream.bufferedReader().use { it.readText() }
                    // If it's a single recipe file, wrap it in a list and show preview
                    try {
                        val recipe = gson.fromJson(json, Recipe::class.java)
                        if (recipe.title.isNotEmpty()) {
                             _pendingLibrary.value = listOf(recipe)
                             return@launch
                        }
                    } catch (e: Exception) {
                        // Not a single recipe, try library
                    }
                    
                    val library = gson.fromJson(json, RecipeLibrary::class.java)
                    _pendingLibrary.value = library.recipes
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun prepareLibraryImport(context: Context, uri: Uri) {
        importRecipe(context, uri)
    }

    fun confirmLibraryImport(recipes: List<Recipe>) {
        createBackup()
        viewModelScope.launch {
            val context = getApplication<Application>().applicationContext
            recipes.forEach { recipe ->
                var recipeToInsert = recipe.copy(id = 0)
                
                // Handle embedded image if present during library/single import
                if (recipe.imagePath?.startsWith("data:image") == true) {
                    try {
                        val base64Data = recipe.imagePath.substringAfter(",")
                        val imageBytes = Base64.decode(base64Data, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                        val fileName = "recipe_image_${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg"
                        val file = File(context.filesDir, fileName)
                        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
                        recipeToInsert = recipeToInsert.copy(imagePath = file.absolutePath)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        recipeToInsert = recipeToInsert.copy(imagePath = null)
                    }
                } else {
                    // For library imports that aren't backups, we usually don't have local paths
                    // but we clear it to be safe if it's not a data URI
                    if (recipe.imagePath != null && !recipe.imagePath.startsWith("/")) {
                        recipeToInsert = recipeToInsert.copy(imagePath = null)
                    }
                }
                
                recipeDao.insertRecipe(recipeToInsert)
            }
            clearPendingLibrary()
        }
    }

    fun importLibrary(context: Context, uri: Uri) {
        importRecipe(context, uri)
    }

    fun restoreBackup(context: Context, backupFile: File) {
        createBackup()
        viewModelScope.launch {
            try {
                val json = backupFile.readText()
                val library = gson.fromJson(json, RecipeLibrary::class.java)
                library.recipes.forEach { insertRecipe(it.copy(id = 0)) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun processRecipeImage(context: Context, uri: Uri): Recipe? {
        _isProcessing.value = true
        return try {
            val image = InputImage.fromFilePath(context, uri)
            val result = recognizer.process(image).await()
            val text = result.text
            
            if (RecipeParser.isRecipe(text)) {
                val recipe = RecipeParser.parse(text)
                _pendingRecipe.value = recipe
                recipe
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            _isProcessing.value = false
        }
    }
}
