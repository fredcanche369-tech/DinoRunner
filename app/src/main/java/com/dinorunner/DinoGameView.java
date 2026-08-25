package com.dinorunner;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.*;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class DinoGameView extends View {

    private enum State { MENU, PLAYING, GAME_OVER }
    private State state = State.MENU;

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random random = new Random();
    private final SharedPreferences prefs;

    private SoundPool soundPool;
    private int jumpSound, loseSound;
    private boolean soundEnabled = true;

    private long lastFrame;
    private long scoreStart;
    private float score;
    private int bestScore;

    // Posiciones y tamaños se calculan proporcionalmente al ancho/alto.
    private float groundY;
    private float dinoX, dinoY, dinoW, dinoH;
    private float velocityY;
    private boolean jumping;
    private float speed;
    private float spawnTimer;

    private final ArrayList<Obstacle> obstacles = new ArrayList<>();

    private boolean touchWasDown = false;

    public DinoGameView(Context context) {
        super(context);
        setFocusable(true);

        prefs = context.getSharedPreferences("dino_runner", Context.MODE_PRIVATE);
        bestScore = prefs.getInt("best_score", 0);

        // Los efectos se generan con ToneGenerator; no hacen falta archivos de audio.
        lastFrame = SystemClock.uptimeMillis();
    }

    /*
     * Para mantener el proyecto simple y sin archivos de audio externos,
     * los tonos se generan como archivos temporales WAV al iniciar.
     */
    private int createToneResource(Context context, int freq, int durationMs) {
        // SoundPool.load() necesita un archivo real. Se usa cacheDir para crearlo.
        try {
            java.io.File f = new java.io.File(context.getCacheDir(),
                    "tone_" + freq + "_" + durationMs + ".wav");
            if (!f.exists()) {
                int sampleRate = 22050;
                int samples = sampleRate * durationMs / 1000;
                java.io.FileOutputStream out = new java.io.FileOutputStream(f);
                java.io.DataOutputStream d = new java.io.DataOutputStream(out);

                writeAscii(d, "RIFF");
                writeLEInt(d, 36 + samples * 2);
                writeAscii(d, "WAVE");
                writeAscii(d, "fmt ");
                writeLEInt(d, 16);
                writeLEShort(d, (short)1);
                writeLEShort(d, (short)1);
                writeLEInt(d, sampleRate);
                writeLEInt(d, sampleRate * 2);
                writeLEShort(d, (short)2);
                writeLEShort(d, (short)16);
                writeAscii(d, "data");
                writeLEInt(d, samples * 2);

                for (int i = 0; i < samples; i++) {
                    double t = i / (double) sampleRate;
                    double envelope = Math.max(0, 1.0 - i / (double) samples);
                    short value = (short)(Math.sin(2 * Math.PI * freq * t)
                            * 0.35 * envelope * 32767);
                    writeLEShort(d, value);
                }
                d.close();
            }
            // Return a small numeric handle by storing the path; SoundPool cannot load
            // arbitrary paths with a resource ID, so this method is replaced below.
            // This line is never reached in the final initialization.
            return 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private void writeAscii(java.io.DataOutputStream d, String s) throws Exception {
        d.writeBytes(s);
    }
    private void writeLEInt(java.io.DataOutputStream d, int v) throws Exception {
        d.write(v & 255); d.write((v >> 8) & 255); d.write((v >> 16) & 255); d.write((v >> 24) & 255);
    }
    private void writeLEShort(java.io.DataOutputStream d, short v) throws Exception {
        d.write(v & 255); d.write((v >> 8) & 255);
    }

    // Reemplazamos el sistema anterior por un sonido seguro sin archivos:
    private void playTone(final boolean jump) {
        if (!soundEnabled) return;
        // Un Beep sencillo del sistema evita depender de recursos de audio.
        android.media.ToneGenerator tg = new android.media.ToneGenerator(
                android.media.AudioManager.STREAM_MUSIC,
                70
        );
        tg.startTone(jump
                ? android.media.ToneGenerator.TONE_PROP_BEEP
                : android.media.ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD,
                jump ? 70 : 180);
        postDelayed(tg::release, jump ? 100 : 220);
    }

    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);
        float w = getWidth(), h = getHeight();

        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.rgb(245, 245, 245));
        c.drawRect(0, 0, w, h, p);

        if (state == State.MENU) {
            drawMenu(c, w, h);
            return;
        }

        drawGame(c, w, h);

        if (state == State.GAME_OVER) {
            drawGameOver(c, w, h);
        }
    }

    private void drawMenu(Canvas c, float w, float h) {
        text(c, "DINO RUNNER", w/2, h*0.25f, 42, Color.rgb(35, 90, 40), true);
        text(c, "Corre, salta y evita los cactus", w/2, h*0.33f, 19, Color.DKGRAY, false);

        drawDino(c, w/2 - 40, h*0.48f, 80, 80, false);

        button(c, w/2, h*0.68f, w*0.55f, 64, "JUGAR", Color.rgb(46,125,50));
        text(c, "Mejor: " + bestScore, w/2, h*0.79f, 20, Color.DKGRAY, true);
        text(c, soundEnabled ? "🔊 Sonido activado" : "🔇 Sonido desactivado",
                w/2, h*0.87f, 16, Color.DKGRAY, false);
    }

    private void drawGame(Canvas c, float w, float h) {
        groundY = h * 0.78f;

        // Cielo.
        p.setColor(Color.rgb(235, 248, 255));
        c.drawRect(0, 0, w, groundY, p);

        // Nubes sencillas.
        p.setColor(Color.WHITE);
        c.drawCircle(w*0.18f, h*0.20f, 22, p);
        c.drawCircle(w*0.22f, h*0.19f, 30, p);
        c.drawCircle(w*0.27f, h*0.20f, 20, p);

        // Suelo.
        p.setColor(Color.rgb(90, 90, 90));
        c.drawRect(0, groundY, w, groundY + 5, p);

        drawDino(c, dinoX, dinoY, dinoW, dinoH, jumping);

        for (Obstacle o : obstacles) drawCactus(c, o.x, o.y, o.w, o.h);

        text(c, "Puntos: " + (int)score, 24, 44, 22, Color.DKGRAY, true);
        text(c, soundEnabled ? "🔊" : "🔇", w - 45, 44, 25, Color.DKGRAY, false);
    }

    private void drawGameOver(Canvas c, float w, float h) {
        p.setColor(0xB8000000);
        c.drawRect(0, 0, w, h, p);

        text(c, "GAME OVER", w/2, h*0.28f, 42, Color.WHITE, true);
        text(c, "Puntuación: " + (int)score, w/2, h*0.38f, 24, Color.WHITE, true);
        text(c, "Mejor puntuación: " + bestScore, w/2, h*0.44f, 21, Color.WHITE, false);

        button(c, w/2, h*0.59f, w*0.62f, 58, "REINTENTAR", Color.rgb(46,125,50));
        button(c, w/2, h*0.70f, w*0.62f, 58, "MENÚ", Color.rgb(80,80,80));
    }

    private void drawDino(Canvas c, float x, float y, float w, float h, boolean jumping) {
        p.setColor(Color.rgb(50, 150, 65));
        c.drawRoundRect(x, y, x+w*0.70f, y+h*0.75f, 10, 10, p);
        c.drawRoundRect(x+w*0.45f, y-h*0.20f, x+w, y+h*0.40f, 12, 12, p);

        // Ojo.
        p.setColor(Color.WHITE);
        c.drawCircle(x+w*0.78f, y-h*0.04f, 6, p);
        p.setColor(Color.BLACK);
        c.drawCircle(x+w*0.79f, y-h*0.04f, 2.5f, p);

        // Patas con animación sencilla.
        p.setColor(Color.rgb(35, 110, 45));
        float legShift = jumping ? 0 : (System.currentTimeMillis()/100)%2==0 ? 5 : -5;
        c.drawRect(x+w*0.18f, y+h*0.70f, x+w*0.29f, y+h*0.98f, p);
        c.drawRect(x+w*0.52f, y+h*0.70f+legShift, x+w*0.63f, y+h*0.98f+legShift, p);
    }

    private void drawCactus(Canvas c, float x, float y, float w, float h) {
        p.setColor(Color.rgb(30, 125, 50));
        c.drawRoundRect(x, y, x+w, y+h, 6, 6, p);
        c.drawRoundRect(x-w*0.65f, y+h*0.35f, x, y+h*0.52f, 6, 6, p);
        c.drawRoundRect(x+w, y+h*0.20f, x+w*1.65f, y+h*0.38f, 6, 6, p);
        c.drawRoundRect(x-w*0.45f, y+h*0.20f, x-w*0.25f, y+h*0.43f, 6, 6, p);
    }

    private void button(Canvas c, float cx, float cy, float width, float height,
                        String label, int color) {
        p.setColor(color);
        c.drawRoundRect(cx-width/2, cy-height/2, cx+width/2, cy+height/2, 18, 18, p);
        text(c, label, cx, cy+8, 21, Color.WHITE, true);
    }

    private void text(Canvas c, String s, float x, float y, float size,
                      int color, boolean bold) {
        p.setColor(color);
        p.setTextSize(size);
        p.setTextAlign(Paint.Align.CENTER);
        p.setTypeface(bold ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        c.drawText(s, x, y, p);
    }

    private void startGame() {
        state = State.PLAYING;
        score = 0;
        speed = Math.max(420, getWidth() * 0.45f);
        spawnTimer = 0;
        obstacles.clear();

        dinoW = Math.max(60, getWidth() * 0.13f);
        dinoH = dinoW;
        groundY = getHeight() * 0.78f;
        dinoX = getWidth() * 0.14f;
        dinoY = groundY - dinoH;

        velocityY = 0;
        jumping = false;
        scoreStart = SystemClock.uptimeMillis();
        lastFrame = SystemClock.uptimeMillis();
        invalidate();
    }

    private void gameOver() {
        state = State.GAME_OVER;
        int finalScore = (int)score;
        if (finalScore > bestScore) {
            bestScore = finalScore;
            prefs.edit().putInt("best_score", bestScore).apply();
        }
        playTone(false);
        invalidate();
    }

    private void update(float dt) {
        if (state != State.PLAYING) return;

        // La velocidad aumenta gradualmente.
        speed += dt * 7.0f;

        score += dt * 10.0f;

        // Física del salto.
        if (jumping) {
            velocityY += 1900f * dt;
            dinoY += velocityY * dt;

            if (dinoY >= groundY - dinoH) {
                dinoY = groundY - dinoH;
                velocityY = 0;
                jumping = false;
            }
        }

        // Crear cactus a intervalos variables.
        spawnTimer -= dt;
        if (spawnTimer <= 0) {
            float cw = Math.max(24, getWidth() * 0.055f);
            float ch = cw * (1.5f + random.nextFloat() * 0.9f);
            obstacles.add(new Obstacle(getWidth()+30, groundY-ch, cw, ch));

            float minDelay = Math.max(0.65f, 1.15f - speed/2200f);
            spawnTimer = minDelay + random.nextFloat() * 0.75f;
        }

        Iterator<Obstacle> it = obstacles.iterator();
        while (it.hasNext()) {
            Obstacle o = it.next();
            o.x -= speed * dt;
            if (o.x + o.w*1.7f < 0) {
                it.remove();
            } else if (collides(o)) {
                gameOver();
                return;
            }
        }
    }

    private boolean collides(Obstacle o) {
        // Hitboxes un poco más pequeñas para que el juego se sienta justo.
        float dx1 = dinoX + dinoW*0.18f;
        float dy1 = dinoY + dinoH*0.15f;
        float dx2 = dinoX + dinoW*0.86f;
        float dy2 = dinoY + dinoH*0.92f;

        float ox1 = o.x - o.w*0.25f;
        float oy1 = o.y;
        float ox2 = o.x + o.w*1.55f;
        float oy2 = o.y + o.h;

        return dx1 < ox2 && dx2 > ox1 && dy1 < oy2 && dy2 > oy1;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getAction() == MotionEvent.ACTION_DOWN) {
            touchWasDown = true;
            return true;
        }

        if (e.getAction() == MotionEvent.ACTION_UP && touchWasDown) {
            touchWasDown = false;
            float x = e.getX();
            float y = e.getY();

            if (state == State.MENU) {
                if (y > getHeight()*0.58f && y < getHeight()*0.75f) {
                    startGame();
                } else if (y > getHeight()*0.82f) {
                    soundEnabled = !soundEnabled;
                    invalidate();
                }
                return true;
            }

            if (state == State.PLAYING) {
                if (x > getWidth()-100 && y < 80) {
                    soundEnabled = !soundEnabled;
                    invalidate();
                } else if (!jumping) {
                    jumping = true;
                    velocityY = -Math.max(760, getHeight()*0.95f);
                    playTone(true);
                }
                return true;
            }

            if (state == State.GAME_OVER) {
                if (y > getHeight()*0.52f && y < getHeight()*0.66f) {
                    startGame();
                } else if (y > getHeight()*0.65f && y < getHeight()*0.78f) {
                    state = State.MENU;
                    invalidate();
                }
                return true;
            }
        }
        return true;
    }

    private final Runnable gameLoop = new Runnable() {
        @Override public void run() {
            long now = SystemClock.uptimeMillis();
            float dt = Math.min(0.035f, (now - lastFrame)/1000f);
            lastFrame = now;

            update(dt);
            invalidate();

            if (state == State.PLAYING) postOnAnimation(this);
        }
    };

    private void postOnAnimation(Runnable r) {
        postDelayed(r, 16);
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        lastFrame = SystemClock.uptimeMillis();
        removeCallbacks(gameLoop);
        postOnAnimation(gameLoop);
    }

    public void pauseGame() {
        removeCallbacks(gameLoop);
    }

    public void resumeGame() {
        lastFrame = SystemClock.uptimeMillis();
        removeCallbacks(gameLoop);
        postOnAnimation(gameLoop);
    }

    private static class Obstacle {
        float x, y, w, h;
        Obstacle(float x, float y, float w, float h) {
            this.x=x; this.y=y; this.w=w; this.h=h;
        }
    }
}
