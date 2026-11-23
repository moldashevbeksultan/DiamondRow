package com.shinamy.diamondrow;

import com.badlogic.gdx.math.MathUtils;

public class Board {
    public static final int SIZE = 8;
    public Gem[][] grid = new Gem[SIZE][SIZE];

    public Board() {
        fillInitialBoard();
    }

    // Заполнение доски без готовых "три в ряд" на старте
    public void fillInitialBoard() {
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                int type;
                do {
                    type = MathUtils.random(Gem.NUM_TYPES - 1);
                } while ((c >= 2 && grid[r][c - 1].type == type && grid[r][c - 2].type == type) ||
                    (r >= 2 && grid[r - 1][c].type == type && grid[r - 2][c].type == type));

                grid[r][c] = new Gem(type, r, c);
            }
        }
    }

    public void swap(int r1, int c1, int r2, int c2) {
        Gem temp = grid[r1][c1];
        grid[r1][c1] = grid[r2][c2];
        grid[r2][c2] = temp;

        grid[r1][c1].row = r1;
        grid[r1][c1].col = c1;
        grid[r2][c2].row = r2;
        grid[r2][c2].col = c2;
    }

    // Поиск всех совпадений по горизонтали и вертикали. Возвращает количество найденных алмазов.
    public int findMatches() {
        boolean[][] toMatch = new boolean[SIZE][SIZE];

        // Горизонтальная проверка
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE - 2; c++) {
                int t = grid[r][c].type;
                if (t == grid[r][c + 1].type && t == grid[r][c + 2].type) {
                    toMatch[r][c] = true;
                    toMatch[r][c + 1] = true;
                    toMatch[r][c + 2] = true;
                }
            }
        }

        // Вертикальная проверка
        for (int c = 0; c < SIZE; c++) {
            for (int r = 0; r < SIZE - 2; r++) {
                int t = grid[r][c].type;
                if (t == grid[r + 1][c].type && t == grid[r + 2][c].type) {
                    toMatch[r][c] = true;
                    toMatch[r + 1][c] = true;
                    toMatch[r + 2][c] = true;
                }
            }
        }

        int matchCount = 0;
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (toMatch[r][c]) {
                    grid[r][c].matched = true;
                    matchCount++;
                }
            }
        }
        return matchCount;
    }

    // Удаление совпавших алмазов, падение верхних и спавн новых
    public void collapseAndRefill() {
        for (int c = 0; c < SIZE; c++) {
            int writeRow = 0;
            // Сдвигаем уцелевшие алмазы вниз
            for (int r = 0; r < SIZE; r++) {
                if (!grid[r][c].matched) {
                    if (writeRow != r) {
                        grid[writeRow][c] = grid[r][c];
                        grid[writeRow][c].row = writeRow;
                        grid[writeRow][c].col = c;
                    }
                    writeRow++;
                }
            }
            // Создаем новые алмазы сверху за пределами экрана (для эффекта падения)
            int spawnOffset = 0;
            for (int r = writeRow; r < SIZE; r++) {
                int type = MathUtils.random(Gem.NUM_TYPES - 1);
                Gem newGem = new Gem(type, r, c);
                newGem.visualY = SIZE + spawnOffset;
                grid[r][c] = newGem;
                spawnOffset++;
            }
        }
    }

    public boolean isStable() {
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (!grid[r][c].isAtTarget()) return false;
            }
        }
        return true;
    }

    public void update(float delta) {
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                grid[r][c].update(delta);
            }
        }
    }
}
