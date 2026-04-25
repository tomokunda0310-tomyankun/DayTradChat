//app/src/main/java/com/daytradchat/papa/MainActivity.kt
//ver 2.16-10
package com.daytradchat.papa

import android.os.Bundle
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.daytradchat.papa.databinding.ActivityMainBinding
import com.daytradchat.papa.ui.MainPagerAdapter
import com.daytradchat.papa.ui.TradeViewModel
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: TradeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.viewPager.adapter = MainPagerAdapter(this)
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "シグナル"
                1 -> "ログ"
                2 -> "設定1"
                3 -> "設定2"
                else -> "システム"
            }
        }.attach()

        val tabStrip = binding.tabLayout.getChildAt(0) as? ViewGroup
        if (tabStrip != null) {
            for (i in 0 until tabStrip.childCount) {
                val tabView = tabStrip.getChildAt(i) as? ViewGroup ?: continue
                for (j in 0 until tabView.childCount) {
                    val v = tabView.getChildAt(j)
                    if (v is TextView) {
                        v.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f)
                        v.maxLines = 1
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.statusLeft.collect { binding.textStatusLeft.text = it } }
                launch { viewModel.statusRight.collect { binding.textStatusRight.text = it } }
                launch { viewModel.hostLine.collect { binding.textHostLine.text = it } }
            }
        }

        viewModel.startSocket()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) viewModel.stopSocket()
    }
}
