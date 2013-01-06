package com.xoppa.android.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;

public abstract class ManagedGame extends Game {
	protected IManagedScreen mSplashScreen;
	protected IManagedScreen mMainScreen;
	
	protected abstract IManagedScreen onCreateSplashScreen();
	protected abstract void onStartLoadingGlobalResources();
	protected abstract IManagedScreen onCreateMainScreen();
	
	public abstract String getTitle();
	public abstract boolean getUseGL20();
	
	@Override
	public void create() {
		showSplashScreen();
		onStartLoadingGlobalResources();
	}
	
	@Override
	public void dispose() {
		super.dispose();
		if (mSplashScreen != null) {
			mSplashScreen.dispose();
			mSplashScreen = null;
		}
		if (mMainScreen != null) {
			mMainScreen.dispose();
			mMainScreen = null;
		}
	}
	
	public void showSplashScreen() {
		if (mSplashScreen == null)
			mSplashScreen = onCreateSplashScreen();
		if (mSplashScreen != null)
			setScreen(mSplashScreen);
	}
	
	public void globalResourcesLoaded() {
		showMainScreen();
	}
	
	public void showMainScreen() {
		if (mMainScreen == null)
			mMainScreen = onCreateMainScreen();
		if (mMainScreen != null)
			setScreen(mMainScreen);
		if (mSplashScreen != null) {
			mSplashScreen.dispose();
			mSplashScreen = null;
		}
	}

	@Override
	public void setScreen(Screen screen) {
		super.setScreen(screen);
	}
	@Override
	public Screen getScreen() {
		return super.getScreen();
	}	
}
