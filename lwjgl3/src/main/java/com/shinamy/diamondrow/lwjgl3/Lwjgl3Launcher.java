package com.shinamy.diamondrow.lwjgl3;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3NativesLoader;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.shinamy.diamondrow.DiamondGame;

import java.io.File;

public class Lwjgl3Launcher {
    public static void main(String[] args) {
        Lwjgl3NativesLoader.load();

        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Diamond Row");
        config.setWindowedMode(480, 800);

        String iconPath = createTempDiamondIcon(64);
        if (iconPath != null) {
            config.setWindowIcon(Files.FileType.Absolute, iconPath);
        }

        new Lwjgl3Application(new DiamondGame(), config);
    }

    private static String createTempDiamondIcon(int size) {
        try {
            File tempFile = File.createTempFile("diamond_icon_", ".png");
            tempFile.deleteOnExit();

            Pixmap pm = new Pixmap(size, size, Pixmap.Format.RGBA8888);
            pm.setColor(new Color(0.11f, 0.12f, 0.16f, 1f));
            pm.fill();

            int cx = size / 2;
            int cy = size / 2;
            int r = (int) (size * 0.42f);

            pm.setColor(new Color(0.2f, 0.6f, 1.0f, 1f));
            pm.fillTriangle(cx, cy - r, cx - r, cy, cx + r, cy);
            pm.fillTriangle(cx, cy + r, cx - r, cy, cx + r, cy);

            int rInner = (int) (r * 0.55f);
            pm.setColor(new Color(0.75f, 0.9f, 1.0f, 1f));
            pm.fillTriangle(cx, cy - rInner, cx - rInner, cy, cx + rInner, cy);
            pm.fillTriangle(cx, cy + rInner, cx - rInner, cy, cx + rInner, cy);

            PixmapIO.writePNG(new FileHandle(tempFile), pm);
            pm.dispose();
            return tempFile.getAbsolutePath();
        } catch (Exception e) {
            return null;
        }
    }
}
