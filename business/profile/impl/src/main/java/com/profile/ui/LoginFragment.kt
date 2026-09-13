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
import com.example.business.profile.impl.databinding.ProfileFragmentLoginBinding
import com.example.common.base.BaseFragment
import com.profile.viewmodel.LoginViewModel
import kotlinx.coroutines.launch

class LoginFragment : BaseFragment<ProfileFragmentLoginBinding>() {

    private val viewModel: LoginViewModel by lazy {
        ViewModelProvider(requireActivity())[LoginViewModel::class.java]
    }

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
    ): ProfileFragmentLoginBinding {
        return ProfileFragmentLoginBinding.inflate(inflater, container, false)
    }

    override fun initView() {
        binding.profileRegisterEntry.setOnClickListener {
            (requireActivity() as LoginActivity).showRegister()
        }

        binding.profileLoginButton.setOnClickListener {
            submitLogin()
        }
    }

    override fun initData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.loginState.collect(::renderLoginState)
            }
        }
    }

    private fun submitLogin() {
        val email = binding.profileLoginAccountInput.text
            ?.toString()
            ?.trim()
            .orEmpty()
        val password = binding.profileLoginPasswordInput.text
            ?.toString()
            .orEmpty()

        binding.profileLoginAccountContainer.error = null
        binding.profileLoginPasswordContainer.error = null

        var valid = true
        if (email.isBlank()) {
            binding.profileLoginAccountContainer.error =
                getString(R.string.profile_auth_email_required)
            valid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.profileLoginAccountContainer.error =
                getString(R.string.profile_auth_email_invalid)
            valid = false
        }

        if (password.isBlank()) {
            binding.profileLoginPasswordContainer.error =
                getString(R.string.profile_auth_password_required)
            valid = false
        }

        if (valid) {
            viewModel.login(email, password)
        }
    }

    private fun renderLoginState(state: LoginViewModel.AuthUiState) {
        when (state) {
            LoginViewModel.AuthUiState.Idle -> {
                setLoading(false)
            }

            LoginViewModel.AuthUiState.Loading -> {
                setLoading(true)
            }

            is LoginViewModel.AuthUiState.Success -> {
                setLoading(false)
                viewModel.consumeLoginState()
                (requireActivity() as LoginActivity).openMainPage()
            }

            is LoginViewModel.AuthUiState.Error -> {
                setLoading(false)
                Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                viewModel.consumeLoginState()
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.profileLoginButton.isEnabled = !loading
        binding.profileLoginAccountInput.isEnabled = !loading
        binding.profileLoginPasswordInput.isEnabled = !loading
        binding.profileLoginButton.setText(
            if (loading) {
                R.string.profile_auth_login_loading
            } else {
                R.string.profile_auth_login_action
            }
        )
    }
}
