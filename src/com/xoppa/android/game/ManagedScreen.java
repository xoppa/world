package com.xoppa.android.game;

public class ManagedScreen<T> implements IManagedScreen {
	public final T mGame;
	
	public ManagedScreen(final T pGame) {
		mGame = pGame;
	}
	
	@Override
	public void render(float delta) {
	}

	@Override
	public void resize(int width, int height) {
	}

	@Override
	public void show() {
	}

	@Override
	public void hide() {
	}

	@Override
	public void pause() {
	}

	@Override
	public void resume() {
	}

	@Override
	public void dispose() {
	}
}
