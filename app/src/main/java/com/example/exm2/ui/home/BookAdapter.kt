package com.example.exm2.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.exm2.Book
import com.example.exm2.R


class BookAdapter(
    var books: List<Book>,
    private val onFavoriteClicked: (Book) -> Unit,
    private val onBookClicked: (Book) -> Unit // Add book click callback
) : RecyclerView.Adapter<BookAdapter.BookViewHolder>() {

    inner class BookViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.bookTitleTextView)
        val authorTextView: TextView = itemView.findViewById(R.id.bookAuthorTextView)
        val coverImageView: ImageView = itemView.findViewById(R.id.bookCoverImageView)
        val favoriteButton: ImageView = itemView.findViewById(R.id.favoriteButton)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onBookClicked(books[position])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_book, parent, false)
        return BookViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        val book = books[position]
        holder.titleTextView.text = book.title
        holder.authorTextView.text = book.author_name?.joinToString(", ") ?: "Unknown Author"

        val coverUrl = book.getCoverUrl()
        if (coverUrl != null) {
            Glide.with(holder.coverImageView.context)
                .load(coverUrl)
                .placeholder(R.drawable.lavender_border)
                .error(R.drawable.ic_book_error)
                .into(holder.coverImageView)
        } else {
            holder.coverImageView.setImageResource(R.drawable.lavender_border)
        }

        holder.favoriteButton.setOnClickListener {
            onFavoriteClicked(book)
        }
    }

    override fun getItemCount() = books.size
}