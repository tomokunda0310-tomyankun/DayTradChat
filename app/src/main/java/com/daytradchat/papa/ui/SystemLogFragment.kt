//app/src/main/java/com/daytradchat/papa/ui/SystemLogFragment.kt
//ver 2.13-00
package com.daytradchat.papa.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.daytradchat.papa.databinding.FragmentLogBinding
import kotlinx.coroutines.launch

class SystemLogFragment : Fragment() {
    private var _binding: FragmentLogBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TradeViewModel by activityViewModels()
    private val adapter = SystemLogAdapter()
    private var stickToTop = true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerLogs.layoutManager = layoutManager
        binding.recyclerLogs.adapter = adapter

        binding.recyclerLogs.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                stickToTop = layoutManager.findFirstCompletelyVisibleItemPosition() <= 0
            }
        })

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.systemLogItems.collect { list ->
                    adapter.submitList(list) {
                        if (stickToTop) {
                            binding.recyclerLogs.doOnPreDraw {
                                if (adapter.itemCount > 0) {
                                    binding.recyclerLogs.scrollToPosition(0)
                                }
                            }
                        }
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
