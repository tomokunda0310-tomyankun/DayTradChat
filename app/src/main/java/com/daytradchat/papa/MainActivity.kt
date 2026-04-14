//app/src/main/java/com/daytradchat/papa/MainActivity.kt
//ver 2.13-12
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
import com.google.android.material.tabs.TabLayout
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
                2 -> "設定"
                else -> "システム"
            }
        }.attach()

        binding.tabLayout.post {
            applyTabTextSize(binding.tabLayout, 9f)
        }

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                binding.tabLayout.post { applyTabTextSize(binding.tabLayout, 9f) }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                binding.tabLayout.post { applyTabTextSize(binding.tabLayout, 9f) }
            }

            override fun onTabReselected(tab: TabLayout.Tab) {
                binding.tabLayout.post { applyTabTextSize(binding.tabLayout, 9f) }
            }
        })

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.statusLeft.collect { binding.textStatusLeft.text = it }
                }
                launch {
                    viewModel.statusRight.collect { binding.textStatusRight.text = it }
                }
                launch {
                    viewModel.hostLine.collect { binding.textHostLine.text = it }
                }
            }
        }

        viewModel.startSocket()
    }

    private fun applyTabTextSize(tabLayout: TabLayout, sizeSp: Float) {
        val slidingTabIndicator = tabLayout.getChildAt(0) as? ViewGroup ?: return
        for (i in 0 until slidingTabIndicator.childCount) {
            val tabView = slidingTabIndicator.getChildAt(i) as? ViewGroup ?: continue
            applyTextSizeRecursive(tabView, sizeSp)
        }
    }

    private fun applyTextSizeRecursive(parent: ViewGroup, sizeSp: Float) {
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            when (child) {
                is TextView -> child.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
                is ViewGroup -> applyTextSizeRecursive(child, sizeSp)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            viewModel.stopSocket()
        }
    }
}