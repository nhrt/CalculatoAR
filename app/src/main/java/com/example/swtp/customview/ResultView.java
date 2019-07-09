package com.example.swtp.customview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.View;

import java.util.List;

public class ResultView extends View {
    private List<Pair<String, RectF>> results;
    private boolean hasResult = false;
    private Paint paint;


    public ResultView(Context context, final AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint();

    }

    public void setResult(List<Pair<String, RectF>> results) {
        this.results = results;
        hasResult = true;
    }

    public boolean hasResult() {
        return hasResult;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.TRANSPARENT);
        canvas.drawPaint(paint);

        paint.setTextSize(60f);
        paint.setColor(Color.BLACK);

        if (results != null) {
            for (Pair<String, RectF> result : results) {
                if (result != null && result.first != null && result.second != null && !result.first.equals("NaN")) {
                    canvas.drawText(result.first, result.second.right, result.second.bottom, paint);
                }
            }

        }


    }
}
