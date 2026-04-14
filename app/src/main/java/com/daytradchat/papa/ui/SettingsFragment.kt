//app/src/main/java/com/daytradchat/papa/ui/SettingsFragment.kt
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
import com.daytradchat.papa.databinding.FragmentSettingsBinding
import com.daytradchat.papa.network.SocketConfig
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TradeViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.textPort.text = "Port: ${SocketConfig.SERVER_PORT}"

        binding.buttonSaveReconnect.setOnClickListener {
            viewModel.saveSettingsAndReconnect(
                binding.editHost.text?.toString().orEmpty(),
                binding.editReconnectSec.text?.toString().orEmpty()
            )
        }

        binding.buttonReset.setOnClickListener {
            viewModel.resetSettingsAndReconnect()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.currentHost.collect {
                        if (binding.editHost.text?.toString() != it) {
                            binding.editHost.setText(it)
                            binding.editHost.setSelection(it.length)
                        }
                    }
                }
                launch {
                    viewModel.reconnectSec.collect {
                        val text = it.toString()
                        if (binding.editReconnectSec.text?.toString() != text) {
                            binding.editReconnectSec.setText(text)
                            binding.editReconnectSec.setSelection(text.length)
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
