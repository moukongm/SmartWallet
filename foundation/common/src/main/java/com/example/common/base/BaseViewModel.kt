package com.example.common.base

import androidx.lifecycle.ViewModel

import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable

open class BaseViewModel : ViewModel() {
    private  val composoteDispose = CompositeDisposable()

    protected fun addDisposable(disposable: Disposable) {
        composoteDispose.add(disposable)
    }

    override fun onCleared() {
        super.onCleared()
        composoteDispose.clear()
    }
}