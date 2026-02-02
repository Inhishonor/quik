/*
- Copyright (C) 2025
 *
- This file is part of QUIK.
 *
- QUIK is free software: you can redistribute it and/or modify
- it under the terms of the GNU General Public License as published by
- the Free Software Foundation, either version 3 of the License, or
- (at your option) any later version.
 *
- QUIK is distributed in the hope that it will be useful,
- but WITHOUT ANY WARRANTY; without even the implied warranty of
- MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
- GNU General Public License for more details.
 *
- You should have received a copy of the GNU General Public License
- along with QUIK.  If not, see <http://www.gnu.org/licenses/>.
 */
package dev.octoshrimpy.quik.feature.compose

import android.app.Activity
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import dev.octoshrimpy.quik.databinding.EmojiReactionPickerBinding
import dev.octoshrimpy.quik.model.Message

class EmojiReactionPicker(
    private val context: Activity,
    private val message: Message,
    private val existingEmoji: String?,
    private val onEmojiSelected: (String, Boolean) -> Unit  // emoji, isRemoval
) {
    // We have to use a custom picker, rather than a dialog, because emojipicker from androidx
    // breaks when using wrap_content. This is very unfourtanate, but is the best we can do.
    fun show() {
        val binding = EmojiReactionPickerBinding.inflate(LayoutInflater.from(context))

        val parentLayout = context.findViewById<ViewGroup>(android.R.id.content)
        parentLayout.addView(binding.root)

        // Dismiss dialog by tapping outside of it
        binding.root.setOnClickListener {
            parentLayout.removeView(binding.root)
        }
        // Prevent clicks inside the dialog from dismissing it
        binding.dialog.setOnClickListener { /* Consume click */ }

        // Map of emoji views to their emoji strings
        val emojiButtons = mapOf(
            binding.emojiThumbsUp to "👍",
            binding.emojiThumbsDown to "👎",
            binding.emojiLaugh to "😂",
            binding.emojiSurprised to "😮",
            binding.emojiSad to "😢",
            binding.emojiAngry to "😠",
            binding.emojiHeart to "❤️"
        )

        // Highlight existing reaction
        emojiButtons.forEach { (button, emoji) ->
            if (emoji == existingEmoji) {
                button.setBackgroundColor(Color.parseColor("#E0E0E0"))
            }
        }

        val emojiClickListener: (String) -> Unit = { emoji ->
            val isRemoval = emoji == existingEmoji
            onEmojiSelected(emoji, isRemoval)
            parentLayout.removeView(binding.root)
        }

        emojiButtons.forEach { (button, emoji) ->
            button.setOnClickListener { emojiClickListener(emoji) }
        }

        binding.emojiPickerFull.setOnEmojiPickedListener { emojiViewItem ->
            val emoji = emojiViewItem.emoji
            val isRemoval = emoji == existingEmoji
            onEmojiSelected(emoji, isRemoval)
            parentLayout.removeView(binding.root)
        }
    }

}