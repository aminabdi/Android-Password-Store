package com.zeapo.pwdstore.crypto

import android.content.ClipData
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.zeapo.pwdstore.R
import com.zeapo.pwdstore.databinding.ActivityDecryptBinding

class DecryptActivity : BasePgpActivity() {

    lateinit var binding: ActivityDecryptBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDecryptBinding.inflate(layoutInflater)
        with(binding) {
            cryptoPasswordCategoryDecrypt.text = relativeParentPath
            cryptoPasswordFile.text = name
            cryptoPasswordFile.setOnLongClickListener {
                val clipboard = clipboard ?: return@setOnLongClickListener false
                val clip = ClipData.newPlainText("pgp_handler_result_pm", name)
                clipboard.setPrimaryClip(clip)
                showSnackbar(resources.getString(R.string.clipboard_username_toast_text))
                true
            }
            val lastChanged = try {
                resources.getString(R.string.last_changed, lastChangedString)
            } catch (e: Exception) {
                showSnackbar(getString(R.string.get_last_changed_failed))
                null
            }
            if (lastChanged != null)
                cryptoPasswordLastChanged.text = lastChanged
            else
                cryptoPasswordLastChanged.visibility = View.GONE
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.pgp_handler, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.copy_password -> copyPasswordToClipboard()
            R.id.edit_password -> edit()
            R.id.share_password_as_plaintext -> shareAsPlaintext()
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * When done, this will launch an EncryptActivity.
     */
    private fun edit() {

    }
}
