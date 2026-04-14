//app/src/main/java/com/daytradchat/papa/MainActivity.kt
// ver 2.13-15

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
                2 -> "設定"
                else -> "システム"
            }
        }.attach()

        // ★確実にタブ文字サイズ変更
        binding.tabLayout.post {
            val vg = binding.tabLayout.getChildAt(0) as? ViewGroup
            vg?.let {
                for (i in 0 until it.childCount) {
                    val tabView = it.getChildAt(i) as? ViewGroup
                    tabView?.let { tvg ->
                        for (j in 0 until tvg.childCount) {
                            val v = tvg.getChildAt(j)
                            if (v is TextView) {
                                v.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
                            }
                        }
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
        if (isFinishing) {
            viewModel.stopSocket()
        }
    }
}
