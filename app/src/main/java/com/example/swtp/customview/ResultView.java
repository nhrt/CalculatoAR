package com.example.swtp.customview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.View;

import com.example.swtp.env.Logger;

import java.util.List;

public class ResultView extends View {
    private static final Logger LOGGER = new Logger();
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

        paint.setTextSize(60f);
        paint.setColor(Color.BLACK);

        if(results != null){
            for(Pair<String, RectF> result : results){
                if(result != null && result.first != null && result.second != null && !result.first.equals("NaN")){
                    LOGGER.i("Result: %s Location: x %d y %d",result.first,(int)result.second.left,(int)result.second.top);
                    canvas.drawText(result.first,result.second.right,result.second.bottom,paint);
                }
            }
        }

    }
}
