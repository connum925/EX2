package com.example.exm2

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide

class BookDetailActivity : AppCompatActivity() {
    private lateinit var ivCover: ImageView
    private lateinit var tvTitle: TextView
    private lateinit var tvAuthor: TextView
    private lateinit var tvDescription: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_book_detail)

        ivCover = findViewById(R.id.ivCover)
        tvTitle = findViewById(R.id.tvTitle)
        tvAuthor = findViewById(R.id.tvAuthor)
        tvDescription = findViewById(R.id.tvDescription)

        val book = intent.getParcelableExtra<Book>("book") // Receive the Book

        if (book != null) {
            tvTitle.text = book.title ?: "Título no disponible"
            tvAuthor.text = book.author_name?.joinToString(", ") ?: "Autor desconocido"
            tvDescription.text = book.description ?: "Descripción no disponible"

            if (book.getCoverUrl() != null) {
                Glide.with(this)
                    .load(book.getCoverUrl())
                    .placeholder(R.drawable.lavender_border)
                    .error(R.drawable.ic_book_error)
                    .into(ivCover)
            } else {
                ivCover.setImageResource(R.drawable.lavender_border)
            }
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}