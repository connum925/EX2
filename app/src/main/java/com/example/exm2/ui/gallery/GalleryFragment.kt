package com.example.exm2.ui.gallery

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.exm2.Book
import com.example.exm2.BookDetailActivity
import com.example.exm2.MainActivity
import com.example.exm2.databinding.FragmentGalleryBinding
import com.example.exm2.ui.home.BookAdapter
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class GalleryFragment : Fragment() {

    private var _binding: FragmentGalleryBinding? = null
    private val binding get() = _binding!!

    private lateinit var galleryViewModel: GalleryViewModel
    private lateinit var recyclerViewFavorites: RecyclerView
    private lateinit var textNoFavorites: TextView
    private lateinit var progressBarFavorites: ProgressBar
    private lateinit var favoriteBookAdapter: BookAdapter
    private lateinit var databaseReference: DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGalleryBinding.inflate(inflater, container, false)
        val root: View = binding.root

        recyclerViewFavorites = binding.recyclerViewFavorites
        textNoFavorites = binding.textNoFavorites
        progressBarFavorites = binding.progressBarFavorites

        recyclerViewFavorites.layoutManager = LinearLayoutManager(requireContext())
        favoriteBookAdapter = BookAdapter(emptyList(),
            onFavoriteClicked = { book ->
                // Now, call the ViewModel's function to remove the book from favorites
                galleryViewModel.removeFavoriteBook(book)
            },
            onBookClicked = { book ->
                // Navigate to BookDetailActivity
                val intent = Intent(requireContext(), BookDetailActivity::class.java)
                intent.putExtra("book", book)
                startActivity(intent)
            })
        recyclerViewFavorites.adapter = favoriteBookAdapter

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        galleryViewModel = ViewModelProvider(this).get(GalleryViewModel::class.java)

        // Get the username from MainActivity
        val mainActivity = activity as? MainActivity
        val username = mainActivity?.getLoggedInUsername()

        // Initialize Firebase with the user-specific path
        databaseReference = FirebaseDatabase.getInstance().getReference("user_favorites")
        galleryViewModel.setDatabaseReference(databaseReference, username)

        // Observe the favorite books LiveData
        galleryViewModel.favoriteBooks.observe(viewLifecycleOwner) { books ->
            favoriteBookAdapter.books = books
            favoriteBookAdapter.notifyDataSetChanged()
            if (books.isEmpty()) {
                textNoFavorites.visibility = View.VISIBLE
                recyclerViewFavorites.visibility = View.GONE
            } else {
                textNoFavorites.visibility = View.GONE
                recyclerViewFavorites.visibility = View.VISIBLE
            }
        }

        // Observe loading state
        galleryViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            progressBarFavorites.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // Observe errors
        galleryViewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            if (errorMessage.isNotEmpty()) {
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}