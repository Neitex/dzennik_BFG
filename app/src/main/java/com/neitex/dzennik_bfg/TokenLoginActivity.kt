package com.neitex.dzennik_bfg

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.util.Base64
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider.getUriForFile
import com.neitex.dzennik_bfg.shared_functions.isValidToken
import com.neitex.dzennik_bfg.shared_functions.makeSnackbar
import com.neitex.dzennik_bfg.shared_functions.saveAccountInfo
import com.neitex.dzennik_bfg.shared_functions.savePrivacyInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.security.KeyPair
import java.security.KeyPairGenerator
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
import kotlin.random.Random


class TokenLoginActivity : AppCompatActivity() {
    private val PickTokenFileRequestCode = 5

    private lateinit var decodedJSONObject: JSONObject
    private lateinit var keyPair: KeyPair
    private var publicKeyFileName: String = "null"
    private lateinit var vieww: View
    private lateinit var preferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_token_login)
        val keyGenerator = KeyPairGenerator.getInstance("RSA")
        keyGenerator.initialize(2048)
        keyPair = keyGenerator.genKeyPair()
        vieww = findViewById(R.id.shareEncryptionFile)
        initButtons(findViewById(R.id.shareEncryptionFile), findViewById(R.id.selectTokenFile))
        preferences = getSharedPreferences("data", MODE_PRIVATE)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        this.finishActivity(0)
    }

    private fun initButtons(shareEncryptionFileButton: Button, selectTokenButton: Button) {
        shareEncryptionFileButton.setOnClickListener {
            if (requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, this, this)) {
                if (publicKeyFileName == "null") {
                    publicKeyFileName = Random.nextLong().toString() + ".dze"
                }
                val publicKeyFile =
                    File(
                        this.cacheDir,
                        publicKeyFileName
                    )
                if (!publicKeyFile.exists()) {
                    publicKeyFile.createNewFile()
                    publicKeyFile.writeText("----DZENNIK TOKEN SHARE PUBLIC KEY----\n")
                    publicKeyFile.appendText(
                        Base64.encodeToString(
                            keyPair.public.encoded,
                            Base64.DEFAULT
                        )
                    )
                    publicKeyFile.appendText("----END OF DZENNIK TOKEN SHARE KEY----")
                }
                val sharingIntent = Intent(Intent.ACTION_SEND)
                sharingIntent.type = "application/dzennik_token"
                sharingIntent.putExtra(
                    Intent.EXTRA_STREAM,
                    getUriForFile(
                        this,
                        "$packageName.provider",
                        publicKeyFile
                    )
                )
                sharingIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
                startActivity(
                    Intent.createChooser(
                        sharingIntent,
                        getString(R.string.send_encryption_intent_name)
                    )
                )
            }
        }

        selectTokenButton.setOnClickListener {
            if (requestPermission(Manifest.permission.READ_EXTERNAL_STORAGE, this, this)) {
                openFile(Uri.parse(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath))
            }
        }
    }

    private fun requestPermission(
        permission: String,
        context: Context,
        activity: Activity
    ): Boolean {
        val isGranted = ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
        if (!isGranted) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(permission),
                0
            )
        }
        return isGranted
    }

    private fun openFile(pickerInitialUri: Uri) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri)
        }

        startActivityForResult(intent, PickTokenFileRequestCode)
    }

    private fun checkData(uri: Uri): Boolean {
        val fileDescriptor = contentResolver.openInputStream(uri) ?: return false
        val file = File.createTempFile(Random.nextInt().toString(), ".dze")
        file.writeText(String(fileDescriptor.readBytes()))
        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(Cipher.DECRYPT_MODE, keyPair.private)
        var str = String(file.readBytes())
        try {
            if (!str.startsWith("----BEGIN DZENNIK TOKEN----") && !str.endsWith("----END DZENNIK TOKEN----")) {
                throw IllegalArgumentException()
            }
            str = str.drop(28).dropLast(26)
            val decodedString = String(cipher.doFinal(Base64.decode(str, Base64.DEFAULT)))
            val parsedJSONObject = JSONObject(decodedString)
            if (!parsedJSONObject.has("token") ||
                !parsedJSONObject.has("privacy")
            ) {
                return false
            }

            decodedJSONObject = parsedJSONObject
        } catch (e: IllegalArgumentException) {
            return false
        } catch (e: JSONException) {
            return false
        } catch (e: IllegalBlockSizeException) {
            return false
        } catch (e: BadPaddingException) {
            return false
        }
        return true
    }

    private fun deleteCache() {
        this.cacheDir.deleteRecursively()
    }

    private fun setLoginEnabled(isEnabled: Boolean) {
        val button = findViewById<Button>(R.id.applyTokenButton)
        button.isEnabled = isEnabled
        button.setOnClickListener {
            GlobalScope.launch(Dispatchers.IO) {
                if (!isValidToken(decodedJSONObject.getString("token"))) {
                    runOnUiThread {
                        makeSnackbar(
                            findViewById<Button>(R.id.applyTokenButton),
                            getString(R.string.invalid_shared_token)
                        )
                    }
                } else {
                    runOnUiThread {
                        it.isEnabled = false
                        makeSnackbar(
                            findViewById(R.id.applyTokenButton),
                            getString(R.string.token_successfully_unencrypted),
                            resources.getColor(R.color.lightBlue)
                        )
                    }
                    saveAccountInfo(
                        decodedJSONObject.getString("token"),
                        getSharedPreferences("data", MODE_PRIVATE)
                    )
                    deleteCache()
                    val privacySettings: JSONObject = decodedJSONObject.getJSONObject("privacy")
                    savePrivacyInfo(preferences, privacySettings)
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            PickTokenFileRequestCode -> {
                if (resultCode == Activity.RESULT_OK) {
                    if (data != null) {
                        val uri = data.data
                        if (uri != null) {
                            if (!checkData(
                                    uri.normalizeScheme()
                                )
                            ) {
                                makeSnackbar(
                                    findViewById<Button>(R.id.applyTokenButton),
                                    getString(R.string.invalid_shared_token_file)
                                )
                                setLoginEnabled(false)
                            } else {
                                makeSnackbar(
                                    findViewById<Button>(R.id.applyTokenButton),
                                    "Successfully unencrypted token!",
                                    resources.getColor(R.color.lightBlue)
                                )
                                setLoginEnabled(true)
                            }
                        }
                    }
                }
            }
        }
    }
}