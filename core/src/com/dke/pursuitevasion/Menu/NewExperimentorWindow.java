package com.dke.pursuitevasion.Menu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.dke.pursuitevasion.*;
import java.util.ArrayList;

/**
 * Created by callum on 20/06/17.
 */
public class NewExperimentorWindow extends Window {
    private String name;
    private Skin skin;
    private PursuitEvasion game;
    private final MenuScreen menuScreen;

    public NewExperimentorWindow(Skin skin, PursuitEvasion game, MenuScreen screen){
        super("New Experimentor", skin);
        this.game = game;
        this.skin = skin;
        this.setSize(Gdx.graphics.getWidth() / 2, Gdx.graphics.getHeight() / 2);
        this.menuScreen = screen;
        this.setMovable(false);
        initGUI();
    }

    public void initGUI(){
        Table table = new Table();
        table.setSize(800,480);

        Label densityLabel = new Label("Density", skin);
        final TextField densityInput = new TextField("", skin);
        Label numberLabel = new Label("Number to generate", skin);
        final TextField numberInput = new TextField("",skin);
        TextButton generateButton = new TextButton("Generate", skin);
        TextButton backButton = new TextButton("Back",skin);

        generateButton.addListener(new ChangeListener(){
           @Override
            public void changed(ChangeEvent event, Actor actor){
               float densityThreshold = Float.valueOf(densityInput.getText());
               int number = Integer.valueOf(numberInput.getText());
               System.out.println("Density Threshold: " + densityThreshold);
               System.out.println("Number: " + number);
               System.out.println((float) Math.floor((Math.random()*4)));
               generateMap();
           }
        });

        backButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                          remove();
            }
        });

        table.add(densityLabel);
        table.add(densityInput).width(150);
        table.row();
        table.add(numberLabel);
        table.add(numberInput).width(150);
        table.row();
        table.add();
        table.add(generateButton);
        table.add(backButton);
        add(table);
    }

    public PolyMap generateMap(){
        PolyMap map = new PolyMap("POLYMAPTEST");
        map.setPolygonMesh(map.generateMesh());
        map.setEdgeVectors(new ArrayList<EdgeVectors>());
        map.setAgentsInfo(new ArrayList<AgentInfo>());
        map.setEvaderInfo(new ArrayList<EvaderInfo>());
        map.setWalls(new ArrayList<WallInfo>());
        map.export();
        return map;
    }







}

