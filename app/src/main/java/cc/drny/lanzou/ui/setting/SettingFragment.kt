package cc.drny.lanzou.ui.setting

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.view.updatePadding
import androidx.navigation.fragment.findNavController
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import cc.drny.lanzou.R
import cc.drny.lanzou.data.lanzou.LanzouShareFile
import cc.drny.lanzou.util.dp2px
import cc.drny.lanzou.util.getNavigationBarHeight
import cc.drny.lanzou.util.showToast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tencent.mmkv.MMKV
import com.tencent.mmkv.MMKVHandler

class SettingFragment: PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preference_setting, rootKey)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listView.clipToPadding = false
        listView.updatePadding(bottom = getNavigationBarHeight() + 80.dp2px())

        findPreference<Preference>("app_history")?.setOnPreferenceClickListener {
            findNavController().navigate(SettingFragmentDirections
                .actionGlobalAnalyzeFolderDialogFragment(LanzouShareFile(
                    "https://jdy2002.lanzoub.com/b041496oj",
                    "123456"
                )))
            true
        }

        findPreference<Preference>("join_qq_group")?.setOnPreferenceClickListener {
            joinQQGroup()
            true
        }

        val openSources = arrayOf(
            "OkHttp",
            "Navigation",
            "MMKV",
            "Retrofit",
            "Jsoup",
            "Glide",
            "Litepal",
            "PermissionX"
        )

        findPreference<Preference>("open_source")?.setOnPreferenceClickListener {
            MaterialAlertDialogBuilder(requireContext()).apply {
                setTitle("第三方开源库")
                setItems(openSources, null)
                setPositiveButton("关闭", null)
                show()
            }
            true
        }
    }

    private fun joinQQGroup() {
        try {
            val intent = Intent()
            intent.data =
                Uri.parse("mqqopensdkapi://bizAgent/qm/qr?url=http%3A%2F%2Fqm.qq.com%2Fcgi-bin%2Fqm%2Fqr%3Ffrom%3Dapp%26p%3Dandroid%26jump_from%3Dwebapi%26k%3D" + "al1cX20hNcmhbnH7O9yJFx9SiTi5zUmt")
            // 此Flag可根据具体产品需要自定义，如设置，则在加群界面按返回，返回手Q主界面，不设置，按返回会返回到呼起产品界面
            // intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } catch (e: Exception) {
            "请先安装QQ".showToast()
        }
    }

}