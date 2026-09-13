package com.profile.ui

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavDeepLinkRequest
import androidx.navigation.fragment.findNavController
import com.alibaba.android.arouter.launcher.ARouter
import com.example.business.profile.impl.R
import com.example.business.profile.impl.databinding.ProfileFragmentBinding
import com.example.common.base.BaseFragment
import com.example.common.router.RouterPath
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.profile.data.UserProfile
import com.profile.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch

class ProfileFragment : BaseFragment<ProfileFragmentBinding>() {

    private var isNavigating = false

    private val viewModel: ProfileViewModel by lazy {
        ViewModelProvider(this)[ProfileViewModel::class.java]
    }

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?) =
        ProfileFragmentBinding.inflate(inflater, container, false)

    override fun initView() {
        binding.profilePageEditButton.setOnClickListener { openPage("personal-info") }
        binding.profilePersonalInfoEntry.setOnClickListener { openPage("personal-info") }
        binding.profileCategoryManagementEntry.setOnClickListener {
            openPage("category-management")
        }
        binding.profileNotificationEntry.setOnClickListener { openPage("notification") }
        binding.profileBackupEntry.setOnClickListener { openPage("backup") }
        binding.profileFileEntry.setOnClickListener { openPage("files") }
        binding.profileAboutEntry.setOnClickListener { openPage("about") }
        binding.profilePageLogoutButton.setOnClickListener { showLogoutDialog() }
    }

    override fun initData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.profile.collect { it?.let(::renderProfile) }
                }
                launch {
                    viewModel.loggedOut.collect { if (it) openLoginPage() }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        isNavigating = false
        viewModel.refreshProfile()
    }

    private fun renderProfile(profile: UserProfile) {
        binding.profilePageUserName.text = profile.nickname
        binding.profilePageUserEmail.text = profile.email
        if (profile.avatarUri.isNotBlank()) {
            binding.profilePageAvatar.setImageURI(Uri.parse(profile.avatarUri))
        } else {
            binding.profilePageAvatar.setImageResource(R.drawable.profile_page_ic_avatar)
        }
    }

    private fun openPage(path: String) {
        if (isNavigating) return
        isNavigating = true
        findNavController().navigate(
            NavDeepLinkRequest.Builder
                .fromUri(Uri.parse("smartwallet://profile/$path"))
                .build()
        )
    }

    private fun showLogoutDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.profile_page_logout)
            .setMessage(R.string.profile_page_logout_message)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.profile_page_logout) { _, _ -> viewModel.logout() }
            .show()
    }

    private fun openLoginPage() {
        ARouter.getInstance().build(RouterPath.USER_LOGIN_ACTIVITY).navigation()
        requireActivity().finish()
    }
}
