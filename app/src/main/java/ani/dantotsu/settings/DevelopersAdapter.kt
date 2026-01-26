package ani.dantotsu.settings

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import ani.dantotsu.R
import ani.dantotsu.databinding.ItemDeveloperBinding
import ani.dantotsu.loadImage
import ani.dantotsu.openLinkInBrowser
import ani.dantotsu.setAnimation

sealed class DeveloperItem {
    data class Section(val title: String) : DeveloperItem()
    data class Dev(val developer: Developer) : DeveloperItem()
}

class DevelopersAdapter(private val items: List<DeveloperItem>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_SECTION = 0
        private const val TYPE_DEVELOPER = 1
    }

    inner class SectionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.sectionTitle)
    }

    inner class DeveloperViewHolder(val binding: ItemDeveloperBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is DeveloperItem.Section -> TYPE_SECTION
            is DeveloperItem.Dev -> TYPE_DEVELOPER
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_SECTION -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_developer_section, parent, false)
                SectionViewHolder(view)
            }
            else -> {
                DeveloperViewHolder(
                    ItemDeveloperBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is DeveloperItem.Section -> {
                (holder as SectionViewHolder).title.text = item.title
            }
            is DeveloperItem.Dev -> {
                val b = (holder as DeveloperViewHolder).binding
                setAnimation(b.root.context, b.root)
                val dev = item.developer
                b.devName.text = dev.name
                b.devProfile.loadImage(dev.pfp)
                b.devRole.text = dev.role
                b.root.setOnClickListener {
                    openLinkInBrowser(dev.url)
                }
            }
        }
    }

    override fun getItemCount(): Int = items.size
}