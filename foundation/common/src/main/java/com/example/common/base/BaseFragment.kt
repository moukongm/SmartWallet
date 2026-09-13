package com.example.common.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.example.common.preload.LayoutPreloadOwner

abstract class BaseFragment<VB : ViewBinding> : Fragment() {

    private var _binding: VB? = null
    protected val binding: VB
        get() = requireNotNull(_binding) {
            "只能在 onCreateView 和 onDestroyView 之间访问 binding"
        }

    protected fun getBindingSafe(): VB? = _binding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = getViewBinding(inflater, container)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initData()
        requestTouchTargetInspection(view)
    }

    override fun onResume() {
        super.onResume()
        view?.let(::requestTouchTargetInspection)
    }

    override fun onDestroyView() {
        (activity as? BaseActivity<*>)?.requestTouchTargetInspection()
        _binding = null
        super.onDestroyView()
    }

    protected fun takePreloadedLayout(
        @LayoutRes layoutRes: Int
    ): View? {
        val preloadOwner = activity as? LayoutPreloadOwner ?: return null
        return preloadOwner
            .layoutPreloader
            .take(layoutRes)
    }

    private fun requestTouchTargetInspection(root: View) {
        root.post {
            if (isAdded) {
                (activity as? BaseActivity<*>)?.requestTouchTargetInspection()
            }
        }
    }

    abstract fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): VB

    abstract fun initView()

    abstract fun initData()
}
