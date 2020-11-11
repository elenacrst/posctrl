package `is`.posctrl.posctrl_android.ui.filter

import `is`.posctrl.posctrl_android.databinding.ItemFilterBinding
import `is`.posctrl.posctrl_android.util.glide.load
import android.content.Context
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.recyclerview.widget.RecyclerView
import javax.inject.Inject

//todo check if inject in constructor requires provides method too for the other classes
class SnapshotsAdapter @Inject constructor(val context: Context) :
    RecyclerView.Adapter<SnapshotViewHolder>() {

    private var data = ArrayList<Bitmap>()

    fun setData(data: Array<Bitmap>?) {
        this.data = ArrayList()
        if (data != null) {
            this.data.addAll(data)
        }
        notifyDataSetChanged()
    }


    override fun getItemCount(): Int {
        return data.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SnapshotViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = ItemFilterBinding.inflate(layoutInflater, parent, false)

        return SnapshotViewHolder(binding, context)
    }

    override fun onBindViewHolder(holder: SnapshotViewHolder, position: Int) {
        val currentItem = data[position]
        holder.bind(currentItem)
    }
}

class SnapshotViewHolder(private val binding: ItemFilterBinding, private val context: Context) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(item: Bitmap) {
        binding.ivSnapshot.load(context, item)
    }
}

