package edu.skku.myapplication

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import org.tensorflow.lite.task.vision.detector.Detection
import java.util.LinkedList

class OverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private var results: List<Detection> = LinkedList<Detection>()
    private var boxPaint = Paint()
    private var textBackgroundPaint = Paint()
    private var textPaint = Paint()

    private var imageWidth: Int = 1
    private var imageHeight: Int = 1

    init {
        // 사각형 테두리 페인트 설정
        boxPaint.color = Color.GREEN
        boxPaint.style = Paint.Style.STROKE
        boxPaint.strokeWidth = 8f

        // 텍스트 배경 페인트 설정
        textBackgroundPaint.color = Color.BLACK
        textBackgroundPaint.style = Paint.Style.FILL
        textBackgroundPaint.alpha = 128 // 반투명

        // 텍스트 페인트 설정
        textPaint.color = Color.WHITE
        textPaint.style = Paint.Style.FILL
        textPaint.textSize = 50f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 화면 크기와 실제 이미지 크기의 비율을 계산
        val scaleX = width.toFloat() / imageWidth
        val scaleY = height.toFloat() / imageHeight

        // 각 탐지 결과에 대해 사각형과 텍스트를 그림
        for (result in results) {
            val boundingBox = result.boundingBox

            // 화면 크기에 맞게 좌표 조정
            val top = boundingBox.top * scaleY
            val bottom = boundingBox.bottom * scaleY
            val left = boundingBox.left * scaleX
            val right = boundingBox.right * scaleX

            // 사각형 그리기
            val drawableRect = RectF(left, top, right, bottom)
            canvas.drawRect(drawableRect, boxPaint)

            // 텍스트 그리기 (카테고리 이름 + 신뢰도 점수)
            val text = "${result.categories[0].label} ${(result.categories[0].score * 100).toInt()}%"
            val textWidth = textPaint.measureText(text)
            val textHeight = textPaint.descent() - textPaint.ascent()
            val textBackgroundRect = RectF(left, top, left + textWidth + 8, top + textHeight + 8)

            canvas.drawRect(textBackgroundRect, textBackgroundPaint)
            canvas.drawText(text, left + 4, top + textHeight, textPaint)
        }
    }

    // MainActivity에서 호출하여 그릴 내용과 이미지 크기를 업데이트하는 함수
    fun setResults(detections: List<Detection>, imageWidth: Int, imageHeight: Int) {
        this.results = detections
        this.imageWidth = imageWidth
        this.imageHeight = imageHeight
        invalidate() // onDraw()를 다시 호출하여 화면을 갱신
    }
}