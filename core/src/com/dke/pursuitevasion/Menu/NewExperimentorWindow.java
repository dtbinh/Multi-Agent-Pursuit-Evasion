package com.dke.pursuitevasion.Menu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.dke.pursuitevasion.PursuitEvasion;

/**
 * Created by callum on 20/06/17.
 */
public class NewExperimentorWindow extends Window {
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

        generateButton.addListener(new ChangeListener(){
           @Override
            public void changed(ChangeEvent event, Actor actor){
               float densityThreshold = Float.valueOf(densityInput.getText());
               int number = Integer.valueOf(numberInput.getText());
               System.out.println("Density Threshold: " + densityThreshold);
               System.out.println("Number: " + number);

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
        add(table);
    }
}
