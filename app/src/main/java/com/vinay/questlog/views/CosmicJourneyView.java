package com.vinay.questlog.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.DashPathEffect;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CosmicJourneyView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pathPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // Starfield
    private static final int STAR_COUNT = 120;
    private final List<Star> stars = new ArrayList<>();
    private final Random random = new Random();

    // Journey Data
    private int userLevel = 1;
    private float xpProgress = 0f; // 0.0 to 1.0
    private boolean hasDragon = false;
    private final List<Planet> planets = new ArrayList<>();
    
    // Shooting Stars
    private final List<ShootingStar> shootingStars = new ArrayList<>();
    
    // Panning & Camera
    private float virtualHeight = 0f;
    private float cameraY = 0f;
    private boolean shouldFocusUser = true;
    private float lastTouchY = 0f;

    public CosmicJourneyView(Context context, AttributeSet attrs) {
        super(context, attrs);
        
        pathPaint.setStyle(Paint.Style.STROKE);
        pathPaint.setStrokeWidth(6f);
        pathPaint.setColor(0x15FFFFFF); // Make path very faintly visible
        pathPaint.setPathEffect(new DashPathEffect(new float[]{15f, 15f}, 0f));
        
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(34f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setShadowLayer(10f, 0, 0, Color.BLACK);
        textPaint.setFakeBoldText(true);
    }

    public void setProgress(int level, float xpRatio, boolean hasDragon) {
        this.userLevel = level;
        this.xpProgress = xpRatio;
        this.hasDragon = hasDragon;
        this.shouldFocusUser = true;
        invalidate();
    }
    
    public void focusOnUser() {
        shouldFocusUser = true;
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        virtualHeight = Math.max(h, h * 4f); // 4x height for massive space
        
        stars.clear();
        for (int i = 0; i < STAR_COUNT; i++) {
            stars.add(new Star(w, (int) virtualHeight));
        }
        
        // Define Planets (Zig-Zag upward with padding to avoid text overlap)
        planets.clear();
        float pWidth = w * 0.8f;
        // Keep everything clustered in the middle 70% of the canvas so panning up/down feels unrestricted
        float usableHeight = virtualHeight * 0.70f; 
        float hStep = usableHeight / 6f;
        float yOffset = virtualHeight * 0.15f; // Push down away from the top boundary
        
        planets.add(new Planet("Habit Genesis", 1, w * 0.25f, virtualHeight - yOffset, 0xFFFFC107, 50f, 0)); // 0
        planets.add(new Planet("Focus Sphere", 6, w * 0.75f, virtualHeight - yOffset - hStep * 1, 0xFF4CAF50, 55f, 1)); // 1
        planets.add(new Planet("Momentum Core", 16, w * 0.3f, virtualHeight - yOffset - hStep * 2, 0xFFE64A19, 55f, 2)); // 2
        planets.add(new Planet("Consistency Ridge", 26, w * 0.7f, virtualHeight - yOffset - hStep * 3, 0xFFFFE082, 65f, 3)); // 3
        planets.add(new Planet("Discipline Peak", 41, w * 0.35f, virtualHeight - yOffset - hStep * 4, 0xFF1976D2, 85f, 4)); // 4
        planets.add(new Planet("Flow State", 61, w * 0.65f, virtualHeight - yOffset - hStep * 5, 0xFF00E5FF, 60f, 5)); // 5
        planets.add(new Planet("Mastery Prime", 86, w * 0.5f, virtualHeight - yOffset - hStep * 6, 0xFF9C27B0, 90f, 6)); // 6
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (planets.isEmpty()) return;

        // 1) Determine current progress & camera focus
        int currentIndex = 0;
        for (int i = 0; i < planets.size(); i++) {
            if (userLevel >= planets.get(i).minLevel) {
                currentIndex = i;
            }
        }
        
        Planet currentPlanet = planets.get(currentIndex);
        Planet nextPlanet = currentIndex < planets.size() - 1 ? planets.get(currentIndex + 1) : currentPlanet;

        float satX = currentPlanet.x;
        float satY = currentPlanet.y;
        if (currentPlanet != nextPlanet) {
            float totalLevels = nextPlanet.minLevel - currentPlanet.minLevel;
            // E.g., if at Lvl 3 out of 1->6, base Progress is (3-1) = 2. Plus xp ratio = 2.5
            // fraction = 2.5 / 5 = 0.50 (50% of the way to the next planet)
            float fractionalProgress = (userLevel - currentPlanet.minLevel) + xpProgress;
            float travelFraction = Math.min(1f, Math.max(0f, fractionalProgress / totalLevels));
            
            satX = currentPlanet.x + (nextPlanet.x - currentPlanet.x) * travelFraction;
            satY = currentPlanet.y + (nextPlanet.y - currentPlanet.y) * travelFraction;
        }

        if (shouldFocusUser && virtualHeight > 0) {
            cameraY = satY - getHeight() / 2f;
            float maxCamera = Math.max(0, virtualHeight - getHeight());
            if (cameraY < 0) cameraY = 0;
            if (cameraY > maxCamera) cameraY = maxCamera;
            shouldFocusUser = false;
        }

        // 2) Draw Starfield (Parallax scrolling)
        canvas.save();
        canvas.translate(0, -cameraY * 0.3f);
        for (Star star : stars) {
            star.update();
            paint.setColor(star.color);
            paint.setAlpha(star.opacity);
            canvas.drawCircle(star.x, star.y, star.size, paint);
        }
        
        // Randomly spawn shooting stars (Rare)
        if (random.nextInt(1000) < 8) {
            shootingStars.add(new ShootingStar(getWidth(), (int) Math.max(getHeight(), virtualHeight)));
        }
        
        // Draw and update shooting stars
        for (int i = shootingStars.size() - 1; i >= 0; i--) {
            ShootingStar ss = shootingStars.get(i);
            ss.update();
            if (ss.isDead()) {
                shootingStars.remove(i);
            } else {
                ss.draw(canvas, paint);
            }
        }
        canvas.restore();

        // 3) Draw Journey Layer (Full scrolling)
        canvas.save();
        canvas.translate(0, -cameraY);

        // Paths removed per user request

        // Draw Planets with EXTREME High Fidelity styling
        for (int i = 0; i < planets.size(); i++) {
            Planet p = planets.get(i);
            boolean isLocked = userLevel < p.minLevel;
            
            canvas.save();
            
            // Draw dark-core base with a solid neon edge to match new aesthetic image
            int glowColor = 0xFFFFFFFF;
            switch (p.style) {
                case 0: glowColor = 0xFFFFD54F; break; // Yellow/Gold
                case 1: glowColor = 0xFFE91E63; break; // Magenta/Pink
                case 2: glowColor = 0xFFFF7043; break; // Deep Orange
                case 3: glowColor = 0xFF29B6F6; break; // Cyan/Blue
                case 4: glowColor = 0xFFEF5350; break; // Red Striped
                case 5: glowColor = 0xFFFFA726; break; // Orange Ringed
                case 6: glowColor = 0xFFAB47BC; break; // Purple/Indigo
            }
            
            if (isLocked) {
                paint.setShadowLayer(p.radius * 2.0f, 0, 0, (glowColor & 0x00FFFFFF) | 0x88000000);
            } else {
                paint.setShadowLayer(p.radius * 6.0f, 0, 0, glowColor); // EXTREME Glowing
                canvas.drawCircle(p.x, p.y, p.radius * 0.95f, paint); // Triple glow underlying layer
                canvas.drawCircle(p.x, p.y, p.radius * 0.95f, paint); 
            }
            paint.setColor(0xFF000000); // Solid black back to pop the neon outline
            canvas.drawCircle(p.x, p.y, p.radius * 0.95f, paint);
            paint.clearShadowLayer();
            
            int alphaGlow = isLocked ? (glowColor & 0x00FFFFFF) | 0x66000000 : glowColor;
            
            RadialGradient core = new RadialGradient(p.x, p.y, p.radius,
                new int[]{0xFF050505, 0xFF151515, alphaGlow},
                new float[]{0f, 0.7f, 1f}, Shader.TileMode.CLAMP);
            paint.setShader(core);
            canvas.drawCircle(p.x, p.y, p.radius, paint);
            paint.setShader(null);
            
            // Draw Specific Inner Materials Matching the Image
            int darkGlow = (glowColor & 0x00FFFFFF) | 0x99000000;
            int midGlow = (glowColor & 0x00FFFFFF) | 0x66000000;
            int faintGlow = (glowColor & 0x00FFFFFF) | 0x33000000;
            
            paint.setColor(darkGlow); // Default tinted overlay for spots
            switch (p.style) {
                case 0: // Habit Genesis (Yellow/Brown with dark landmasses)
                    android.graphics.SweepGradient landGrad0 = new android.graphics.SweepGradient(p.x, p.y,
                        new int[]{0x00000000, midGlow, 0x00000000, darkGlow, 0x00000000},
                        new float[]{0f, 0.2f, 0.5f, 0.7f, 1f});
                    paint.setShader(landGrad0);
                    canvas.drawCircle(p.x, p.y, p.radius, paint);
                    paint.setShader(null);
                    
                    canvas.drawCircle(p.x - p.radius*0.3f, p.y - p.radius*0.2f, p.radius*0.35f, paint);
                    canvas.drawCircle(p.x + p.radius*0.2f, p.y + p.radius*0.4f, p.radius*0.25f, paint);
                    break;
                    
                case 1: // Focus Sphere (Magenta with dark spots)
                    paint.setColor(darkGlow);
                    canvas.drawCircle(p.x - p.radius*0.2f, p.y + p.radius*0.3f, p.radius*0.4f, paint);
                    canvas.drawCircle(p.x + p.radius*0.3f, p.y - p.radius*0.1f, p.radius*0.3f, paint);
                    break;
                    
                case 2: // Momentum Core (Deep Orange with dark craters)
                    paint.setColor(midGlow);
                    canvas.drawCircle(p.x, p.y, p.radius*0.2f, paint);
                    canvas.drawCircle(p.x - p.radius*0.4f, p.y - p.radius*0.2f, p.radius*0.15f, paint);
                    canvas.drawCircle(p.x + p.radius*0.3f, p.y + p.radius*0.3f, p.radius*0.25f, paint);
                    break;
                    
                case 3: // Consistency Ridge (Cyan Gas Giant)
                    android.graphics.LinearGradient gasBands = new android.graphics.LinearGradient(
                        p.x, p.y - p.radius, p.x, p.y + p.radius,
                        new int[]{0x00000000, midGlow, 0x00000000, darkGlow, 0x00000000, faintGlow, 0x00000000},
                        new float[]{0f, 0.2f, 0.4f, 0.6f, 0.75f, 0.9f, 1f}, Shader.TileMode.MIRROR);
                    paint.setShader(gasBands);
                    canvas.drawCircle(p.x, p.y, p.radius, paint);
                    paint.setShader(null);
                    break;
                    
                case 4: // Discipline Peak (Red wavy/diagonal bands)
                    android.graphics.LinearGradient wavyBands = new android.graphics.LinearGradient(
                        p.x - p.radius, p.y - p.radius, p.x + p.radius, p.y + p.radius,
                        new int[]{0x00000000, darkGlow, 0x00000000, darkGlow, 0x00000000},
                        new float[]{0f, 0.3f, 0.5f, 0.7f, 1f}, Shader.TileMode.CLAMP);
                    paint.setShader(wavyBands);
                    canvas.drawCircle(p.x, p.y, p.radius, paint);
                    paint.setShader(null);
                    break;
                    
                case 5: // Flow State (Orange diagonal stripes + Ring)
                    android.graphics.LinearGradient diagStripes = new android.graphics.LinearGradient(
                        p.x + p.radius, p.y - p.radius, p.x - p.radius, p.y + p.radius,
                        new int[]{0x00000000, darkGlow, 0x00000000, darkGlow, 0x00000000},
                        null, Shader.TileMode.MIRROR);
                    paint.setShader(diagStripes);
                    canvas.drawCircle(p.x, p.y, p.radius, paint);
                    paint.setShader(null);

                    canvas.save();
                    canvas.translate(p.x, p.y);
                    canvas.rotate(-20f);
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(6f);
                    paint.setColor(alphaGlow);
                    canvas.drawOval(-p.radius * 1.8f, -p.radius * 0.3f, p.radius * 1.8f, p.radius * 0.3f, paint);
                    paint.setStrokeWidth(2f);
                    canvas.drawOval(-p.radius * 2.1f, -p.radius * 0.4f, p.radius * 2.1f, p.radius * 0.4f, paint);
                    paint.setStyle(Paint.Style.FILL);
                    canvas.restore();
                    break;
                    
                case 6: // Mastery Prime (Core Only)
                    // Draw Planet Sphere (Deep Blue/Purple/Pink core)
                    RadialGradient planetCore = new RadialGradient(p.x - p.radius * 0.3f, p.y - p.radius * 0.3f, p.radius * 1.5f,
                        new int[]{0xFFF48FB1, 0xFFAB47BC, 0xFF0288D1, 0xFF000000}, // Pink to purple to cyan to black
                        new float[]{0f, 0.4f, 0.8f, 1f}, Shader.TileMode.CLAMP);
                    paint.setShader(planetCore);
                    canvas.drawCircle(p.x, p.y, p.radius, paint);
                    paint.setShader(null);
                    break;
            }
            
            canvas.restore();
            
            // Text Label
            float textY = p.y + p.radius + 30; // Closer due to smaller font
            textPaint.setColor(isLocked ? 0xFFAAAAAA : 0xFFFFD700); // Requested golden color
            textPaint.setTextSize(24f); // Specifically requested small font
            canvas.drawText(p.name, p.x, textY, textPaint);
            
            // Revert size back to global default for any other text drawing
            textPaint.setTextSize(34f);
        }

        // Draw Satellite (The User)
        float shipAngle = 0f;
        if (currentPlanet != nextPlanet) {
            float dx = nextPlanet.x - currentPlanet.x;
            float dy = nextPlanet.y - currentPlanet.y;
            shipAngle = (float) Math.toDegrees(Math.atan2(dy, dx)); // Heading angle from +X axis
        } else {
            // Idle Orbiting
            float orbitAngle = (System.currentTimeMillis() % 4000) * 360f / 4000f;
            satX += Math.cos(Math.toRadians(orbitAngle)) * 30;
            satY += Math.sin(Math.toRadians(orbitAngle)) * 30;
            shipAngle = orbitAngle + 90f; // Face direction of orbit
        }
        
        canvas.save();
        canvas.translate(satX, satY);
        
        // Draw Spaceship as 🔥 Emoji 🚀
        paint.setTextSize(60f); // Make rocket smaller as requested
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setShadowLayer(10f, 0, 0, 0xAA000000); // Add drop shadow so it pops from background
        
        // Emoji rotation adjustment: the 🚀 emoji is naturally tilted ~45 degrees up and to the right (-45 deg).
        // Rotating by +45 aligns its nose to the 0 deg (+X axis).
        canvas.rotate(shipAngle + 45f);
        canvas.drawText("🚀", 0, 20, paint);
        paint.clearShadowLayer();
        
        // Draw Pet Dragon Orbiting!
        if (hasDragon) {
            // Dragon rotates independently of the ship
            canvas.restore(); 
            canvas.save();
            canvas.translate(satX, satY);
            
            float dragonTimeOffset = (System.currentTimeMillis() % 2000) / 5.5f; // Faster orbit
            canvas.rotate(dragonTimeOffset);
            
            // Dragon Head
            paint.setColor(0xFFFF0055); // Crimson Dragon
            paint.setShadowLayer(15f, 0, 0, 0xFFFF0055);
            canvas.drawCircle(45, 0, 9, paint);
            // Dragon Tail
            paint.setColor(0xFFFF9800); 
            canvas.drawCircle(38, 5, 5, paint);
            paint.clearShadowLayer();
            
            canvas.restore();
            canvas.save(); // Prepare for next draw logic
            canvas.translate(satX, satY);
        }
        
        canvas.restore();
        
        // "YOU" Label
        textPaint.setColor(0xFF00FFCC);
        canvas.drawText("YOU", satX, satY - 45, textPaint);
        
        canvas.restore(); // Restore Journey Layer
        
        invalidate(); // Endless animation loop for stars
    }

    @Override
    public boolean onTouchEvent(android.view.MotionEvent event) {
        switch (event.getAction()) {
            case android.view.MotionEvent.ACTION_DOWN:
                lastTouchY = event.getY();
                return true;
            case android.view.MotionEvent.ACTION_MOVE:
                float dy = event.getY() - lastTouchY;
                cameraY -= dy;
                lastTouchY = event.getY();
                
                // Allow a bit of over-panning
                float maxCamera = Math.max(0, virtualHeight - getHeight() * 0.85f);
                if (cameraY < -getHeight() * 0.15f) cameraY = -getHeight() * 0.15f;
                if (cameraY > maxCamera) cameraY = maxCamera;
                return true;
        }
        return super.onTouchEvent(event);
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
            if (random.nextInt(15) == 0) color = 0xFF4081; // Pinkish stars
            if (random.nextInt(15) == 0) color = 0xFF00E5FF; // Neon Blue stars
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
        float x, y;
        float vx, vy;
        float tailLength;
        int alpha = 255;
        
        ShootingStar(int w, int h) { // Removed camY parameter
            x = w + 50f;
            y = random.nextFloat() * h; // Use full virtual height for y spawn
            vx = -3f - random.nextFloat() * 4f; // Slower horizontal speed
            vy = 1f + random.nextFloat() * 3f;  // Slower vertical speed
            tailLength = 250f + random.nextFloat() * 350f; // Much longer tail
        }
        
        void update() {
            x += vx;
            y += vy;
            alpha -= 1; // Slower fade out (lives longer)
        }
        
        boolean isDead() {
            return alpha <= 0 || x < -200 || y > virtualHeight;
        }
        
        void draw(Canvas canvas, Paint paint) {
            paint.setShader(null);
            paint.setStrokeWidth(4f);
            paint.setColor(Color.WHITE);
            paint.setAlpha(alpha);
            
            // Draw a line for the comet tail
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

    private class Planet {
        String name;
        int minLevel;
        float x, y, radius;
        int color;
        int style; // Controls rendering type

        Planet(String name, int minLevel, float x, float y, int color, float radius, int style) {
            this.name = name;
            this.minLevel = minLevel;
            this.x = x;
            this.y = y;
            this.color = color;
            this.radius = radius;
            this.style = style;
        }
    }
}
