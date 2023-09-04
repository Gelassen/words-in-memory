package io.github.gelassen.wordinmemory.ml


class OCRTranslator {

/*    val recognizer = TextRecognition.getClient(
        ChineseTextRecognizerOptions
            .Builder()
            .build()
    )

    fun run(text: String, successListener: OnSuccessListener<Text>, failureListener: OnFailureListener) {
        recognizer.process(
            InputImage.fromBitmap(
                chineseTextToImage(text),
                0)
        )
            .addOnSuccessListener(successListener)
            .addOnFailureListener(failureListener)
    }

    *//**
     * Don't forget to reclaim memory y calling bitmap.recycle()
     *
     * @author https://stackoverflow.com/a/18077318/3649629
     * *//*
    private fun chineseTextToImage(text: String): Bitmap {
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
        val textLayout = StaticLayout(
            text, textPaint,
            bounds.width(), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false
        )
        var maxWidth = -1
        for (i in 0 until textLayout.lineCount) {
            if (maxWidth < textLayout.getLineWidth(i)) {
                maxWidth = textLayout.getLineWidth(i).toInt()
            }
        }
        val bmp = Bitmap.createBitmap(maxWidth, textLayout.height, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(Color.BLACK) // just adding black background

        val canvas = Canvas(bmp)
        textLayout.draw(canvas)

        return bmp
    }

    *//**
     * Memory-map the model file in Assets.
     *
     * @ref https://blog.tensorflow.org/2018/03/using-tensorflow-lite-on-android.html
     * *//*
    @Throws(IOException::class)
    private fun loadModelFile(activity: Activity): MappedByteBuffer? {
        val fileDescriptor = activity.assets.openFd(getModelPath())
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun getModelPath(): String {
        return ""
    }*/


}