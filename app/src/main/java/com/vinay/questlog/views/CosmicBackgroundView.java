package com.vinay.questlog.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CosmicBackgroundView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final int STAR_COUNT = 80;
    private final List<Star> stars = new ArrayList<>();
    private final List<ShootingStar> shootingStars = new ArrayList<>();
    private final Random random = new Random();

    public CosmicBackgroundView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        stars.clear();
        for (int i = 0; i < STAR_COUNT; i++) {
            stars.add(new Star(w, h));
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        // Draw static fading stars
        for (Star star : stars) {
            star.update();
            paint.setColor(star.color);
            paint.setAlpha(star.opacity);
            canvas.drawCircle(star.x, star.y, star.size, paint);
        }
        
        // Randomly spawn shooting stars
        if (random.nextInt(1000) < 5) {
            shootingStars.add(new ShootingStar(getWidth(), getHeight()));
        }
        
        // Draw shooting stars
        for (int i = shootingStars.size() - 1; i >= 0; i--) {
            ShootingStar ss = shootingStars.get(i);
            ss.update();
            if (ss.isDead()) {
                shootingStars.remove(i);
            } else {
                ss.draw(canvas, paint);
            }
        }
        
        invalidate(); // Loop animation
    }

    private class Star {
        float x, y, size;
        int opacity, speed, color = Color.WHITE;
        boolean fadingOut = true;

        Star(int w, int h) {
            x = random.nextFloat() * w;
            y = random.nextFloat() * h;
            size = 1 + random.nextFloat() * 2;
            opacity = random.nextInt(255);
            speed = 1 + random.nextInt(3);
            if (random.nextInt(15) == 0) color = 0xFF4081; // Pink
            if (random.nextInt(15) == 0) color = 0xFF00E5FF; // Cyan
        }

        void update() {
            if (fadingOut) {
                opacity -= speed;
                if (opacity <= 20) fadingOut = false;
            } else {
                opacity += speed;
                if (opacity >= 200) fadingOut = true;
            }
        }
    }

    private class ShootingStar {
        float x, y, vx, vy, tailLength;
        int alpha = 255;
        
        ShootingStar(int w, int h) {
            x = w + 50f;
            y = random.nextFloat() * h;
            vx = -3f - random.nextFloat() * 4f; // Slower
            vy = 1f + random.nextFloat() * 3f; // Slower
            tailLength = 200f + random.nextFloat() * 400f; // Much longer
        }
        
        void update() {
            x += vx;
            y += vy;
            alpha -= 1; // Slower fade
        }
        
        boolean isDead() {
            return alpha <= 0 || x < -200 || y > getHeight() * 1.5f;
        }
        
        void draw(Canvas canvas, Paint paint) {
            paint.setShader(null);
            paint.setStrokeWidth(4f);
            paint.setColor(Color.WHITE);
            paint.setAlpha(alpha);
            
            paint.setStyle(Paint.Style.STROKE);
            android.graphics.LinearGradient tailGrad = new android.graphics.LinearGradient(
                x, y, x - (vx * tailLength / 20f), y - (vy * tailLength / 20f),
                Color.WHITE, 0x00FFFFFF, Shader.TileMode.CLAMP
            );
            paint.setShader(tailGrad);
            canvas.drawLine(x, y, x - (vx * tailLength / 20f), y - (vy * tailLength / 20f), paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setShader(null);
            paint.setAlpha(255);
        }
    }
}
