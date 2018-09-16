package com.zhuinden.commandqueuesample

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.os.Handler
import android.os.Looper
import com.zhuinden.commandqueue.CommandQueue
import java.util.concurrent.Executors

class MainViewModel : ViewModel() {
    private val backgroundThread = Executors.newSingleThreadExecutor()
    private val uiThread = Handler(Looper.getMainLooper())

    sealed class Events {
        data class DoSomething(val placeholder: String = "") : Events()
        data class DoOtherThing(val placeholder: String = "") : Events()
    }

    val loading: MutableLiveData<Boolean> = MutableLiveData()

    val commandQueue: CommandQueue<Events> = CommandQueue()

    fun doSomethingInteresting() {
        if(loading.value == true) {
            return
        }
        loading.value = true
        backgroundThread.execute {
            Thread.sleep(5000L)
            loading.postValue(false)
            uiThread.post {
                commandQueue.sendEvent(Events.DoSomething())
                commandQueue.sendEvent(Events.DoOtherThing())
            }
        }
    }
}