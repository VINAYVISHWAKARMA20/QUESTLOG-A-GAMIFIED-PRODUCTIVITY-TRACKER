package com.example.quest_log.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CosmicBackgroundView extends View {
    private static final int STAR_COUNT = 80;
    private final List<Star> stars = new ArrayList<>();
    private final Random random = new Random();
    private final Paint starPaint = new Paint();
    private ShootingStar shootingStar;

    public CosmicBackgroundView(Context context, AttributeSet attrs) {
        super(context, attrs);
        starPaint.setAntiAlias(true);
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
        
        // Draw Stars
        for (Star star : stars) {
            star.update();
            starPaint.setColor(star.color);
            starPaint.setAlpha(star.opacity);
            canvas.drawCircle(star.x, star.y, star.size, starPaint);
        }

        // Draw Shooting Star
        if (shootingStar != null) {
            shootingStar.update();
            if (shootingStar.isDead) {
                shootingStar = null;
            } else {
                starPaint.setColor(Color.WHITE);
                starPaint.setAlpha(shootingStar.opacity);
                starPaint.setStrokeWidth(shootingStar.size);
                canvas.drawLine(shootingStar.startX, shootingStar.startY, shootingStar.x, shootingStar.y, starPaint);
            }
        } else if (random.nextInt(300) == 0) { // Trigger chance
            shootingStar = new ShootingStar(getWidth(), getHeight());
        }

        invalidate(); // Animate
    }

    private class Star {
        float x, y, size;
        int opacity, speed;
        int color = Color.WHITE;
        boolean fadingOut = true;

        Star(int w, int h) {
            x = random.nextFloat() * w;
            y = random.nextFloat() * h;
            size = 1 + random.nextFloat() * 2;
            opacity = random.nextInt(255);
            speed = 1 + random.nextInt(3);
            if (random.nextInt(10) == 0) color = 0xFFFFD700; // Occasional gold star
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
        float startX, startY, x, y, size;
        float vx, vy;
        int opacity = 255;
        boolean isDead = false;

        ShootingStar(int w, int h) {
            startX = random.nextFloat() * w;
            startY = random.nextFloat() * (h / 2f);
            x = startX;
            y = startY;
            vx = 15 + random.nextFloat() * 10;
            vy = 10 + random.nextFloat() * 8;
            size = 2 + random.nextFloat() * 3;
            // Angle it slightly
            if (random.nextBoolean()) vx = -vx;
        }

        void update() {
            x += vx;
            y += vy;
            opacity -= 5;
            if (opacity <= 0) isDead = true;
        }
    }
}
