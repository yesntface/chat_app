package com.example.chatapp.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.chatapp.R
import com.example.chatapp.dialogs.ProgressBarDialog
import com.example.chatapp.dto.SignInDto
import com.example.chatapp.extensions.validEmail
import com.example.chatapp.messages.LatestMessageActivity
import com.example.chatapp.view_model.AuthViewModel

class LoginActivity: AppCompatActivity() {

    private val authViewModel: AuthViewModel = AuthViewModel()
    lateinit var myProgressBar: ProgressBarDialog
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var email: String
    private lateinit var password: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        myProgressBar = ProgressBarDialog(this)

        emailEditText = findViewById(R.id.email_editText_login)
        passwordEditText = findViewById(R.id.password_editText_login)

        findViewById<Button>(R.id.login_button_login).setOnClickListener {
            email = emailEditText.text.toString()
            password = passwordEditText.text.toString()

            if(validateEmailAndPassword(email, password)) {
                myProgressBar.show()
                performLogin()
            }

        }

        findViewById<TextView>(R.id.back_to_register_textview).setOnClickListener{
            finish()
        }
    }

    private fun performLogin() {
        authViewModel.signIn(SignInDto(email, password))
        authViewModel.signInLiveData.observe(
            this,
            {
                if(it.success) {
                    myProgressBar.dismiss()
                    val intent = Intent(this, LatestMessageActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                } else {
                    Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun validateEmailAndPassword(email: String, password: String): Boolean {
        if(!validEmail(email)) {
            Toast.makeText(this, "Wrong Email Format", Toast.LENGTH_LONG).show()
            return false
        }

        if(password.length <= 5) {
            Toast.makeText(this, "Wrong Password Format", Toast.LENGTH_LONG).show()
            return false
        }
        return true
    }
}