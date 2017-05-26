package com.dke.pursuitevasion.Menu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
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
        this.setModal(false);
        this.skin = skin;
        this.setSize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        this.menuScreen = screen;
        this.setMovable(false);
        initGUI();
    }

    private void initGUI() {
        int buttonWidth = 120, buttonHeight = 100;
        int padding = 30;

        Table table=new Table();
        table.setSize(800, 480);

        TextButton selectMapButton = new TextButton("Select map", skin);
        //selectMapButton.setOrigin(140,50);
        selectMapButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                showFileChooser();
            }
        });

        TextButton startButton = new TextButton("Start", skin);

        startButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                    game.setScreen(new SimulatorScreen(game, file));
                    remove();
            }
        });
        //selectMapButton.setPosition(140,150);


        TextButton backButton = new TextButton("Back", skin);
        backButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor){
                remove();
            }
        });
        //selectMapButton.setPosition(140,250);

        table.add(selectMapButton).width(150);
        table.row();
        table.add(startButton).width(150);
        table.row();
        table.add(backButton).width(150);
        table.row();
        add(table);
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
