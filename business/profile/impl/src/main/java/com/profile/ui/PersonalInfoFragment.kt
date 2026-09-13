package com.profile.ui

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.business.profile.impl.R
import com.example.business.profile.impl.databinding.ProfileFragmentPersonalInfoBinding
import com.example.common.base.BaseFragment
import com.profile.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch

class PersonalInfoFragment : BaseFragment<ProfileFragmentPersonalInfoBinding>() {

    private val viewModel: ProfileViewModel by lazy {
        ViewModelProvider(this)[ProfileViewModel::class.java]
    }
    private var avatarUri = ""
    private var profileRendered = false

    private val avatarPicker = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@registerForActivityResult
        requireContext().contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
        avatarUri = uri.toString()
        binding.profilePersonalAvatar.setImageURI(uri)
    }

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?) =
        ProfileFragmentPersonalInfoBinding.inflate(inflater, container, false)

    override fun initView() {
        binding.profilePersonalBack.setOnClickListener {
            findNavController().navigateUp()
        }
        binding.profilePersonalAvatarButton.setOnClickListener {
            avatarPicker.launch(arrayOf("image/*"))
        }
        binding.profilePersonalSaveButton.setOnClickListener {
            viewModel.updateProfile(
                binding.profilePersonalNickname.text?.toString().orEmpty(),
                binding.profilePersonalEmail.text?.toString().orEmpty(),
                avatarUri
            )
        }
    }

    override fun initData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.profile.collect { profile ->
                        if (profile != null && !profileRendered) {
                            profileRendered = true
                            avatarUri = profile.avatarUri
                            binding.profilePersonalNickname.setText(profile.nickname)
                            binding.profilePersonalEmail.setText(profile.email)
                            if (avatarUri.isNotBlank()) {
                                binding.profilePersonalAvatar.setImageURI(Uri.parse(avatarUri))
                            } else {
                                binding.profilePersonalAvatar.setImageResource(
                                    R.drawable.profile_page_ic_avatar
                                )
                            }
                        }
                    }
                }
                launch {
                    viewModel.profileSaved.collect {
                        Toast.makeText(requireContext(), "资料已保存", Toast.LENGTH_SHORT).show()
                        findNavController().navigateUp()
                    }
                }
            }
        }
    }
}
