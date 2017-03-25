package com.dke.pursuitevasion.Menu;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.dke.pursuitevasion.PursuitEvasion;
import com.dke.pursuitevasion.Simulator.SimulatorScreen;

/**
 * Created by Nicola Gheza on 27/02/2017.
 */
public class NewSimulationWindow extends Window {

    private PursuitEvasion game;
    private final MenuScreen menuScreen;
    private Skin skin;
    private FileHandle file;
    private boolean mapSelected;

    public NewSimulationWindow(Skin skin, PursuitEvasion game, MenuScreen screen) {
        super("New Simulation", skin);
        this.game = game;
        this.setModal(true);
        this.skin = skin;
        this.setSize(400, 400);
        this.menuScreen = screen;

        initGUI();

    }

    private void initGUI() {
        int buttonWidth = 120, buttonHeight = 30;
        int padding = 30;

        TextButton selectMapButton = new TextButton("Select map", skin);
        selectMapButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                showFileChooser();
            }
        });
        add(selectMapButton);

        TextButton startButton = new TextButton("Start", skin);
        startButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new SimulatorScreen(game, file));
                remove();
            }
        });
        add(startButton);
    }

    private void showFileChooser() {
        menuScreen.chooseFile();
    }

    public void setFile(FileHandle file) {
        this.file = file;
    }

    public void setMapSelected() {
        mapSelected = true;
    }
}
