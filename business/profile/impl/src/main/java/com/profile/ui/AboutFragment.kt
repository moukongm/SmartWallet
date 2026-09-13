package com.profile.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.example.business.profile.impl.R
import com.example.business.profile.impl.databinding.ProfileFragmentAboutBinding
import com.example.common.base.BaseFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class AboutFragment : BaseFragment<ProfileFragmentAboutBinding>() {

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?) =
        ProfileFragmentAboutBinding.inflate(inflater, container, false)

    override fun initView() {
        binding.profileAboutBack.setOnClickListener { findNavController().navigateUp() }
        val version = requireContext().packageManager
            .getPackageInfo(requireContext().packageName, 0)
            .versionName
        binding.profileAboutVersion.text = "版本 $version"
        binding.profileAboutPrivacyEntry.setOnClickListener {
            showDocument(R.string.profile_about_privacy, R.string.profile_about_privacy_content)
        }
        binding.profileAboutAgreementEntry.setOnClickListener {
            showDocument(R.string.profile_about_agreement, R.string.profile_about_agreement_content)
        }
    }

    override fun initData() = Unit

    private fun showDocument(title: Int, content: Int) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(content)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }
}
