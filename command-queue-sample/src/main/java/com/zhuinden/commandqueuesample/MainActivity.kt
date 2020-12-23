package com.zhuinden.commandqueuesample

import android.os.Bundle
import android.view.Window
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.zhuinden.commandqueuesample.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private val viewModel by lazy { createViewModel { MainViewModel() } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        val binding = ActivityMainBinding.bind(findViewById(Window.ID_ANDROID_CONTENT))

        binding.button.onClick {
            viewModel.doSomethingInteresting()
        }

        viewModel.loading.observe(this, Observer { loading: Boolean? ->
            binding.loadingOverlay.showIf { loading!! }
        })
    }

    override fun onStart() {
        super.onStart()
        viewModel.commandQueue.setReceiver { command ->
            when (command) {
                is MainViewModel.Events.DoSomething -> showToast("Do something!", Toast.LENGTH_SHORT)
                is MainViewModel.Events.DoOtherThing -> showToast("Do other thing!", Toast.LENGTH_SHORT)
            }.safe()
        }
    }

    override fun onStop() {
        viewModel.commandQueue.detachReceiver()
        super.onStop()
    }
}

