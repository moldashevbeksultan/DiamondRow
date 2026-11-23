package com.shinamy.diamondrow;

import com.badlogic.gdx.graphics.Color;

public class Gem {
    public static final int NUM_TYPES = 5;
    public static final Color[] COLORS = {
        new Color(0.2f, 0.6f, 1.0f, 1f),   // Синий (Blue)
        new Color(0.95f, 0.25f, 0.25f, 1f), // Красный (Red)
        new Color(0.2f, 0.85f, 0.35f, 1f),  // Зеленый (Green)
        new Color(1.0f, 0.85f, 0.1f, 1f),   // Желтый (Yellow)
        new Color(0.75f, 0.3f, 0.95f, 1f)   // Фиолетовый (Purple)
    };

    public int type;
    public int row;
    public int col;

    // Визуальные координаты для плавной анимации падения и обмена
    public float visualX;
    public float visualY;
    public float scale = 1.0f;
    public boolean matched = false;

    public Gem(int type, int row, int col) {
        this.type = type;
        this.row = row;
        this.col = col;
        this.visualX = col;
        this.visualY = row;
    }

    public Color getColor() {
        return COLORS[type];
    }

    public void update(float delta) {
        float speed = 12f * delta;
        visualX += (col - visualX) * Math.min(1f, speed);
        visualY += (row - visualY) * Math.min(1f, speed);

        if (matched) {
            scale = Math.max(0f, scale - 5f * delta);
        } else if (scale < 1.0f) {
            scale = Math.min(1.0f, scale + 5f * delta);
        }
    }

    public boolean isAtTarget() {
        return Math.abs(col - visualX) < 0.02f && Math.abs(row - visualY) < 0.02f && !matched;
    }
}
