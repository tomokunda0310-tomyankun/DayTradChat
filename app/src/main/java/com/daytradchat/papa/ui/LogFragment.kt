//app/src/main/java/com/daytradchat/papa/ui/LogFragment.kt
//ver 2.13-00
package com.daytradchat.papa.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.daytradchat.papa.databinding.FragmentLogBinding
import kotlinx.coroutines.launch

class LogFragment : Fragment() {
    private var _binding: FragmentLogBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TradeViewModel by activityViewModels()
    private val adapter = LogAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.recyclerLogs.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerLogs.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.logItems.collect { adapter.submitList(it) }
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
