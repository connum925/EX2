package com.example.exm2.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.exm2.BookDetailActivity
import com.example.exm2.MainActivity
import com.example.exm2.databinding.FragmentHomeBinding
import com.google.android.material.button.MaterialButton
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var homeViewModel: HomeViewModel
    private lateinit var etSearch: EditText
    private lateinit var btnSearch: MaterialButton
    private lateinit var recyclerView: RecyclerView
    private lateinit var tvNoResults: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var bookAdapter: BookAdapter
    private lateinit var databaseReference: DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        etSearch = binding.etSearch
        btnSearch = binding.btnSearch
        recyclerView = binding.recyclerView
        tvNoResults = binding.tvNoResults
        progressBar = binding.progressBar

        // Get the username from MainActivity
        val mainActivity = activity as? MainActivity
        val username = mainActivity?.getLoggedInUsername()

        // Initialize Firebase with the user-specific path
        databaseReference =
            FirebaseDatabase.getInstance().getReference("user_favorites") // Changed path
        homeViewModel.setDatabaseReference(databaseReference, username)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        bookAdapter = BookAdapter(emptyList(),
            onFavoriteClicked = { book ->
                homeViewModel.toggleBookFavorite(book)
            },
            onBookClicked = { book ->
                homeViewModel.getBookDetails(book.key.replace("/works/", "")) { detailedBook ->
                    // Start BookDetailActivity with book details
                    val intent = Intent(requireContext(), BookDetailActivity::class.java)
                    intent.putExtra("book", detailedBook) // Pass the Book object
                    startActivity(intent)
                }
            })
        recyclerView.adapter = bookAdapter

        homeViewModel.books.observe(viewLifecycleOwner) { books ->
            bookAdapter.books = books
            bookAdapter.notifyDataSetChanged()
            if (books.isEmpty()) {
                tvNoResults.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            } else {
                tvNoResults.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
            }
        }

        homeViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        homeViewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            if (errorMessage.isNotEmpty()) {
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
            }
        }

        btnSearch.setOnClickListener {
            homeViewModel.searchBooks(etSearch.text.toString().trim())
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}