package com.vinay.questlog.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class LineGraphView extends View {

    private Paint linePaint;
    private Paint fillPaint;
    private Path linePath;
    private Path fillPath;
    private List<Float> dataPoints = new ArrayList<>();
    private int graphColor = 0xFF00E5FF; // Default Cyan

    public LineGraphView(Context context) {
        super(context);
        init();
    }

    public LineGraphView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(6f);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);
        
        fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setStyle(Paint.Style.FILL);

        linePath = new Path();
        fillPath = new Path();
    }

    public void setData(List<Integer> statuses) {
        this.dataPoints.clear();
        for (Integer s : statuses) {
            dataPoints.add((float) s);
        }
        invalidate();
    }

    public void setGraphColor(int color) {
        this.graphColor = color;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (dataPoints.size() < 2) return;

        float width = getWidth();
        float height = getHeight();
        float padding = 10f;

        float xStep = (width - 2 * padding) / (dataPoints.size() - 1);
        float yMax = height - 2 * padding;

        linePath.reset();
        fillPath.reset();

        linePaint.setColor(graphColor);
        linePaint.setShadowLayer(15, 0, 0, graphColor); // Neon Glow

        for (int i = 0; i < dataPoints.size(); i++) {
            float x = padding + i * xStep;
            float y = height - padding - (dataPoints.get(i) * yMax);
            
            if (i == 0) {
                linePath.moveTo(x, y);
                fillPath.moveTo(x, height);
                fillPath.lineTo(x, y);
            } else {
                // Cubic spline would be nice, but simple line for now
                linePath.lineTo(x, y);
                fillPath.lineTo(x, y);
            }
            
            if (i == dataPoints.size() - 1) {
                fillPath.lineTo(x, height);
                fillPath.close();
            }
        }

        // Gradient Fill
        fillPaint.setShader(new LinearGradient(0, 0, 0, height, 
            graphColor & 0x88FFFFFF, Color.TRANSPARENT, Shader.TileMode.CLAMP));

        canvas.drawPath(fillPath, fillPaint);
        canvas.drawPath(linePath, linePaint);
    }
}
