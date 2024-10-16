package com.example.juicetracker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.R.layout
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.navArgs
import com.example.juicetracker.data.JuiceColor
import com.example.juicetracker.databinding.FragmentEntryDialogBinding
import com.example.juicetracker.ui.AppViewModelProvider
import com.example.juicetracker.ui.EntryViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

class EntryDialogFragment: BottomSheetDialogFragment() {
    private val entryVM by viewModels<EntryViewModel>{
        AppViewModelProvider.Factory
    }
    private var selectedColor: JuiceColor = JuiceColor.Red

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FragmentEntryDialogBinding
            .inflate(inflater, container, false).root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentEntryDialogBinding.bind(view)
        val args: EntryDialogFragmentArgs by navArgs()
        val juiceId = args.itemId
        val colorLabelMap = JuiceColor.entries.associateBy { getString(it.label) }
        if(args.itemId > 0){
            viewLifecycleOwner.lifecycleScope.launch{
                repeatOnLifecycle(Lifecycle.State.STARTED){
                    entryVM.getJuiceStream(args.itemId)
                        .filterNotNull().collect{ item ->
                        with(binding){
                            name.setText(item.name)
                            description.setText(item.description)
                            ratingBar.rating = item.rating.toFloat()
                            colorSpinner.setSelection(findColorIndex(item.color))
                        }
                    }
                }
            }
        }
        //Validar boton guardar si no esta vacio el campo de texto
        binding.name.doOnTextChanged{ _, start, _, count ->
            binding.saveButton.isEnabled = (start + count) > 0
        }
        //Configurar lista de colores
        binding.colorSpinner.adapter = ArrayAdapter(
            requireContext(),
            layout.support_simple_spinner_dropdown_item,
            colorLabelMap.map {it.key}
        )
        binding.colorSpinner.onItemSelectedListener = object :
            AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                val selected = parent.getItemAtPosition(pos).toString()
                selectedColor = colorLabelMap[selected] ?: selectedColor
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                selectedColor = JuiceColor.Red
            }
        }
        //Boton de guardar
        binding.saveButton.setOnClickListener {
            entryVM.saveJuice(
                id = juiceId,
                name = binding.name.text.toString(),
                description = binding.description.text.toString(),
                color = selectedColor.name,
                rating = binding.ratingBar.rating.toInt()
            )
            dismiss()
        }
        //Boton de cancelar
        binding.cancelButton.setOnClickListener {
            dismiss()
        }
    }
    /** Encuentra el indice segun el color dado **/
    private fun findColorIndex(color: String): Int {
        val juiceColor = JuiceColor.valueOf(color)
        return JuiceColor.entries.indexOf(juiceColor)
    }
}