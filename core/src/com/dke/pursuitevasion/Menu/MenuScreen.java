package com.dke.pursuitevasion.Menu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.dke.pursuitevasion.Editor.MapEditorScreen;
import com.dke.pursuitevasion.PursuitEvasion;

/**
 * Created by jeeza on 10-2-17.
 */
public class MenuScreen implements Screen {
    private final PursuitEvasion game;
    private SpriteBatch batch;
    private Stage stage;
    private Skin skin;

    public MenuScreen(PursuitEvasion game) {
        this.game = game;
        batch = new SpriteBatch();
        stage = new Stage();
        skin = new Skin(Gdx.files.internal("uiskin.json"));

        TextButton buttonMapBuilder = new TextButton("Map Builder", skin);
        buttonMapBuilder.setPosition(550, 350);
        buttonMapBuilder.setSize(200, 50);
        stage.addActor(buttonMapBuilder);
        buttonMapBuilder.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                newMapBuilder();
            }
        });
    }

    private void newMapBuilder() {
        game.setScreen(new MapEditorScreen(game));
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(1, 1, 1, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {

    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {

    }

    @Override
    public void dispose() {
        batch.dispose();
    }
}
