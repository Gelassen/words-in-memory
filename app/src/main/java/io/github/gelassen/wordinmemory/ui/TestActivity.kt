package io.github.gelassen.wordinmemory.ui

import android.R.attr.text
import android.content.ComponentName
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Paint.Align
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.mlkit.vision.text.Text
import io.github.gelassen.wordinmemory.App
import io.github.gelassen.wordinmemory.R
import io.github.gelassen.wordinmemory.ml.Translation


class TestActivity: AppCompatActivity(), OnSuccessListener<Text>, OnFailureListener {

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)

        val translation = Translation()
        translation.run(
            "因为你看看了我的出版物，请告诉我你认为什么",
            this,
            this)

/*        text2ImgThird()
//        text2ImgSecond()
//        text2img()*/

/*        runTestIntent {
            *//*runTestIntent6()*//*
            *//*runTestIntent5()*//*
            *//*runTestIntent4()*//*
            *//*runTestIntent3()*//*
            *//*runTestIntent2()*//*
            *//*runTestIntent1()*//*
        }*/
    }

    private fun text2ImgThird() {
        val text = "因为你看看了我的出版物，请告诉我你认为什么"
        val bounds = Rect()
        val textPaint: TextPaint = object : TextPaint() {
            init {
                color = Color.WHITE
                textAlign = Align.LEFT
                textSize = 20f
                isAntiAlias = true
            }
        }
        textPaint.getTextBounds(text, 0, text.length, bounds)
        val mTextLayout = StaticLayout(
            text, textPaint,
            bounds.width(), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false
        )
        var maxWidth = -1
        for (i in 0 until mTextLayout.lineCount) {
            if (maxWidth < mTextLayout.getLineWidth(i)) {
                maxWidth = mTextLayout.getLineWidth(i).toInt()
            }
        }
        val bmp = Bitmap.createBitmap(
            maxWidth, mTextLayout.height,
            Bitmap.Config.ARGB_8888
        )
        bmp.eraseColor(Color.BLACK) // just adding black background

        val canvas = Canvas(bmp)
        mTextLayout.draw(canvas)

        val iv = findViewById<View>(io.github.gelassen.wordinmemory.R.id.content) as ImageView
//        iv.layoutParams = layoutParams
        iv.setBackgroundColor(Color.GRAY)
        iv.setImageBitmap(bmp)
    }

    private fun text2ImgSecond() {
        val msg = "因为你看看了我的出版物，请告诉我你认为什么"
        val bitmap = Bitmap.createBitmap(300, 400, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.CYAN)
        val paint = Paint()
        paint.textAlign = Align.LEFT // 若设置为center，则文本左半部分显示不全 paint.setColor(Color.RED);

        paint.isAntiAlias = true // 消除锯齿

        paint.textSize = 20f

        val allSaveFLag = 31
        canvas.drawText(msg, 20f, 30f, paint)
//        canvas.save(Canvas.ALL_SAVE_FLAG)
        canvas.save()
        canvas.restore()

        val iv = findViewById<View>(io.github.gelassen.wordinmemory.R.id.content) as ImageView
//        iv.layoutParams = layoutParams
        iv.setBackgroundColor(Color.GRAY)
        iv.setImageBitmap(bitmap)
    }
    private fun text2img() {
        try {
            val tv = TextView(this)
            val layoutParams = LinearLayout.LayoutParams(80, 100)
            tv.layoutParams = layoutParams
            tv.text = "因为你看看了我的出版物，请告诉我你认为什么"
            tv.setTextColor(Color.BLACK)
            tv.setBackgroundColor(Color.TRANSPARENT)

            val testB: Bitmap

            testB = Bitmap.createBitmap(80, 100, Bitmap.Config.ARGB_8888)
            val c = Canvas(testB)
            tv.layout(0, 0, 80, 100)
            tv.draw(c)

            val iv = findViewById<View>(io.github.gelassen.wordinmemory.R.id.content) as ImageView
            iv.layoutParams = layoutParams
            iv.setBackgroundColor(Color.GRAY)
            iv.setImageBitmap(testB)
/*            iv.maxHeight = 80
            iv.maxWidth = 80*/
        } catch (ex: Exception) {
            Log.e(App.TAG, "Failed to perform text2img", ex)
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

    override fun onSuccess(textTranslated: Text?) {
        Log.d(App.TAG, "Translated text ${textTranslated!!.text}")
    }

    override fun onFailure(ex: java.lang.Exception) {
        Log.e(App.TAG, "Failed translation", ex)
    }
}