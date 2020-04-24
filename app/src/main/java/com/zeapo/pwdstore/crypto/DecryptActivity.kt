package com.zeapo.pwdstore.crypto

import android.content.ClipData
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.format.DateUtils
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.CheckBox
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import com.github.ajalt.timberkt.Timber
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.zeapo.pwdstore.PasswordEntry
import com.zeapo.pwdstore.R
import com.zeapo.pwdstore.databinding.ActivityDecryptBinding
import com.zeapo.pwdstore.utils.Otp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.msfjarvis.openpgpktx.util.OpenPgpApi
import org.apache.commons.io.FileUtils
import org.openintents.openpgp.IOpenPgpService2
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Date

class DecryptActivity : BasePgpActivity() {

    private lateinit var binding: ActivityDecryptBinding

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
            try {
                cryptoPasswordLastChanged.text = resources.getString(R.string.last_changed,
                        getLastChangedString(intent.getLongExtra("LAST_CHANGED_TIMESTAMP", -1L)))
            } catch (e: Exception) {
                cryptoPasswordLastChanged.visibility = View.GONE
            }
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

    override fun onBound(service: IOpenPgpService2) {
        super.onBound(service)
        decryptAndVerify()
    }

    private fun decryptAndVerify(receivedIntent: Intent? = null) {
        val data = receivedIntent ?: Intent()
        data.action = OpenPgpApi.ACTION_DECRYPT_VERIFY

        val inputStream = FileUtils.openInputStream(File(fullPath))
        val outputStream = ByteArrayOutputStream()

        lifecycleScope.launch(Dispatchers.IO) {
            api?.executeApiAsync(data, inputStream, outputStream, object : OpenPgpApi.IOpenPgpCallback {
                override fun onReturn(result: Intent?) {
                    when (result?.getIntExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_ERROR)) {
                        OpenPgpApi.RESULT_CODE_SUCCESS -> with(binding) {
                            try {
                                val showPassword = settings.getBoolean("show_password", true)
                                val showExtraContent = settings.getBoolean("show_extra_content", true)

                                cryptoContainerDecrypt.visibility = View.VISIBLE

                                val monoTypeface = Typeface.createFromAsset(assets, "fonts/sourcecodepro.ttf")
                                val entry = PasswordEntry(outputStream)

                                passwordEntry = entry

                                if (intent.getStringExtra("OPERATION") == "EDIT") {
                                    edit()
                                    return
                                }

                                if (entry.password.isEmpty()) {
                                    cryptoPasswordShow.visibility = View.GONE
                                    cryptoPasswordShowLabel.visibility = View.GONE
                                } else {
                                    cryptoPasswordShowLabel.visibility = View.VISIBLE
                                    cryptoPasswordShow.visibility = View.VISIBLE
                                    cryptoPasswordShow.typeface = monoTypeface
                                    cryptoPasswordShow.text = entry.password
                                }
                                cryptoPasswordShow.typeface = monoTypeface
                                cryptoPasswordShow.text = entry.password

                                cryptoPasswordToggleShow.visibility = if (showPassword) View.GONE else View.VISIBLE

                                if (entry.hasExtraContent()) {
                                    cryptoExtraShow.typeface = monoTypeface
                                    cryptoExtraShow.text = entry.extraContent

                                    if (showExtraContent) {
                                        cryptoExtraShowLayout.visibility = View.VISIBLE
                                        cryptoExtraToggleShow.visibility = View.GONE
                                        cryptoExtraShow.transformationMethod = null
                                    } else {
                                        cryptoExtraShowLayout.visibility = View.GONE
                                        cryptoExtraToggleShow.visibility = View.VISIBLE
                                        cryptoExtraToggleShow.setOnCheckedChangeListener { _, _ ->
                                            cryptoExtraShow.text = entry.extraContent
                                        }

                                        cryptoExtraShow.transformationMethod = object : PasswordTransformationMethod() {
                                            override fun getTransformation(source: CharSequence, view: View): CharSequence {
                                                return if (cryptoExtraToggleShow.isChecked) source else super.getTransformation(source, view)
                                            }
                                        }
                                    }

                                    if (entry.hasUsername()) {
                                        cryptoUsernameShow.visibility = View.VISIBLE
                                        cryptoUsernameShowLabel.visibility = View.VISIBLE
                                        cryptoCopyUsername.visibility = View.VISIBLE

                                        cryptoCopyUsername.setOnClickListener { copyUsernameToClipBoard(entry.username!!) }
                                        cryptoUsernameShow.typeface = monoTypeface
                                        cryptoUsernameShow.text = entry.username
                                    } else {
                                        cryptoUsernameShow.visibility = View.GONE
                                        cryptoUsernameShowLabel.visibility = View.GONE
                                        cryptoCopyUsername.visibility = View.GONE
                                    }
                                }

                                if (entry.hasTotp() || entry.hasHotp()) {
                                    cryptoExtraShowLayout.visibility = View.VISIBLE
                                    cryptoExtraShow.typeface = monoTypeface
                                    cryptoExtraShow.text = entry.extraContent

                                    cryptoOtpShow.visibility = View.VISIBLE
                                    cryptoOtpShowLabel.visibility = View.VISIBLE
                                    cryptoCopyOtp.visibility = View.VISIBLE

                                    if (entry.hasTotp()) {
                                        cryptoCopyOtp.setOnClickListener {
                                            copyOtpToClipBoard(
                                                    Otp.calculateCode(
                                                            entry.totpSecret,
                                                            Date().time / (1000 * entry.totpPeriod),
                                                            entry.totpAlgorithm,
                                                            entry.digits)
                                            )
                                        }
                                        cryptoOtpShow.text =
                                                Otp.calculateCode(
                                                        entry.totpSecret,
                                                        Date().time / (1000 * entry.totpPeriod),
                                                        entry.totpAlgorithm,
                                                        entry.digits)
                                    } else {
                                        // we only want to calculate and show HOTP if the user requests it
                                        cryptoCopyOtp.setOnClickListener {
                                            if (settings.getBoolean("hotp_remember_check", false)) {
                                                if (settings.getBoolean("hotp_remember_choice", false)) {
                                                    calculateAndCommitHotp(entry)
                                                } else {
                                                    calculateHotp(entry)
                                                }
                                            } else {
                                                // show a dialog asking permission to update the HOTP counter in the entry
                                                val checkInflater = LayoutInflater.from(this@DecryptActivity)
                                                val checkLayout = checkInflater.inflate(R.layout.otp_confirm_layout, null)
                                                val rememberCheck: CheckBox =
                                                        checkLayout.findViewById(R.id.hotp_remember_checkbox)
                                                val dialogBuilder = MaterialAlertDialogBuilder(this@DecryptActivity)
                                                dialogBuilder.setView(checkLayout)
                                                dialogBuilder.setMessage(R.string.dialog_update_body)
                                                        .setCancelable(false)
                                                        .setPositiveButton(R.string.dialog_update_positive) { _, _ ->
                                                            run {
                                                                calculateAndCommitHotp(entry)
                                                                if (rememberCheck.isChecked) {
                                                                    settings.edit {
                                                                        putBoolean("hotp_remember_check", true)
                                                                        putBoolean("hotp_remember_choice", true)
                                                                    }
                                                                }
                                                            }
                                                        }
                                                        .setNegativeButton(R.string.dialog_update_negative) { _, _ ->
                                                            run {
                                                                calculateHotp(entry)
                                                                settings.edit {
                                                                    putBoolean("hotp_remember_check", true)
                                                                    putBoolean("hotp_remember_choice", false)
                                                                }
                                                            }
                                                        }
                                                val updateDialog = dialogBuilder.create()
                                                updateDialog.setTitle(R.string.dialog_update_title)
                                                updateDialog.show()
                                            }
                                        }
                                        cryptoOtpShow.setText(R.string.hotp_pending)
                                    }
                                    cryptoOtpShow.typeface = monoTypeface
                                } else {
                                    cryptoOtpShow.visibility = View.GONE
                                    cryptoOtpShowLabel.visibility = View.GONE
                                    cryptoCopyOtp.visibility = View.GONE
                                }

                                if (settings.getBoolean("copy_on_decrypt", true)) {
                                    copyPasswordToClipboard()
                                }
                            } catch (e: Exception) {
                                Timber.e(e) { "An Exception occurred" }
                            }
                        }
                        OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED -> handleUserInteractionRequest(result, REQUEST_DECRYPT)
                        OpenPgpApi.RESULT_CODE_ERROR -> handleError(result)
                    }
                }
            })
        }
    }

    /**
     * When done, this will launch an EncryptActivity.
     */
    private fun edit() {

    }

    /**
     * Gets a relative string describing when this shape was last changed
     * (e.g. "one hour ago")
     */
    private fun getLastChangedString(timeStamp: Long): CharSequence {
        if (timeStamp < 0) {
            throw RuntimeException()
        }

        return DateUtils.getRelativeTimeSpanString(this, timeStamp, true)
    }

}
