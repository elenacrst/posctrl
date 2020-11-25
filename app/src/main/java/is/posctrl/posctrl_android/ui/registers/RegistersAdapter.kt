package `is`.posctrl.posctrl_android.ui.registers

import `is`.posctrl.posctrl_android.data.model.RegisterResult
import `is`.posctrl.posctrl_android.databinding.ItemRegisterBinding
import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.recyclerview.widget.RecyclerView

class RegistersAdapter(private val clickListener: RegisterCellListener) :
    RecyclerView.Adapter<RegisterViewHolder>() {

    private var data = ArrayList<RegisterResult>()

    fun setData(data: Array<RegisterResult>?) {
        this.data = ArrayList()
        if (data != null) {
            this.data.addAll(data)
        }
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RegisterViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = ItemRegisterBinding.inflate(layoutInflater, parent, false)

        return RegisterViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RegisterViewHolder, position: Int) {
        val currentItem = data[position]
        holder.bind(clickListener, currentItem)
    }
}

open class RegisterCellListener(val clickListener: (register: RegisterResult) -> Unit) {
    fun onClick(register: RegisterResult) = clickListener(register)
}

class RegisterViewHolder(private val binding: ItemRegisterBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(clickListener: RegisterCellListener, item: RegisterResult) {
        binding.clickListener = clickListener
        binding.item = item
    }
}

