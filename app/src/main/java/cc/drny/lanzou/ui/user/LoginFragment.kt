package cc.drny.lanzou.ui.user

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import cc.drny.lanzou.LanzouApplication
import cc.drny.lanzou.R
import cc.drny.lanzou.data.user.User
import cc.drny.lanzou.databinding.FragmentLoginBinding

class LoginFragment: Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel by viewModels<LoginViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val navController = findNavController()
        viewModel.liveData.observe(viewLifecycleOwner) {
            it.onSuccess {
                val userFragment = navController.getBackStackEntry(R.id.userFragment)
                val fileFragment = navController.getBackStackEntry(R.id.fileFragment)
                fileFragment.savedStateHandle["isLogin"] = true
                userFragment.savedStateHandle["isLogin"] = true
                navController.popBackStack()
            }.onFailure { throwable ->
                Log.d("jdy", throwable.toString())
                binding.editUsername.error = throwable.message
            }
        }

        binding.btnLogin.setOnClickListener {
            val username = binding.editUsername.text!!.trim().toString()
            val password = binding.editPassword.text!!.trim().toString()
            if (username.isEmpty()) {
                binding.editUsername.error = "用户名不能为空"
            } else if (password.isEmpty()) {
                binding.editPassword.error = "密码不能为空"
            } else {
                viewModel.login(User(username, password))
            }
        }

        binding.btnRegister.setOnClickListener {
            navController.navigate(LoginFragmentDirections
                .actionLoginFragmentToWebViewFragment(LanzouApplication.LANZOU_HOST_REGISTER, false))
        }

        binding.btnForgetPwd.setOnClickListener {
            navController.navigate(LoginFragmentDirections
                .actionLoginFragmentToWebViewFragment(LanzouApplication.LANZOU_HOST_FORGET, false))
        }

        binding.btnBrowserLogin.setOnClickListener {
            navController.navigate(LoginFragmentDirections
                .actionLoginFragmentToWebViewFragment(LanzouApplication.LANZOU_HOST_LOGIN, true))
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}