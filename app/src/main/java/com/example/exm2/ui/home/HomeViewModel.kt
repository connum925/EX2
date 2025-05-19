package com.example.exm2.ui.home

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.exm2.Book
import com.example.exm2.RetrofitClient
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.launch
import java.lang.Exception

class HomeViewModel : ViewModel() {

    private val _books = MutableLiveData<List<Book>>()
    val books: LiveData<List<Book>> = _books

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    private var databaseReference: DatabaseReference? = null
    private var currentUsername: String? = null

    fun setDatabaseReference(ref: DatabaseReference, username: String?) {
        databaseReference = ref
        currentUsername = username
    }

    fun searchBooks(query: String) {
        if (query.isEmpty()) return

        if (!query.matches("[a-zA-Z0-9 ]*".toRegex())) {
            _errorMessage.value = "La búsqueda no puede contener caracteres especiales."
            return
        }

        _isLoading.value = true
        _errorMessage.value = ""

        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.searchBooks(query)
                _books.value = response.docs
            } catch (e: Exception) {
                _errorMessage.value = "Error al buscar libros: ${e.message}"
                Log.e("HomeViewModel", "Error searching books", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleBookFavorite(book: Book) {
        if (databaseReference == null || currentUsername == null) {
            Log.e("HomeViewModel", "Database reference or username is not initialized!")
            return
        }

        val bookKey = book.key?.replace("/works/", "")
        val userFavoritesRef = bookKey?.let { databaseReference!!.child(currentUsername!!).child(it) }

        if (userFavoritesRef != null) {
            userFavoritesRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        // Book is a favorite, remove it
                        userFavoritesRef.removeValue()
                            .addOnSuccessListener {
                                Log.d("HomeViewModel", "Book removed from favorites: ${book.title}")
                            }
                            .addOnFailureListener { e ->
                                Log.e(
                                    "HomeViewModel",
                                    "Error removing book from favorites: ${e.message}",
                                    e
                                )
                            }
                    } else {
                        // Book is not a favorite, add it
                        userFavoritesRef.setValue(book)
                            .addOnSuccessListener {
                                Log.d("HomeViewModel", "Book added to favorites: ${book.title}")
                            }
                            .addOnFailureListener { e ->
                                Log.e(
                                    "HomeViewModel",
                                    "Error adding book to favorites: ${e.message}",
                                    e
                                )
                            }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("HomeViewModel", "Database event cancelled: ${error.message}", error.toException())
                }
            })
        }
    }

    // Function to fetch book details
    fun getBookDetails(bookKey: String, onDetailsFetched: (Book) -> Unit) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.getBookDetails(bookKey)
                // Update the book object with the description
                _books.value = _books.value?.map {
                    if (it.key?.contains(bookKey) == true) {
                        it.apply {
                            description = response.getDescriptionString()
                        }
                    } else {
                        it
                    }
                }
                _books.value?.find { it.key?.contains(bookKey) ?: false }?.let { onDetailsFetched(it) } // Notify the fragment
            } catch (e: Exception) {
                _errorMessage.value = "Error fetching book details: ${e.message}"
                Log.e("HomeViewModel", "Error fetching book details", e)
            }
        }
    }


}