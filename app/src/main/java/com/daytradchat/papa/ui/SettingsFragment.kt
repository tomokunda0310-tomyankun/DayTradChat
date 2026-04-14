//app/src/main/java/com/daytradchat/papa/ui/SettingsFragment.kt
//ver 2.13-13

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.textVersion.text = "ver 2.13-13"
        binding.textPort.text = "Port: ${SocketConfig.SERVER_PORT}"

        binding.buttonSaveReconnect.setOnClickListener {
            val host = binding.editHost.text?.toString().orEmpty().trim()
            val reconnectSec = binding.editReconnectSec.text?.toString()?.trim()?.toIntOrNull() ?: 0
            viewModel.saveSettingsAndReconnect(host, reconnectSec)
        }

        binding.buttonReset.setOnClickListener {
            viewModel.resetSettingsAndReconnect()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.currentHost.collect { host ->
                        val current = binding.editHost.text?.toString().orEmpty()
                        if (current != host) {
                            binding.editHost.setText(host)
                            binding.editHost.setSelection(host.length)
                        }
                    }
                }

                launch {
                    viewModel.reconnectSec.collect { sec ->
                        val secText = sec.toString()
                        val current = binding.editReconnectSec.text?.toString().orEmpty()
                        if (current != secText) {
                            binding.editReconnectSec.setText(secText)
                            binding.editReconnectSec.setSelection(secText.length)
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