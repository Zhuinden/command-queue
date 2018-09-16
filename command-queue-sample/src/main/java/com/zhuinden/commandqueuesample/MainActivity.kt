package com.zhuinden.commandqueuesample

import android.arch.lifecycle.Observer
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    private val viewModel by lazy { createViewModel { MainViewModel() } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        button.onClick {
            viewModel.doSomethingInteresting()
        }

        viewModel.loading.observe(this, Observer { loading: Boolean? ->
            loadingOverlay.showIf { loading!! }
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

