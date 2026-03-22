package com.myname.jarvisai.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.myname.jarvisai.R
import com.myname.jarvisai.databinding.ActivityModelManagerBinding
import com.myname.jarvisai.databinding.ItemModelBinding
import com.myname.jarvisai.models.ModelConfig
import com.myname.jarvisai.utils.PreferencesManager

class ModelManagerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityModelManagerBinding
    private lateinit var prefsManager: PreferencesManager
    private lateinit var adapter: ModelsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityModelManagerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefsManager = PreferencesManager(this)

        setupToolbar()
        setupRecyclerView()
        setupListeners()
        loadModels()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        adapter = ModelsAdapter(
            onItemClick = { model -> showEditDialog(model) },
            onSwitchChanged = { model, enabled -> toggleModel(model, enabled) }
        )
        binding.modelsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.modelsRecyclerView.adapter = adapter
    }

    private fun setupListeners() {
        binding.autoFallbackSwitch.isChecked = prefsManager.isAutoFallbackEnabled()
        binding.autoFallbackSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefsManager.setAutoFallbackEnabled(isChecked)
        }

        binding.addModelButton.setOnClickListener {
            showAddModelDialog()
        }
    }

    private fun loadModels() {
        val models = prefsManager.getModelList()
        adapter.submitList(models)
    }

    private fun toggleModel(model: ModelConfig, enabled: Boolean) {
        val updated = model.copy(enabled = enabled)
        prefsManager.updateModel(model.id, updated)
        Toast.makeText(this, "${model.name} ${if (enabled) "enabled" else "disabled"}", Toast.LENGTH_SHORT).show()
    }

    private fun showAddModelDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_model, null)
        
        AlertDialog.Builder(this)
            .setTitle("Add Custom Model")
            .setView(dialogView)
            .setPositiveButton("Add") { dialog, _ ->
                val name = dialogView.findViewById<TextInputEditText>(R.id.modelNameInput).text.toString()
                val modelId = dialogView.findViewById<TextInputEditText>(R.id.modelIdInput).text.toString()
                val provider = dialogView.findViewById<TextInputEditText>(R.id.providerInput).text.toString()
                val apiKey = dialogView.findViewById<TextInputEditText>(R.id.apiKeyInput).text.toString()
                val baseUrl = dialogView.findViewById<TextInputEditText>(R.id.baseUrlInput).text.toString()

                if (name.isNotBlank() && modelId.isNotBlank() && provider.isNotBlank()) {
                    val newModel = ModelConfig(
                        id = "custom_${System.currentTimeMillis()}",
                        name = name,
                        modelId = modelId,
                        provider = provider.lowercase(),
                        apiKey = apiKey,
                        baseUrl = baseUrl,
                        priority = 100,
                        enabled = true,
                        isFree = false,
                        description = "Custom model"
                    )
                    prefsManager.addModel(newModel)
                    loadModels()
                    Toast.makeText(this, "Model added!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Please fill required fields", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditDialog(model: ModelConfig) {
        val options = arrayOf("Edit Priority", "Delete Model", "Cancel")
        
        AlertDialog.Builder(this)
            .setTitle(model.name)
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> showPriorityDialog(model)
                    1 -> deleteModel(model)
                }
                dialog.dismiss()
            }
            .show()
    }

    private fun showPriorityDialog(model: ModelConfig) {
        val input = TextInputEditText(this).apply {
            setText(model.priority.toString())
            hint = "Priority (lower = higher priority)"
        }

        AlertDialog.Builder(this)
            .setTitle("Set Priority for ${model.name}")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val priority = input.text.toString().toIntOrNull() ?: model.priority
                val updated = model.copy(priority = priority)
                prefsManager.updateModel(model.id, updated)
                loadModels()
                Toast.makeText(this, "Priority updated", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteModel(model: ModelConfig) {
        AlertDialog.Builder(this)
            .setTitle("Delete Model?")
            .setMessage("Are you sure you want to delete ${model.name}?")
            .setPositiveButton("Delete") { _, _ ->
                prefsManager.deleteModel(model.id)
                loadModels()
                Toast.makeText(this, "Model deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}

// RecyclerView Adapter
class ModelsAdapter(
    private val onItemClick: (ModelConfig) -> Unit,
    private val onSwitchChanged: (ModelConfig, Boolean) -> Unit
) : RecyclerView.Adapter<ModelsAdapter.ModelViewHolder>() {

    private var models: List<ModelConfig> = emptyList()

    fun submitList(newModels: List<ModelConfig>) {
        models = newModels.sortedBy { it.priority }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModelViewHolder {
        val binding = ItemModelBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ModelViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ModelViewHolder, position: Int) {
        holder.bind(models[position])
    }

    override fun getItemCount() = models.size

    inner class ModelViewHolder(private val binding: ItemModelBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(model: ModelConfig) {
            binding.apply {
                modelName.text = model.name
                modelProvider.text = model.provider.uppercase()
                modelDescription.text = model.description
                modelPriority.text = "Priority: ${model.priority}"
                freeBadge.visibility = if (model.isFree) android.view.View.VISIBLE else android.view.View.GONE
                modelSwitch.isChecked = model.enabled

                root.setOnClickListener {
                    onItemClick(model)
                }

                modelSwitch.setOnCheckedChangeListener { _, isChecked ->
                    onSwitchChanged(model, isChecked)
                }
                
                // Test button (if exists in layout)
                try {
                    val testBtn = binding.root.findViewById<android.widget.ImageButton>(com.myname.jarvisai.R.id.testButton)
                    testBtn?.setOnClickListener {
                        onTestClick(model)
                    }
                } catch (e: Exception) {
                    // Test button not in layout, skip
                }
            }
        }
    }
}
