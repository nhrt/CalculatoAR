package com.example.swtp.customview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.View;

import java.util.List;

public class ResultView extends View {
    List<Pair<String, RectF>> results;


    public ResultView(Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    public void setResult(List<Pair<String, RectF>>  results){
        this.results  = results;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.TRANSPARENT);
        canvas.drawPaint(paint);

        paint.setTextSize(48f);
        paint.setColor(Color.BLACK);

        if(results != null){
            int i = 1;
            for(Pair<String, RectF> result : results){
                if(result != null && result.first != null){
                    canvas.drawText(result.first,100 * i,100 * i,paint);
                    i++;
                }

            }


        }

    }
}
