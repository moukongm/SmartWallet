package com.profile.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.business.profile.impl.databinding.ProfileFragmentBackupBinding
import com.example.common.base.BaseFragment
import com.profile.viewmodel.DataBackupViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DataBackupFragment : BaseFragment<ProfileFragmentBackupBinding>() {

    private val viewModel: DataBackupViewModel by lazy {
        ViewModelProvider(
            this,
            DataBackupViewModel.Factory()
        )[DataBackupViewModel::class.java]
    }

    private val exportFile = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri ?: return@registerForActivityResult
        runTask {
            val content = viewModel.createBackup()
            withContext(Dispatchers.IO) {
                requireContext().contentResolver.openOutputStream(uri)?.bufferedWriter()
                    ?.use { it.write(content) }
            }
            "备份导出成功"
        }
    }

    private val importFile = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@registerForActivityResult
        runTask {
            val content = withContext(Dispatchers.IO) {
                requireContext().contentResolver.openInputStream(uri)?.bufferedReader()
                    ?.use { it.readText() }.orEmpty()
            }
            "已恢复 ${viewModel.restoreBackup(content)} 条账单"
        }
    }

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?) =
        ProfileFragmentBackupBinding.inflate(inflater, container, false)

    override fun initView() {
        binding.profileBackupBack.setOnClickListener { findNavController().navigateUp() }
        binding.profileBackupExportButton.setOnClickListener {
            val date = SimpleDateFormat("yyyyMMdd", Locale.CHINA).format(Date())
            exportFile.launch("smartwallet-backup-$date.json")
        }
        binding.profileBackupImportButton.setOnClickListener {
            importFile.launch(arrayOf("application/json", "text/*"))
        }
    }

    override fun initData() = Unit

    private fun runTask(block: suspend () -> String) {
        viewLifecycleOwner.lifecycleScope.launch {
            binding.profileBackupProgress.visibility = View.VISIBLE
            try {
                binding.profileBackupStatus.text = block()
            } catch (exception: Exception) {
                binding.profileBackupStatus.text =
                    exception.message ?: "操作失败"
            } finally {
                binding.profileBackupProgress.visibility = View.GONE
            }
        }
    }
}
