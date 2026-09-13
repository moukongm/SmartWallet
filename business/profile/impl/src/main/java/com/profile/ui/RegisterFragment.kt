package com.profile.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import android.util.Patterns
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.business.profile.impl.R
import com.example.business.profile.impl.databinding.ProfileFragmentRegisterBinding
import com.example.common.base.BaseFragment
import com.profile.viewmodel.LoginViewModel
import kotlinx.coroutines.launch

class RegisterFragment : BaseFragment<ProfileFragmentRegisterBinding>() {

    private val viewModel: LoginViewModel by lazy {
        ViewModelProvider(requireActivity())[LoginViewModel::class.java]
    }

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
    ): ProfileFragmentRegisterBinding {
        return ProfileFragmentRegisterBinding.inflate(inflater, container, false)
    }

    override fun initView() {
        binding.profileRegisterButton.setOnClickListener {
            submitRegister()
        }
    }

    override fun initData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.registerState.collect(::renderRegisterState)
            }
        }
    }

    private fun submitRegister() {
        val email = binding.profileRegisterAccountInput.text
            ?.toString()
            ?.trim()
            .orEmpty()
        val password = binding.profileRegisterPasswordInput.text
            ?.toString()
            .orEmpty()
        val confirmPassword = binding.profileRegisterPasswordConfirmInput.text
            ?.toString()
            .orEmpty()

        binding.profileRegisterAccountContainer.error = null
        binding.profileRegisterPasswordContainer.error = null
        binding.profileRegisterPasswordConfirmContainer.error = null

        var valid = true
        if (email.isBlank()) {
            binding.profileRegisterAccountContainer.error =
                getString(R.string.profile_auth_email_required)
            valid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.profileRegisterAccountContainer.error =
                getString(R.string.profile_auth_email_invalid)
            valid = false
        }

        if (password.isBlank()) {
            binding.profileRegisterPasswordContainer.error =
                getString(R.string.profile_auth_password_required)
            valid = false
        } else if (password.length < MIN_PASSWORD_LENGTH) {
            binding.profileRegisterPasswordContainer.error =
                getString(R.string.profile_auth_password_too_short)
            valid = false
        }

        if (confirmPassword.isBlank()) {
            binding.profileRegisterPasswordConfirmContainer.error =
                getString(R.string.profile_auth_password_confirm_required)
            valid = false
        } else if (password != confirmPassword) {
            binding.profileRegisterPasswordConfirmContainer.error =
                getString(R.string.profile_auth_password_mismatch)
            valid = false
        }

        if (!binding.profileRegisterAgreementCheckbox.isChecked) {
            Toast.makeText(
                requireContext(),
                R.string.profile_auth_agreement_required,
                Toast.LENGTH_SHORT
            ).show()
            valid = false
        }

        if (valid) {
            val nickname = email.substringBefore("@")
            viewModel.register(email, password, nickname)
        }
    }

    private fun renderRegisterState(state: LoginViewModel.AuthUiState) {
        when (state) {
            LoginViewModel.AuthUiState.Idle -> {
                setLoading(false)
            }

            LoginViewModel.AuthUiState.Loading -> {
                setLoading(true)
            }

            is LoginViewModel.AuthUiState.Success -> {
                setLoading(false)
                viewModel.consumeRegisterState()
                if (state.authenticated) {
                    (requireActivity() as LoginActivity).openMainPage()
                } else {
                    Toast.makeText(
                        requireContext(),
                        R.string.profile_auth_register_verify_email,
                        Toast.LENGTH_LONG
                    ).show()
                    (requireActivity() as LoginActivity).showLogin()
                }
            }

            is LoginViewModel.AuthUiState.Error -> {
                setLoading(false)
                Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                viewModel.consumeRegisterState()
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.profileRegisterButton.isEnabled = !loading
        binding.profileRegisterAccountInput.isEnabled = !loading
        binding.profileRegisterPasswordInput.isEnabled = !loading
        binding.profileRegisterPasswordConfirmInput.isEnabled = !loading
        binding.profileRegisterAgreementCheckbox.isEnabled = !loading
        binding.profileRegisterButton.setText(
            if (loading) {
                R.string.profile_auth_register_loading
            } else {
                R.string.profile_auth_register_action
            }
        )
    }

    private companion object {
        const val MIN_PASSWORD_LENGTH = 6
    }
}
