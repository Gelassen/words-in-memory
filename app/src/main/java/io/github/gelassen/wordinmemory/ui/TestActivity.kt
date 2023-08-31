package io.github.gelassen.wordinmemory.ui

import android.R.attr.text
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import io.github.gelassen.wordinmemory.App


class TestActivity: AppCompatActivity() {

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        runTestIntent {
            runTestIntent6()
            /*runTestIntent5()*/
            /*runTestIntent4()*/
            /*runTestIntent3()*/
            /*runTestIntent2()*/
            /*runTestIntent1()*/
        }
    }

    private fun runTestIntent6() {
        val intent = Intent()
        intent.action = Intent.ACTION_PROCESS_TEXT
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, true)
        intent.putExtra(Intent.EXTRA_PROCESS_TEXT, "hello")
        startActivity(intent)
    }
    private fun runTestIntent5() {
        val intent = Intent()
        intent.type = "text/plain"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            intent.action = Intent.ACTION_PROCESS_TEXT
            intent.putExtra(Intent.EXTRA_PROCESS_TEXT, text)
        } else {
            intent.action = Intent.ACTION_SEND
            intent.putExtra(Intent.EXTRA_TEXT, text)
        }

        for (resolveInfo in packageManager.queryIntentActivities(intent, 0)) {
            if (resolveInfo.activityInfo.packageName.contains("com.google.android.apps.translate")) {
                intent.component = ComponentName(
                    resolveInfo.activityInfo.packageName,
                    resolveInfo.activityInfo.name
                )
                startActivity(intent)
            }
        }
    }

    private fun runTestIntent4() {
        val intent = Intent()
        intent.action = Intent.ACTION_VIEW
        intent.setPackage("com.google.android.apps.translate")

        val uri = Uri.Builder()
            .scheme("http")
            .authority("translate.google.com")
            .path("/m/translate")
            .appendQueryParameter(
                "q",
                "c'est l'meunier Mathurin qui caresse les filles au tic-tac du moulin"
            )
            .appendQueryParameter("tl", "pl") // target language
            .appendQueryParameter("sl", "fr") // source language
            .build()
        //intent.setType("text/plain"); //not needed, but possible
        //intent.setType("text/plain"); //not needed, but possible
        intent.data = uri
    }
    private fun runTestIntent3() {
        val intent = Intent()
        intent.action = Intent.ACTION_VIEW
//        intent.setPackage("com.google.android.apps.translate")
        intent.setData(Uri.parse("android-app://com.google.android.apps.translate"))
        startActivity(intent)
    }
    @RequiresApi(Build.VERSION_CODES.M)
    private fun runTestIntent2() {
        val textToTranslate = "Hello world!"
        val translateIntent = Intent()
        translateIntent.setComponent(
            ComponentName(
                "com.google.android.apps.translate",
                "com.google.android.apps.translate.QuickTranslateActivity"
            )
        )
        translateIntent.setAction(Intent.ACTION_PROCESS_TEXT)
        translateIntent.putExtra(Intent.EXTRA_PROCESS_TEXT, textToTranslate)
        startActivity(translateIntent)
    }

    private fun runTestIntent1() {
        val intent = Intent()
        intent.action = Intent.ACTION_SEND
        intent.putExtra(Intent.EXTRA_TEXT, "hello")
        intent.putExtra("key_text_input", "hello")
        intent.putExtra("key_text_output", "")
        intent.putExtra("key_language_from", "en")
        intent.putExtra("key_language_to", "zh")
        intent.putExtra("key_suggest_translation", "")
        intent.putExtra("key_from_floating_window", false)
        intent.component = ComponentName(
            "com.google.android.apps.translate",  //Change is here
            //"com.google.android.apps.translate.HomeActivity"));
            "com.google.android.apps.translate.TranslateActivity"
        )
        startActivity(intent)
    }
    private fun runTestIntent(codeBlock: () -> Unit) {
        try {
            codeBlock()
        } catch (e: Exception) {
            Log.e(App.TAG, "Failed to launch google translate", e)
            // TODO Auto-generated catch block
            Toast.makeText(
                application, "Sorry, No Google Translation Installed",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}