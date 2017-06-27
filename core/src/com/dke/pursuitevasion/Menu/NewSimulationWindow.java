package com.dke.pursuitevasion.Menu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.dke.pursuitevasion.PolyMap;
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
    private boolean mapSelected, fromSimScreen;
    PolyMap map;

    public NewSimulationWindow(Skin skin, PursuitEvasion game, MenuScreen screen, PolyMap Map) {
        super("New Simulation", skin);
        this.game = game;
        this.setModal(false);
        this.skin = skin;
        this.setSize(400, 325);
        this.menuScreen = screen;
        this.setMovable(false);
        if(Map!=null){
            map = Map;
            fromSimScreen = true;
        }
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

        CheckBox graphCheckBox = new CheckBox(" Graph Searcher", skin);
        graphCheckBox.setName("GRAPHSEARCHER");
        //graphCheckBox.setChecked(true);
        CheckBox coordinateExplorer = new CheckBox(" Coordinate Explorer", skin);
        coordinateExplorer.setName("COORDINATEEXPLORER");
        coordinateExplorer.setChecked(true);

        final CheckBox disCom = new CheckBox("Disable Comms", skin);
        final ButtonGroup aiGroup = new ButtonGroup(graphCheckBox, coordinateExplorer);
        aiGroup.setMaxCheckCount(2);
        aiGroup.setMinCheckCount(0);
        aiGroup.setUncheckLast(false);

        final Label heatSizeInfo = new Label("Set PF size",skin);
        final TextField heatSize = new TextField("19",skin);
        heatSize.setAlignment(1);
        final Label pursVision = new Label("Set Vision", skin);
        final TextField pursDist = new TextField("1.5",skin);
        pursDist.setAlignment(1);

        TextButton startButton = new TextButton("Start", skin);

        startButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if(aiGroup.getAllChecked().size>0) {
                    if (file != null) {
                        if (aiGroup.getAllChecked().size == 2) {
                            game.setScreen(new SimulatorScreen(game, file, null, "BOTH", Integer.parseInt(heatSize.getText()), Float.parseFloat(pursDist.getText()), disCom.isChecked()));
                            remove();
                        } else {
                            game.setScreen(new SimulatorScreen(game, file, null, aiGroup.getChecked().getName(), Integer.parseInt(heatSize.getText()), Float.parseFloat(pursDist.getText()), disCom.isChecked()));
                            remove();
                        }
                    }
                    if (map != null) {
                        if (aiGroup.getAllChecked().size == 2) {
                            System.out.println("2");
                            game.setScreen(new SimulatorScreen(game, null, map, "BOTH", Integer.parseInt(heatSize.getText()), Float.parseFloat(pursDist.getText()), disCom.isChecked()));
                            remove();
                        } else {
                            game.setScreen(new SimulatorScreen(game, null, map, aiGroup.getChecked().getName(), Integer.parseInt(heatSize.getText()), Float.parseFloat(pursDist.getText()), disCom.isChecked()));
                            remove();
                        }
                    }
                }
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

        if(!fromSimScreen) {
            table.add(selectMapButton).width(150).center().colspan(2).padBottom(9f);
            table.row();
        }

        table.add(coordinateExplorer).width(150).padBottom(2f);
        table.add(graphCheckBox).width(150).padBottom(2f);
        table.row();
        table.add(disCom).padBottom(8f);
        table.row();
        table.add(heatSizeInfo);
        table.add(pursVision);
        table.row();
        table.add(heatSize).padRight(3f);
        table.add(pursDist).padLeft(3f);
        table.row();
        table.add(startButton).width(150).center().colspan(2).padTop(7f);
        table.row();
        table.add(backButton).width(150).center().colspan(2).padTop(4f);
        table.row();
        table.center();
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
