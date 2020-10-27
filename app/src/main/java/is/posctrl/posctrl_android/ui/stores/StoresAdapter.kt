package `is`.posctrl.posctrl_android.ui.stores

import `is`.posctrl.posctrl_android.data.model.StoreResponse
import `is`.posctrl.posctrl_android.databinding.ItemStoreBinding
import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.recyclerview.widget.RecyclerView

class StoresAdapter(private val clickListener: StoreCellListener) :
        RecyclerView.Adapter<CategoryCellViewHolder>() {

    private var data = ArrayList<StoreResponse>()

    fun setData(data: List<StoreResponse>?) {
        this.data = ArrayList()
        if (data != null) {
            this.data.addAll(data)
        }
        notifyDataSetChanged()
    }


    override fun getItemCount(): Int {
        return data.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryCellViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = ItemStoreBinding.inflate(layoutInflater, parent, false)

        return CategoryCellViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryCellViewHolder, position: Int) {
        val currentItem = data[position]
        holder.bind(clickListener, currentItem)
    }
}

class StoreCellListener(val clickListener: (id: Long?) -> Unit) {
    fun onClick(id: Long?) = clickListener(id)
}

class CategoryCellViewHolder(private val binding: ItemStoreBinding) :
        RecyclerView.ViewHolder(binding.root) {

    fun bind(clickListener: StoreCellListener, item: StoreResponse) {
        binding.clickListener = clickListener
        binding.item = item
    }
}

