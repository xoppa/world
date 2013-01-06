package com.xoppa.android.loaders.scene;

import java.io.IOException;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.assets.loaders.SynchronousAssetLoader;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.btVehicleTuning;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.XmlReader;
import com.xoppa.android.loaders.scene.orge.OgreSceneLoader;
import com.xoppa.android.world.Entity;
import com.xoppa.android.world.Scene;
import com.xoppa.android.world.systems.BulletVehicle;
import com.xoppa.android.world.systems.BulletVehicle.ConstructInfo;

public class VehicleLoader extends SynchronousAssetLoader<BulletVehicle.ConstructInfo, AssetLoaderParameters<BulletVehicle.ConstructInfo>> {

	public VehicleLoader(FileHandleResolver resolver) {
		super(resolver);
	}

	public static ConstructInfo load(AssetManager assetManager, XmlReader.Element element, String basePath) {
		BulletVehicle.ConstructInfo result = new BulletVehicle.ConstructInfo();
		result.tuning = new btVehicleTuning();
		result.scene = assetManager.get(basePath+"/"+element.getAttribute("scene"), Scene.class);
		result.chassis = element.getAttribute("chassis");
		final String[] coordinates = element.getAttribute("coordinates", "0;1;2").split(";", result.coordinateSystem.length);
		for (int i = 0; i < coordinates.length; i++)
			result.coordinateSystem[i] = Integer.parseInt(coordinates[i]);
		final int n = element.getChildCount();
		int idx = 0;
		for (int i = 0; i < n; i++) {
			final XmlReader.Element child = element.getChild(i);
			if (child.getName().compareTo("wheel")!=0) 
				continue;
			final BulletVehicle.ConstructInfo.Wheel wheel = new BulletVehicle.ConstructInfo.Wheel(idx==0?null:result.wheels.get(idx-1));
			wheel.node = child.getAttribute("node");
			wheel.isFrontWheel = child.getBoolean("front", wheel.isFrontWheel);
			wheel.rollInfluence = child.getFloat("rollInfluence", wheel.rollInfluence);
			wheel.frictionSlip = child.getFloat("frictionSlip", wheel.frictionSlip);
			XmlReader.Element sub = child.getChildByName("direction");
			if (sub != null) wheel.direction = new Vector3(sub.getFloat("x", 0f), sub.getFloat("y", 0f), sub.getFloat("z", 0f));
			sub = child.getChildByName("axle");
			if (sub != null) wheel.axle = new Vector3(sub.getFloat("x", 0f), sub.getFloat("y", 0f), sub.getFloat("z", 0f));
			sub = child.getChildByName("suspension");
			if (sub != null) {
				wheel.suspensionRestLength = sub.getFloat("restLength", wheel.suspensionRestLength);
				wheel.suspensionStiffness = sub.getFloat("stiffness", wheel.suspensionStiffness);
			}
			sub = child.getChildByName("damping");
			if (sub != null) {
				wheel.wheelsDampingRelaxation = sub.getFloat("relaxation", wheel.wheelsDampingRelaxation);
				wheel.wheelsDampingCompression = sub.getFloat("compression", wheel.wheelsDampingCompression);
			}
			result.wheels.add(wheel);
			idx++;
		}
		return result;
	}
	
	@Override
	public ConstructInfo load(AssetManager assetManager, String fileName, AssetLoaderParameters<ConstructInfo> parameter) {
		FileHandle file = resolve(fileName);
		XmlReader reader = new XmlReader();
		try {
			return load(assetManager, reader.parse(file), file.parent().path());
		} catch(IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public Array<AssetDescriptor> getDependencies(String fileName, AssetLoaderParameters<ConstructInfo> parameter) {
		FileHandle file = resolve(fileName);
		XmlReader reader = new XmlReader();
		XmlReader.Element root;
		try {
			root = reader.parse(file);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		if (root.getName().compareTo("vehicle")==0) {
			String scene = root.getAttribute("scene", "");
			if (scene.length() > 0) {
				Array<AssetDescriptor> result = new Array<AssetDescriptor>(1);
				result.add(new AssetDescriptor(file.parent().child(scene).path(), Scene.class, new OgreSceneLoader.Parameters(Entity.ConstructInfo.class, ".entity")));
				return result;
			}
		}
		return null;
	}

}
