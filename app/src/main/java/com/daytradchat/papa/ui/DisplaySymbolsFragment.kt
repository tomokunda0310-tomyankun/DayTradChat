//app/src/main/java/com/daytradchat/papa/ui/DisplaySymbolsFragment.kt
//ver 2.16-00
package com.daytradchat.papa.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.daytradchat.papa.databinding.FragmentDisplaySymbolsBinding
import kotlinx.coroutines.launch

class DisplaySymbolsFragment : Fragment() {
    private var _binding: FragmentDisplaySymbolsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TradeViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDisplaySymbolsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val allAdapter = ArrayAdapter<String>(requireContext(), android.R.layout.simple_list_item_1, mutableListOf())
        val selectedAdapter = ArrayAdapter<String>(requireContext(), android.R.layout.simple_list_item_1, mutableListOf())

        binding.listAllCodes.adapter = allAdapter
        binding.listSelectedCodes.adapter = selectedAdapter

        binding.listAllCodes.setOnItemClickListener { _, _, position, _ ->
            viewModel.addDisplayCode(allAdapter.getItem(position).orEmpty())
        }
        binding.listSelectedCodes.setOnItemClickListener { _, _, position, _ ->
            viewModel.removeDisplayCodeAt(position)
        }
        binding.buttonResetDisplay.setOnClickListener {
            viewModel.resetDisplayCodesToday()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.availableCodes.collect { codes ->
                        allAdapter.clear()
                        allAdapter.addAll(codes)
                        allAdapter.notifyDataSetChanged()
                    }
                }
                launch {
                    viewModel.selectedDisplayLabels.collect { selected ->
                        selectedAdapter.clear()
                        selectedAdapter.addAll(selected)
                        selectedAdapter.notifyDataSetChanged()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
