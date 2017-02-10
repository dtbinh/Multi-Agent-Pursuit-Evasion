package com.dke.pursuitevasion;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;
import com.dke.pursuitevasion.Menu.MenuScreen;

public class PursuitEvasion extends Game {
	private MenuScreen menuScreen;
	private Screen previousScreen = null;

	@Override
	public void create () {
		menuScreen = new MenuScreen(this);
		setScreen(menuScreen);
	}

}
