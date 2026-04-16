//app/src/main/java/com/daytradchat/papa/ui/DisplaySymbolsFragment.kt
//ver 2.16-13
package com.daytradchat.papa.ui

import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
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
    private var lastTapPosition: Int = -1
    private var lastTapAt: Long = 0L

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDisplaySymbolsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val selectedAdapter = ArrayAdapter<String>(requireContext(), android.R.layout.simple_list_item_1, mutableListOf())
        binding.listSelectedCodes.adapter = selectedAdapter

        binding.listSelectedCodes.setOnItemClickListener { _, _, position, _ ->
            val now = SystemClock.elapsedRealtime()
            if (position == lastTapPosition && now - lastTapAt <= 500L) {
                viewModel.removeDisplayCodeAt(position)
                lastTapPosition = -1
                lastTapAt = 0L
            } else {
                lastTapPosition = position
                lastTapAt = now
                Toast.makeText(requireContext(), "同じ行をもう一度タップで削除", Toast.LENGTH_SHORT).show()
            }
        }
        binding.buttonUndoRemove.setOnClickListener {
            viewModel.restoreLastRemovedDisplayCode()
        }
        binding.buttonResetDisplay.setOnClickListener {
            viewModel.resetDisplayCodesToday()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.selectedDisplayLabels.collect { selected ->
                        selectedAdapter.clear()
                        selectedAdapter.addAll(selected.toList())
                        selectedAdapter.notifyDataSetChanged()
                    }
                }
                launch {
                    viewModel.canUndoDisplayRemoval.collect { canUndo ->
                        binding.buttonUndoRemove.isEnabled = canUndo
                        binding.buttonUndoRemove.alpha = if (canUndo) 1f else 0.5f
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
