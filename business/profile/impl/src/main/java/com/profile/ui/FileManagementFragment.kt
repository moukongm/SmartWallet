package com.profile.ui

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.business.bill.api.model.Bill
import com.example.business.profile.impl.databinding.ProfileFragmentFilesBinding
import com.example.common.base.BaseFragment
import com.profile.viewmodel.FileManagementViewModel
import kotlinx.coroutines.launch

class FileManagementFragment : BaseFragment<ProfileFragmentFilesBinding>() {

    private val viewModel: FileManagementViewModel by lazy {
        ViewModelProvider(
            this,
            FileManagementViewModel.Factory()
        )[FileManagementViewModel::class.java]
    }
    private lateinit var fileAdapter: ProfileFileAdapter

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?) =
        ProfileFragmentFilesBinding.inflate(inflater, container, false)

    override fun initView() {
        fileAdapter = ProfileFileAdapter(
            onOpen = ::openFile,
            onRemove = { bill -> viewModel.removeAttachment(bill) }
        )
        binding.profileFilesBack.setOnClickListener { findNavController().navigateUp() }
        binding.profileFilesList.layoutManager = LinearLayoutManager(requireContext())
        binding.profileFilesList.adapter = fileAdapter
    }

    override fun initData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    fileAdapter.submitList(state.files)
                    binding.profileFilesProgress.visibility =
                        if (state.loading) View.VISIBLE else View.GONE
                    binding.profileFilesEmpty.visibility =
                        if (!state.loading && state.files.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }

    private fun openFile(bill: Bill) {
        startActivity(
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse(bill.imageUri), "image/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        )
    }
}
