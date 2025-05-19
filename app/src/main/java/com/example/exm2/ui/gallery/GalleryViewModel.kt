package com.example.exm2.ui.gallery

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.exm2.Book
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class GalleryViewModel : ViewModel() {

    private val _favoriteBooks = MutableLiveData<List<Book>>()
    val favoriteBooks: LiveData<List<Book>> = _favoriteBooks

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    private var databaseReference: DatabaseReference? = null
    private var currentUsername: String? = null
    //ValueEventListener
    private var valueEventListener: ValueEventListener? = null

    // Function to set the database reference and username
    fun setDatabaseReference(ref: DatabaseReference, username: String?) {
        databaseReference = ref
        currentUsername = username
        loadFavoriteBooks() // Load favorites immediately after setting the reference
    }

    // Function to load the user's favorite books from Firebase
    private fun loadFavoriteBooks() {
        if (databaseReference == null || currentUsername == null) {
            _errorMessage.value = "Error: Database reference or username is not initialized."
            return
        }

        _isLoading.value = true
        _errorMessage.value = ""

        //Remove old listener
        valueEventListener?.let { databaseReference!!.child(currentUsername!!).removeEventListener(it) }

        valueEventListener = object : ValueEventListener { // Use addValueEventListener to keep updated
            override fun onDataChange(snapshot: DataSnapshot) {
                val books = mutableListOf<Book>()
                for (childSnapshot in snapshot.children) {
                    val book = childSnapshot.getValue(Book::class.java)
                    book?.let {
                        if (it.key.isNotEmpty()) {
                            books.add(it)
                        }
                    }
                }
                _favoriteBooks.value = books
                _isLoading.value = false
            }

            override fun onCancelled(error: DatabaseError) {
                _errorMessage.value = "Error loading favorite books: ${error.message}"
                _isLoading.value = false
                Log.e("GalleryViewModel", "Database error: ${error.message}", error.toException())
            }
        }

        databaseReference!!.child(currentUsername!!).addValueEventListener(valueEventListener!!)
    }

    fun removeFavoriteBook(book: Book) {
        if (databaseReference == null || currentUsername == null) {
            _errorMessage.value = "Error: Database reference or username is not initialized."
            return
        }

        val bookKey = book.key.replace("/works/", "")
        val userFavoritesRef = databaseReference!!.child(currentUsername!!).child(bookKey)

        viewModelScope.launch {
            try {
                userFavoritesRef.removeValue().await()
                Log.d("GalleryViewModel", "Book removed from favorites: ${book.title}")
            } catch (e: Exception) {
                _errorMessage.value = "Error removing book: ${e.message}"
                Log.e("GalleryViewModel", "Error removing book: ${e.message}", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Remove the listener to prevent memory leaks (important!)
        valueEventListener?.let { databaseReference?.child(currentUsername!!)?.removeEventListener(it) }
    }
}