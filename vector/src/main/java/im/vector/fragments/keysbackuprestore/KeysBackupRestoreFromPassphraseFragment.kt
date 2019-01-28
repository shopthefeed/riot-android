/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package im.vector.fragments.keysbackuprestore

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.os.Bundle
import android.support.design.widget.TextInputLayout
import android.text.Editable
import android.text.InputType
import android.text.SpannableString
import android.text.style.ClickableSpan
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import androidx.core.text.set
import butterknife.BindView
import butterknife.OnClick
import butterknife.OnTextChanged
import im.vector.R
import im.vector.activity.MXCActionBarActivity
import im.vector.fragments.VectorBaseFragment
import org.matrix.androidsdk.MXSession

class KeysBackupRestoreFromPassphraseFragment : VectorBaseFragment() {

    override fun getLayoutResId() = R.layout.fragment_keys_backup_restore_from_passphrase

    private lateinit var viewModel: KeysBackupRestoreFromPassphraseViewModel

    @BindView(R.id.keys_backup_passphrase_enter_til)
    lateinit var mPassphraseInputLayout: TextInputLayout
    @BindView(R.id.keys_backup_passphrase_enter_edittext)
    lateinit var mPassphraseTextEdit: EditText

    @BindView(R.id.keys_backup_passphrase_help_with_link)
    lateinit var helperTextWithLink: TextView

    @OnClick(R.id.keys_backup_view_show_password)
    fun toggleVisibilityMode() {
        viewModel.showPasswordMode.value = !(viewModel.showPasswordMode.value ?: false)
    }


    lateinit var mInteractionListener: InteractionListener

    companion object {
        fun newInstance() = KeysBackupRestoreFromPassphraseFragment()
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is InteractionListener) {
            mInteractionListener = context
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel = ViewModelProviders.of(this).get(KeysBackupRestoreFromPassphraseViewModel::class.java)

        viewModel.passphraseErrorText.observe(this, Observer { newValue ->
            mPassphraseInputLayout.error = newValue
        })

        helperTextWithLink.text = spannableStringForHelperText(context!!)

        helperTextWithLink.setOnClickListener {
            mInteractionListener.didSelectRecoveryKeyMode()
        }

        viewModel.isRestoring.observe(this, Observer {
            val isLoading = it ?: false
            if (isLoading) mInteractionListener.setShowWaitingView(context?.getString(R.string.keys_backup_restoring_waiting_message)) else mInteractionListener.setHideWaitingView()
        })

        viewModel.showPasswordMode.observe(this, Observer {
            val shouldBeVisible = it ?: false
            if (shouldBeVisible) {
                mPassphraseTextEdit.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
             } else {
                mPassphraseTextEdit.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            mPassphraseTextEdit.setSelection(viewModel.passphrase.value?.length ?: 0)
        })

        mPassphraseTextEdit.setOnEditorActionListener { tv, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                onNext()
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }

    }

    private fun spannableStringForHelperText(context: Context): SpannableString {
        val tapableText = context.getString(R.string.keys_backup_restore_use_recovery_key)
        val helperText = context.getString(R.string.keys_backup_restore_with_passphrase_helper_with_link, tapableText)

        val spanString = SpannableString(helperText)
        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View?) {

            }
        }
        val start = helperText.indexOf(tapableText)
        val end = start + tapableText.length
        spanString[start, end] = clickableSpan
        return spanString
    }

    @OnTextChanged(R.id.keys_backup_passphrase_enter_edittext)
    fun onPassphraseTextEditChange(s: Editable?) {
        s?.toString()?.let { viewModel.updatePassphrase(it) }
    }

    @OnClick(R.id.keys_backup_setup_step2_button)
    fun onNext() {
        val value = viewModel.passphrase.value
        if (value.isNullOrBlank()) {
            viewModel.passphraseErrorText.value = context?.getString(R.string.keys_backup_passphrase_empty_error_message)
        } else {
            viewModel.recoverKeys(context!!, mInteractionListener.getSession(), mInteractionListener.getKeysVersion())
        }
    }

    interface InteractionListener {
        fun didSelectRecoveryKeyMode()
        fun getSession(): MXSession
        fun getKeysVersion(): String
        fun setShowWaitingView(status: String?)
        fun setHideWaitingView()
    }
}