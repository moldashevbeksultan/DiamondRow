package com.shinamy.diamondrow;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class DiamondGame extends ApplicationAdapter {
    private static final float WORLD_WIDTH = 480f;
    private static final float WORLD_HEIGHT = 800f;
    private static final float CELL_SIZE = 54f;
    private static final float BOARD_OFFSET_X = (WORLD_WIDTH - Board.SIZE * CELL_SIZE) / 2f;
    private static final float BOARD_OFFSET_Y = 120f;

    private OrthographicCamera camera;
    private Viewport viewport;
    private ShapeRenderer shapeRenderer;
    private SpriteBatch batch;
    private BitmapFont font;
    private GlyphLayout layout;
    private Preferences prefs;

    private Board board;

    private enum State { IDLE, SWAPPING, REVERTING, FALLING, PAUSED, GAME_OVER }
    private State state = State.IDLE;

    private int level = 1;
    private int score = 0;
    private int highScore = 0;
    private int movesLeft = 20;
    private int targetScore = 200;

    private int selectedRow = -1;
    private int selectedCol = -1;
    private int swapR1, swapC1, swapR2, swapC2;
    private float dragStartX, dragStartY;
    private boolean isDragging = false;
    private float matchTimer = 0f;

    @Override
    public void create() {
        camera = new OrthographicCamera();
        viewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT, camera);
        shapeRenderer = new ShapeRenderer();
        batch = new SpriteBatch();
        font = new BitmapFont();
        font.getData().setScale(1.8f);
        layout = new GlyphLayout();

        prefs = Gdx.app.getPreferences("DiamondRowPrefs");
        highScore = prefs.getInteger("highScore", 0);

        startLevel(1, true);
        setupInput();
    }

    private void startLevel(int lvl, boolean resetScore) {
        this.level = lvl;
        if (resetScore) {
            this.score = 0;
        }
        if (lvl == 1) {
            targetScore = 200;
            movesLeft = 20;
        } else if (lvl == 2) {
            targetScore = 500;
            movesLeft = 25;
        } else {
            targetScore = -1;
            movesLeft = -1;
        }
        board = new Board();
        selectedRow = -1;
        selectedCol = -1;
        isDragging = false;
        state = State.IDLE;
    }

    private void setupInput() {
        Gdx.input.setInputProcessor(new InputAdapter() {
            private final Vector3 touchPos = new Vector3();

            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                viewport.unproject(touchPos.set(screenX, screenY, 0));

                if (state == State.PAUSED) {
                    // Кнопка RESUME
                    if (touchPos.x >= 140 && touchPos.x <= 340 && touchPos.y >= 390 && touchPos.y <= 460) {
                        state = State.IDLE;
                    }
                    // Кнопка EXIT
                    if (touchPos.x >= 140 && touchPos.x <= 340 && touchPos.y >= 290 && touchPos.y <= 360) {
                        Gdx.app.exit();
                    }
                    return true;
                }

                if (state == State.GAME_OVER) {
                    // Кнопка RESTART
                    if (touchPos.x >= 140 && touchPos.x <= 340 && touchPos.y >= 300 && touchPos.y <= 370) {
                        startLevel(1, true);
                    }
                    return true;
                }

                if (state != State.IDLE) return true;

                // Кнопка STOP в правом верхнем углу
                if (touchPos.x >= 365 && touchPos.x <= 455 && touchPos.y >= 735 && touchPos.y <= 780) {
                    state = State.PAUSED;
                    selectedRow = -1;
                    selectedCol = -1;
                    isDragging = false;
                    return true;
                }

                int col = (int) ((touchPos.x - BOARD_OFFSET_X) / CELL_SIZE);
                int row = (int) ((touchPos.y - BOARD_OFFSET_Y) / CELL_SIZE);

                if (isValidCell(row, col)) {
                    dragStartX = touchPos.x;
                    dragStartY = touchPos.y;
                    isDragging = true;

                    if (selectedRow != -1 && isAdjacent(selectedRow, selectedCol, row, col)) {
                        initiateSwap(selectedRow, selectedCol, row, col);
                        selectedRow = -1;
                        selectedCol = -1;
                        isDragging = false;
                    } else {
                        selectedRow = row;
                        selectedCol = col;
                    }
                }
                return true;
            }

            @Override
            public boolean touchDragged(int screenX, int screenY, int pointer) {
                if (!isDragging || state != State.IDLE || selectedRow == -1) return false;

                viewport.unproject(touchPos.set(screenX, screenY, 0));
                float dx = touchPos.x - dragStartX;
                float dy = touchPos.y - dragStartY;
                float threshold = CELL_SIZE * 0.4f;

                if (Math.abs(dx) > threshold || Math.abs(dy) > threshold) {
                    int targetRow = selectedRow;
                    int targetCol = selectedCol;

                    if (Math.abs(dx) > Math.abs(dy)) {
                        targetCol += (dx > 0) ? 1 : -1;
                    } else {
                        targetRow += (dy > 0) ? 1 : -1;
                    }

                    if (isValidCell(targetRow, targetCol)) {
                        initiateSwap(selectedRow, selectedCol, targetRow, targetCol);
                        selectedRow = -1;
                        selectedCol = -1;
                    }
                    isDragging = false;
                }
                return true;
            }

            @Override
            public boolean touchUp(int screenX, int screenY, int pointer, int button) {
                isDragging = false;
                return true;
            }
        });
    }

    private void initiateSwap(int r1, int c1, int r2, int c2) {
        swapR1 = r1; swapC1 = c1;
        swapR2 = r2; swapC2 = c2;
        board.swap(r1, c1, r2, c2);
        state = State.SWAPPING;
    }

    private boolean isValidCell(int r, int c) {
        return r >= 0 && r < Board.SIZE && c >= 0 && c < Board.SIZE;
    }

    private boolean isAdjacent(int r1, int c1, int r2, int c2) {
        return Math.abs(r1 - r2) + Math.abs(c1 - c2) == 1;
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();
        if (state != State.PAUSED) {
            updateLogic(delta);
        }

        Gdx.gl.glClearColor(0.11f, 0.12f, 0.16f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        viewport.apply();
        shapeRenderer.setProjectionMatrix(camera.combined);
        batch.setProjectionMatrix(camera.combined);

        drawBoardAndGems();
        drawUI();
    }

    private void updateLogic(float delta) {
        board.update(delta);

        switch (state) {
            case SWAPPING:
                if (board.isStable()) {
                    int matched = board.findMatches();
                    if (matched > 0) {
                        addScore(matched * 10);
                        if (movesLeft > 0) movesLeft--;
                        matchTimer = 0.22f;
                        state = State.FALLING;
                    } else {
                        board.swap(swapR1, swapC1, swapR2, swapC2);
                        state = State.REVERTING;
                    }
                }
                break;

            case REVERTING:
                if (board.isStable()) {
                    state = State.IDLE;
                }
                break;

            case FALLING:
                if (matchTimer > 0) {
                    matchTimer -= delta;
                    if (matchTimer <= 0) {
                        board.collapseAndRefill();
                    }
                } else if (board.isStable()) {
                    int cascadeMatched = board.findMatches();
                    if (cascadeMatched > 0) {
                        addScore(cascadeMatched * 10);
                        matchTimer = 0.22f;
                    } else {
                        checkLevelProgression();
                    }
                }
                break;

            default:
                break;
        }
    }

    private void addScore(int points) {
        score += points;
        if (score > highScore) {
            highScore = score;
            prefs.putInteger("highScore", highScore);
            prefs.flush();
        }
    }

    private void checkLevelProgression() {
        if (targetScore > 0 && score >= targetScore) {
            startLevel(level + 1, false);
        } else if (movesLeft == 0) {
            state = State.GAME_OVER;
        } else {
            state = State.IDLE;
        }
    }

    private void drawBoardAndGems() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                if ((r + c) % 2 == 0) {
                    shapeRenderer.setColor(0.16f, 0.18f, 0.24f, 1f);
                } else {
                    shapeRenderer.setColor(0.14f, 0.15f, 0.20f, 1f);
                }
                shapeRenderer.rect(BOARD_OFFSET_X + c * CELL_SIZE, BOARD_OFFSET_Y + r * CELL_SIZE, CELL_SIZE, CELL_SIZE);
            }
        }

        if (selectedRow != -1 && selectedCol != -1 && state == State.IDLE) {
            shapeRenderer.setColor(1f, 1f, 1f, 0.25f);
            shapeRenderer.rect(BOARD_OFFSET_X + selectedCol * CELL_SIZE, BOARD_OFFSET_Y + selectedRow * CELL_SIZE, CELL_SIZE, CELL_SIZE);
        }

        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                Gem gem = board.grid[r][c];
                if (gem.scale <= 0.05f) continue;

                float cx = BOARD_OFFSET_X + (gem.visualX + 0.5f) * CELL_SIZE;
                float cy = BOARD_OFFSET_Y + (gem.visualY + 0.5f) * CELL_SIZE;
                float radius = (CELL_SIZE * 0.40f) * gem.scale;

                Color base = gem.getColor();

                shapeRenderer.setColor(base);
                drawDiamond(cx, cy, radius);

                shapeRenderer.setColor(
                    Math.min(1f, base.r + 0.35f),
                    Math.min(1f, base.g + 0.35f),
                    Math.min(1f, base.b + 0.35f),
                    1f
                );
                drawDiamond(cx, cy + radius * 0.1f, radius * 0.55f);
            }
        }

        // Кнопка STOP
        shapeRenderer.setColor(state == State.PAUSED ? Color.GRAY : new Color(0.8f, 0.22f, 0.22f, 1f));
        shapeRenderer.rect(365, 735, 90, 45);

        if (state == State.PAUSED) {
            shapeRenderer.setColor(0.05f, 0.05f, 0.08f, 0.90f);
            shapeRenderer.rect(40, 250, WORLD_WIDTH - 80, 280);

            // Кнопка RESUME
            shapeRenderer.setColor(0.2f, 0.7f, 0.35f, 1f);
            shapeRenderer.rect(140, 390, 200, 70);

            // Кнопка EXIT
            shapeRenderer.setColor(0.8f, 0.22f, 0.22f, 1f);
            shapeRenderer.rect(140, 290, 200, 70);
        }

        if (state == State.GAME_OVER) {
            shapeRenderer.setColor(0.05f, 0.05f, 0.08f, 0.90f);
            shapeRenderer.rect(40, 260, WORLD_WIDTH - 80, 280);

            // Кнопка RESTART
            shapeRenderer.setColor(0.2f, 0.7f, 0.35f, 1f);
            shapeRenderer.rect(140, 300, 200, 70);
        }

        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void drawDiamond(float cx, float cy, float r) {
        shapeRenderer.triangle(cx, cy + r, cx - r * 0.85f, cy, cx + r * 0.85f, cy);
        shapeRenderer.triangle(cx, cy - r, cx - r * 0.85f, cy, cx + r * 0.85f, cy);
    }

    private void drawUI() {
        batch.begin();

        font.getData().setScale(1.7f);
        font.setColor(Color.WHITE);
        font.draw(batch, "DIAMOND ROW", 25f, 768f);

        font.getData().setScale(1.3f);
        font.setColor(Color.WHITE);
        drawCenteredText("STOP", 410f, 763f);

        font.getData().setScale(1.4f);
        font.setColor(Color.LIGHT_GRAY);
        font.draw(batch, "BEST: " + highScore, 25f, 715f);
        font.draw(batch, "SCORE: " + score, 25f, 680f);

        if (level <= 2) {
            font.setColor(Color.GOLD);
            font.draw(batch, "LEVEL: " + level + "  (GOAL: " + targetScore + ")", 25f, 635f);
            font.setColor(movesLeft <= 5 ? Color.SALMON : Color.CYAN);
            font.draw(batch, "MOVES: " + movesLeft, 320f, 680f);
        } else {
            font.setColor(Color.GREEN);
            font.draw(batch, "LEVEL 3: ENDLESS MODE", 25f, 635f);
            font.setColor(Color.CYAN);
            font.draw(batch, "MOVES: INF", 320f, 680f);
        }

        if (state == State.PAUSED) {
            font.getData().setScale(2.0f);
            font.setColor(Color.GOLD);
            drawCenteredText("PAUSED", WORLD_WIDTH / 2f, 495f);

            font.getData().setScale(1.6f);
            font.setColor(Color.WHITE);
            drawCenteredText("RESUME", WORLD_WIDTH / 2f, 432f);
            drawCenteredText("EXIT", WORLD_WIDTH / 2f, 332f);
        }

        if (state == State.GAME_OVER) {
            font.getData().setScale(2.2f);
            font.setColor(Color.SALMON);
            drawCenteredText("GAME OVER", WORLD_WIDTH / 2f, 495f);

            font.getData().setScale(1.5f);
            font.setColor(Color.WHITE);
            drawCenteredText("FINAL SCORE: " + score, WORLD_WIDTH / 2f, 435f);

            font.getData().setScale(1.8f);
            drawCenteredText("RESTART", WORLD_WIDTH / 2f, 345f);
        }

        batch.end();
    }

    private void drawCenteredText(String text, float x, float y) {
        layout.setText(font, text);
        font.draw(batch, text, x - layout.width / 2f, y);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void dispose() {
        shapeRenderer.dispose();
        batch.dispose();
        font.dispose();
    }
}
