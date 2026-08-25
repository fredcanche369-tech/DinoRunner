package com.dinorunner;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class DinoGameView extends View {

    // Estados principales del juego.
    private enum State {
        MENU,
        PLAYING,
        GAME_OVER
    }

    private State state = State.MENU;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random random = new Random();
    private final SharedPreferences preferences;

    // Sonido sencillo generado por Android.
    private boolean soundEnabled = true;

    // Tiempo y puntuación.
    private long lastFrameTime;
    private float score;
    private int bestScore;

    // Dinosaurio.
    private float dinoX;
    private float dinoY;
    private float dinoWidth;
    private float dinoHeight;
    private float velocityY;
    private boolean jumping;

    // Suelo y velocidad.
    private float groundY;
    private float speed;

    // Aparición de obstáculos.
    private float spawnTimer;

    private final ArrayList<Obstacle> obstacles = new ArrayList<>();

    // Para controlar los toques.
    private boolean touchDown;

    // Colores.
    private static final int SKY_COLOR = Color.rgb(235, 248, 255);
    private static final int GROUND_COLOR = Color.rgb(80, 80, 80);
    private static final int DINO_COLOR = Color.rgb(50, 150, 65);
    private static final int DINO_DARK_COLOR = Color.rgb(35, 110, 45);
    private static final int CACTUS_COLOR = Color.rgb(30, 125, 50);

    public DinoGameView(Context context) {
        super(context);

        setFocusable(true);

        preferences = context.getSharedPreferences(
                "dino_runner",
                Context.MODE_PRIVATE
        );

        bestScore = preferences.getInt("best_score", 0);

        lastFrameTime = SystemClock.uptimeMillis();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float width = getWidth();
        float height = getHeight();

        // Fondo.
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(245, 245, 245));
        canvas.drawRect(0, 0, width, height, paint);

        if (state == State.MENU) {
            drawMenu(canvas, width, height);
            return;
        }

        drawGame(canvas, width, height);

        if (state == State.GAME_OVER) {
            drawGameOver(canvas, width, height);
        }
    }

    // ---------------- MENU ----------------

    private void drawMenu(Canvas canvas, float width, float height) {

        drawText(
                canvas,
                "DINO RUNNER",
                width / 2f,
                height * 0.25f,
                42,
                Color.rgb(35, 90, 40),
                true
        );

        drawText(
                canvas,
                "Corre, salta y evita los cactus",
                width / 2f,
                height * 0.33f,
                18,
                Color.DKGRAY,
                false
        );

        drawDino(
                canvas,
                width / 2f - 40,
                height * 0.48f,
                80,
                80,
                false
        );

        drawButton(
                canvas,
                width / 2f,
                height * 0.68f,
                width * 0.55f,
                64,
                "JUGAR",
                Color.rgb(46, 125, 50)
        );

        drawText(
                canvas,
                "Mejor: " + bestScore,
                width / 2f,
                height * 0.79f,
                20,
                Color.DKGRAY,
                true
        );

        drawText(
                canvas,
                soundEnabled
                        ? "Sonido activado"
                        : "Sonido desactivado",
                width / 2f,
                height * 0.87f,
                16,
                Color.DKGRAY,
                false
        );
    }

    // ---------------- JUEGO ----------------

    private void drawGame(
            Canvas canvas,
            float width,
            float height
    ) {

        groundY = height * 0.78f;

        // Cielo.
        paint.setColor(SKY_COLOR);
        canvas.drawRect(0, 0, width, groundY, paint);

        // Nubes.
        paint.setColor(Color.WHITE);

        canvas.drawCircle(
                width * 0.18f,
                height * 0.20f,
                22,
                paint
        );

        canvas.drawCircle(
                width * 0.22f,
                height * 0.19f,
                30,
                paint
        );

        canvas.drawCircle(
                width * 0.27f,
                height * 0.20f,
                20,
                paint
        );

        // Suelo.
        paint.setColor(GROUND_COLOR);
        canvas.drawRect(
                0,
                groundY,
                width,
                groundY + 5,
                paint
        );

        // Dinosaurio.
        drawDino(
                canvas,
                dinoX,
                dinoY,
                dinoWidth,
                dinoHeight,
                jumping
        );

        // Obstáculos.
        for (Obstacle obstacle : obstacles) {
            drawCactus(
                    canvas,
                    obstacle.x,
                    obstacle.y,
                    obstacle.width,
                    obstacle.height
            );
        }

        // Puntuación.
        drawText(
                canvas,
                "Puntos: " + (int) score,
                24,
                44,
                22,
                Color.DKGRAY,
                true,
                Paint.Align.LEFT
        );

        // Sonido.
        drawText(
                canvas,
                soundEnabled ? "SON" : "SIL",
                width - 25,
                44,
                15,
                Color.DKGRAY,
                true,
                Paint.Align.RIGHT
        );
    }

    // ---------------- GAME OVER ----------------

    private void drawGameOver(
            Canvas canvas,
            float width,
            float height
    ) {

        paint.setColor(0xB8000000);
        canvas.drawRect(0, 0, width, height, paint);

        drawText(
                canvas,
                "GAME OVER",
                width / 2f,
                height * 0.28f,
                42,
                Color.WHITE,
                true
        );

        drawText(
                canvas,
                "Puntuación: " + (int) score,
                width / 2f,
                height * 0.38f,
                24,
                Color.WHITE,
                true
        );

        drawText(
                canvas,
                "Mejor puntuación: " + bestScore,
                width / 2f,
                height * 0.44f,
                21,
                Color.WHITE,
                false
        );

        drawButton(
                canvas,
                width / 2f,
                height * 0.59f,
                width * 0.62f,
                58,
                "REINTENTAR",
                Color.rgb(46, 125, 50)
        );

        drawButton(
                canvas,
                width / 2f,
                height * 0.70f,
                width * 0.62f,
                58,
                "MENU",
                Color.rgb(80, 80, 80)
        );
    }

    // ---------------- DINOSAURIO ----------------

    private void drawDino(
            Canvas canvas,
            float x,
            float y,
            float width,
            float height,
            boolean jumpingNow
    ) {

        paint.setColor(DINO_COLOR);

        // Cuerpo.
        canvas.drawRoundRect(
                new RectF(
                        x,
                        y,
                        x + width * 0.70f,
                        y + height * 0.75f
                ),
                10,
                10,
                paint
        );

        // Cabeza.
        canvas.drawRoundRect(
                new RectF(
                        x + width * 0.45f,
                        y - height * 0.20f,
                        x + width,
                        y + height * 0.40f
                ),
                12,
                12,
                paint
        );

        // Ojo.
        paint.setColor(Color.WHITE);

        canvas.drawCircle(
                x + width * 0.78f,
                y - height * 0.04f,
                6,
                paint
        );

        paint.setColor(Color.BLACK);

        canvas.drawCircle(
                x + width * 0.79f,
                y - height * 0.04f,
                2.5f,
                paint
        );

        // Patas.
        paint.setColor(DINO_DARK_COLOR);

        float legShift;

        if (jumpingNow) {
            legShift = 0;
        } else {
            long animation = System.currentTimeMillis() / 100;
            legShift = animation % 2 == 0 ? 5 : -5;
        }

        canvas.drawRect(
                x + width * 0.18f,
                y + height * 0.70f,
                x + width * 0.29f,
                y + height * 0.98f,
                paint
        );

        canvas.drawRect(
                x + width * 0.52f,
                y + height * 0.70f + legShift,
                x + width * 0.63f,
                y + height * 0.98f + legShift,
                paint
        );
    }

    // ---------------- CACTUS ----------------

    private void drawCactus(
            Canvas canvas,
            float x,
            float y,
            float width,
            float height
    ) {

        paint.setColor(CACTUS_COLOR);

        // Tronco.
        canvas.drawRoundRect(
                new RectF(
                        x,
                        y,
                        x + width,
                        y + height
                ),
                6,
                6,
                paint
        );

        // Brazo izquierdo.
        canvas.drawRoundRect(
                new RectF(
                        x - width * 0.65f,
                        y + height * 0.35f,
                        x,
                        y + height * 0.52f
                ),
                6,
                6,
                paint
        );

        // Brazo derecho.
        canvas.drawRoundRect(
                new RectF(
                        x + width,
                        y + height * 0.20f,
                        x + width * 1.65f,
                        y + height * 0.38f
                ),
                6,
                6,
                paint
        );

        // Brazo superior izquierdo.
        canvas.drawRoundRect(
                new RectF(
                        x - width * 0.45f,
                        y + height * 0.20f,
                        x - width * 0.25f,
                        y + height * 0.43f
                ),
                6,
                6,
                paint
        );
    }

    // ---------------- BOTONES ----------------

    private void drawButton(
            Canvas canvas,
            float centerX,
            float centerY,
            float width,
            float height,
            String label,
            int color
    ) {

        paint.setColor(color);

        canvas.drawRoundRect(
                new RectF(
                        centerX - width / 2f,
                        centerY - height / 2f,
                        centerX + width / 2f,
                        centerY + height / 2f
                ),
                18,
                18,
                paint
        );

        drawText(
                canvas,
                label,
                centerX,
                centerY + 8,
                21,
                Color.WHITE,
                true
        );
    }

    private void drawText(
            Canvas canvas,
            String text,
            float x,
            float y,
            float size,
            int color,
            boolean bold
    ) {

        drawText(
                canvas,
                text,
                x,
                y,
                size,
                color,
                bold,
                Paint.Align.CENTER
        );
    }

    private void drawText(
            Canvas canvas,
            String text,
            float x,
            float y,
            float size,
            int color,
            boolean bold,
            Paint.Align align
    ) {

        paint.setColor(color);
        paint.setTextSize(size);
        paint.setTextAlign(align);

        paint.setTypeface(
                bold
                        ? android.graphics.Typeface.DEFAULT_BOLD
                        : android.graphics.Typeface.DEFAULT
        );

        canvas.drawText(
                text,
                x,
                y,
                paint
        );
    }

    // ---------------- INICIAR JUEGO ----------------

    private void startGame() {

        state = State.PLAYING;

        score = 0;

        speed = Math.max(
                420,
                getWidth() * 0.45f
        );

        spawnTimer = 0;

        obstacles.clear();

        dinoWidth = Math.max(
                60,
                getWidth() * 0.13f
        );

        dinoHeight = dinoWidth;

        groundY = getHeight() * 0.78f;

        dinoX = getWidth() * 0.14f;

        dinoY = groundY - dinoHeight;

        velocityY = 0;

        jumping = false;

        lastFrameTime = SystemClock.uptimeMillis();

        invalidate();

        removeCallbacks(gameLoop);
        scheduleGameLoop();
    }

    // ---------------- GAME OVER ----------------

    private void gameOver() {

        state = State.GAME_OVER;

        int finalScore = (int) score;

        if (finalScore > bestScore) {

            bestScore = finalScore;

            preferences
                    .edit()
                    .putInt("best_score", bestScore)
                    .apply();
        }

        playTone(false);

        removeCallbacks(gameLoop);

        invalidate();
    }

    // ---------------- ACTUALIZACIÓN ----------------

    private void update(float deltaTime) {

        if (state != State.PLAYING) {
            return;
        }

        // Aumentar velocidad poco a poco.
        speed += deltaTime * 7.0f;

        // Aumentar puntuación.
        score += deltaTime * 10.0f;

        // Física del salto.
        if (jumping) {

            velocityY += 1900f * deltaTime;

            dinoY += velocityY * deltaTime;

            if (dinoY >= groundY - dinoHeight) {

                dinoY = groundY - dinoHeight;

                velocityY = 0;

                jumping = false;
            }
        }

        // Crear cactus.
        spawnTimer -= deltaTime;

        if (spawnTimer <= 0) {

            float cactusWidth = Math.max(
                    24,
                    getWidth() * 0.055f
            );

            float cactusHeight =
                    cactusWidth *
                    (1.5f + random.nextFloat() * 0.9f);

            obstacles.add(
                    new Obstacle(
                            getWidth() + 30,
                            groundY - cactusHeight,
                            cactusWidth,
                            cactusHeight
                    )
            );

            float minimumDelay = Math.max(
                    0.65f,
                    1.15f - speed / 2200f
            );

            spawnTimer =
                    minimumDelay +
                    random.nextFloat() * 0.75f;
        }

        Iterator<Obstacle> iterator =
                obstacles.iterator();

        while (iterator.hasNext()) {

            Obstacle obstacle =
                    iterator.next();

            obstacle.x -= speed * deltaTime;

            if (obstacle.x + obstacle.width * 1.7f < 0) {

                iterator.remove();

            } else if (collides(obstacle)) {

                gameOver();

                return;
            }
        }
    }

    // ---------------- COLISIÓN ----------------

    private boolean collides(Obstacle obstacle) {

        float dinoLeft =
                dinoX + dinoWidth * 0.18f;

        float dinoTop =
                dinoY + dinoHeight * 0.15f;

        float dinoRight =
                dinoX + dinoWidth * 0.86f;

        float dinoBottom =
                dinoY + dinoHeight * 0.92f;

        float obstacleLeft =
                obstacle.x - obstacle.width * 0.25f;

        float obstacleTop =
                obstacle.y;

        float obstacleRight =
                obstacle.x + obstacle.width * 1.55f;

        float obstacleBottom =
                obstacle.y + obstacle.height;

        return dinoLeft < obstacleRight
                && dinoRight > obstacleLeft
                && dinoTop < obstacleBottom
                && dinoBottom > obstacleTop;
    }

    // ---------------- TOQUES ----------------

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if (event.getAction() == MotionEvent.ACTION_DOWN) {

            touchDown = true;

            return true;
        }

        if (
                event.getAction() == MotionEvent.ACTION_UP
                        && touchDown
        ) {

            touchDown = false;

            float x = event.getX();
            float y = event.getY();

            // MENÚ.
            if (state == State.MENU) {

                if (
                        y > getHeight() * 0.58f
                                && y < getHeight() * 0.75f
                ) {

                    startGame();

                } else if (y > getHeight() * 0.82f) {

                    soundEnabled = !soundEnabled;

                    invalidate();
                }

                return true;
            }

            // JUEGO.
            if (state == State.PLAYING) {

                // Botón de sonido.
                if (
                        x > getWidth() - 100
                                && y < 80
                ) {

                    soundEnabled = !soundEnabled;

                    invalidate();

                } else if (!jumping) {

                    jumping = true;

                    velocityY =
                            -Math.max(
                                    760,
                                    getHeight() * 0.95f
                            );

                    playTone(true);
                }

                return true;
            }

            // GAME OVER.
            if (state == State.GAME_OVER) {

                if (
                        y > getHeight() * 0.52f
                                && y < getHeight() * 0.66f
                ) {

                    startGame();

    }
                } else if (
                        y > getHeight() * 0.65f
                                && y < getHeight() * 0.78f
                ) {

                    state = State.MENU;

                    invalidate();
                }

                return true;
            }
        }

        return true;
    }

    // ---------------- SONIDO ----------------

    private void playTone(boolean jump) {

        if (!soundEnabled) {
            return;
        }

        ToneGenerator toneGenerator =
                new ToneGenerator(
                        AudioManager.STREAM_MUSIC,
                        70
                );

        if (jump) {

            toneGenerator.startTone(
                    ToneGenerator.TONE_PROP_BEEP,
                    70
            );

        } else {

            toneGenerator.startTone(
                    ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD,
                    180
            );
        }

        postDelayed(
                toneGenerator::release,
                jump ? 100 : 220
        );
    }

    private final Runnable gameLoop = new Runnable() {
    @Override
    public void run() {

        long currentTime = SystemClock.uptimeMillis();

        float deltaTime = Math.min(
                0.035f,
                (currentTime - lastFrameTime) / 1000f
        );

        lastFrameTime = currentTime;

        update(deltaTime);

        invalidate();

        if (state == State.PLAYING) {
            scheduleGameLoop();
        }
    }
};
private void scheduleGameLoop() {
    postDelayed(gameLoop, 16);
}

@Override
protected void onAttachedToWindow() {
    super.onAttachedToWindow();

    lastFrameTime = SystemClock.uptimeMillis();

    removeCallbacks(gameLoop);

    if (state == State.PLAYING) {
        scheduleGameLoop();
    }
}

public void pauseGame() {
    removeCallbacks(gameLoop);
}

public void resumeGame() {
    lastFrameTime = SystemClock.uptimeMillis();

    removeCallbacks(gameLoop);

    if (state == State.PLAYING) {
        scheduleGameLoop();
    }
}

private static class Obstacle {

    float x;
    float y;
    float width;
    float height;

    Obstacle(
            float x,
            float y,
            float width,
            float height
    ) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }
}
