package io.freyr.app.osdc;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

import javax.imageio.ImageIO;

import io.freyr.app.osdc.util.ArrayUtils;
import io.freyr.app.osdc.util.Dialogue;
import io.freyr.app.osdc.util.Misc;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import lombok.Cleanup;
import lombok.val;
import net.openrs.cache.Archive;
import net.openrs.cache.Cache;
import net.openrs.cache.Container;
import net.openrs.cache.FileStore;
import net.openrs.cache.ReferenceTable;
import net.openrs.cache.region.Location;
import net.openrs.cache.region.Region;
import net.openrs.cache.sprite.Sprite;
import net.openrs.cache.sprite.Sprites;
import net.openrs.cache.sprite.Textures;
import net.openrs.cache.track.Track;
import net.openrs.cache.track.Tracks;
import net.openrs.cache.type.CacheIndex;
import net.openrs.cache.type.TypeListManager;
import net.openrs.cache.type.areas.AreaType;
import net.openrs.cache.type.identkits.IdentkitType;
import net.openrs.cache.type.identkits.IdentkitTypeList;
import net.openrs.cache.type.items.ItemTypeList;
import net.openrs.cache.type.npcs.NpcType;
import net.openrs.cache.type.npcs.NpcTypeList;
import net.openrs.cache.type.objects.ObjectType;
import net.openrs.cache.type.objects.ObjectTypeList;
import net.openrs.cache.type.overlays.OverlayType;
import net.openrs.cache.type.overlays.OverlayTypeList;
import net.openrs.cache.type.sequences.SequenceType;
import net.openrs.cache.type.sequences.SequenceTypeList;
import net.openrs.cache.type.spotanims.SpotAnimType;
import net.openrs.cache.type.spotanims.SpotAnimTypeList;
import net.openrs.cache.type.underlays.UnderlayType;
import net.openrs.cache.type.underlays.UnderlayTypeList;
import net.openrs.cache.type.varbits.VarBitType;
import net.openrs.cache.type.varbits.VarBitTypeList;
import net.openrs.cache.util.CompressionUtils;
import net.openrs.cache.util.XTEAManager;
import net.openrs.util.ImageUtils;

public final class Controller implements Initializable {

	@FXML
	ProgressBar progressBar;

	@FXML
	Text progressText;

	Optional<File> cacheDirectory = Optional.empty();

	private double xOffset, yOffset;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

	}
	
	@FXML
	private void dumpIdk() {
		if (!cacheDirectory.isPresent()) {
			DirectoryChooser cacheChooser = new DirectoryChooser();
			cacheChooser.setTitle("Select osrs cache.");

			Optional<File> cacheResult = Optional.ofNullable(cacheChooser.showDialog(App.getStage()));

			if (!cacheResult.isPresent()) {
				return;
			}

			cacheDirectory = cacheResult;
		}
		
		createTask(new Task<Void>() {

			@Override
			protected Void call() throws Exception {
				
				@Cleanup
				Cache cache = new Cache(FileStore.open(cacheDirectory.get().toPath().toString()));
				
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				
				@Cleanup
				DataOutputStream dos = new DataOutputStream(bos);
					
					IdentkitTypeList list = new IdentkitTypeList();
					
					list.initialize(cache);
					
					dos.writeShort(list.size());					
					
					for (int i = 0; i < list.size(); i++ ) {
						
						IdentkitType type = list.list(i);
						
						if (type.getBodyPartId() != -1) {
							dos.write(1);
							dos.writeByte(type.getBodyPartId());
						}
						
						if (type.getBodyModels() != null) {
							dos.write(2);
							dos.write(type.getBodyModels().length);
							
							for(int value : type.getBodyModels()) {
								dos.writeShort(value);
							}
						}
						
						if (type.isNonSelectable()) {
							dos.write(3);
						}
						
						if (type.getRecolorToFind() != null) {
							
							for (int index = 0; index < type.getRecolorToFind().length; index++) {
								short value = type.getRecolorToFind()[index];
								
								dos.write(40 + index);
								dos.writeShort(value);
							}							
							
						}
						
						if (type.getRecolorToReplace() != null) {
							
							for (int index = 0; index < type.getRecolorToReplace().length; index++) {
								short value = type.getRecolorToReplace()[index];
								
								dos.write(50 + index);
								dos.writeShort(value);
							}							
							
						}
						
						if (type.getHeadModels() != null) {
							
							for (int index = 0; index < type.getHeadModels().length; index++) {
								
								int headModelId = type.getHeadModels()[index];
								
								dos.write(60 + index);
								dos.writeShort(headModelId);
							}
							
						}
						
						dos.write(0);						
						
					}
					
					File dir = new File("./dump/");
					
					if (!dir.exists()) {
						dir.mkdirs();
					}
					
					@Cleanup
					FileOutputStream fos = new FileOutputStream(new File(dir, "idk.dat"));					
					fos.write(bos.toByteArray());
					
					System.out.println(String.format("Dumped %s idk definitions into 317 format.", list.size()));

					Platform.runLater(() -> {
						Dialogue.openDirectory("Would you like to view these files?", dir);
					});

				return null;
			}
			
		});
		
		
	}

	@FXML
	private void dumpItemDefs() {

		if (!cacheDirectory.isPresent()) {
			DirectoryChooser cacheChooser = new DirectoryChooser();
			cacheChooser.setTitle("Select osrs cache.");

			Optional<File> cacheResult = Optional.ofNullable(cacheChooser.showDialog(App.getStage()));

			if (!cacheResult.isPresent()) {
				return;
			}

			cacheDirectory = cacheResult;
		}

		createTask(new Task<Void>() {

			@Override
			protected Void call() throws Exception {
				try (Cache cache = new Cache(FileStore.open(cacheDirectory.get().toPath().toString()))) {

					val dir = new File("./dump/");

					if (!dir.exists()) {
						dir.mkdirs();
					}

					val list = new ItemTypeList();

					list.initialize(cache);

					@Cleanup
					DataOutputStream dat = new DataOutputStream(new FileOutputStream(new File(dir, "obj.dat")));

					@Cleanup
					DataOutputStream idx = new DataOutputStream(new FileOutputStream(new File(dir, "obj.idx")));

					val size = list.size();

					idx.writeShort(size);
					dat.writeShort(size);

					for (int index = 0; index < list.size(); index++) {

						val start = dat.size();

						val item = list.list(index);

						if (item.getInventoryModel() != 0) {
							dat.writeByte(1);
							dat.writeShort(item.getInventoryModel());
						}

						if (item.getName() != null && !item.getName().equalsIgnoreCase("Dwarf remains")
								&& !item.getName().equalsIgnoreCase("null")) {
							dat.writeByte(2);
							dat.write(item.getName().getBytes());
							dat.writeByte(10);
						}

						if (item.getZoom2d() != 2000) {
							dat.writeByte(4);
							dat.writeShort(item.getZoom2d());
						}

						if (item.getXan2d() != 0) {
							dat.writeByte(5);
							dat.writeShort(item.getXan2d());
						}

						if (item.getYan2d() != 0) {
							dat.writeByte(6);
							dat.writeShort(item.getYan2d());
						}

						if (item.getxOffset2d() != 0) {
							dat.writeByte(7);
							dat.writeShort(item.getxOffset2d());
						}

						if (item.getyOffset2d() != 0) {
							dat.writeByte(8);
							dat.writeShort(item.getyOffset2d());
						}

						if (item.isStackable()) {
							dat.writeByte(11);
						}

						if (item.getCost() != 1) {
							dat.writeByte(12);
							dat.writeInt(item.getCost());
						}

						if (item.isMembers()) {
							dat.writeByte(16);
						}

						if (item.getMaleModel0() != 0 || item.getMaleOffset() != 0) {
							dat.writeByte(23);
							dat.writeShort(item.getMaleModel0());
							dat.writeByte(item.getMaleOffset());
						}

						if (item.getMaleModel1() != 0) {
							dat.writeByte(24);
							dat.writeShort(item.getMaleModel1());
						}

						if (item.getFemaleModel0() != 0 || item.getFemaleOffset() != 0) {
							dat.writeByte(25);
							dat.writeShort(item.getFemaleModel0());
							dat.writeByte(item.getFemaleOffset());
						}

						if (item.getFemaleModel1() != 0) {
							dat.writeByte(26);
							dat.writeShort(item.getFemaleModel1());
						}

						if (!ArrayUtils.isEmpty(item.getOptions())) {
							for (int i = 0; i < item.getOptions().length; i++) {
								if (item.getOptions()[i] != null) {
									dat.writeByte(30 + i);
									dat.write(item.getOptions()[i].getBytes());
									dat.writeByte(10);
								}
							}
						}

						if (!ArrayUtils.isEmpty(item.getInterfaceOptions())) {
							for (int i = 0; i < item.getInterfaceOptions().length; i++) {
								if (item.getInterfaceOptions()[i] != null) {
									dat.writeByte(35 + i);
									dat.write(item.getInterfaceOptions()[i].getBytes());
									dat.writeByte(10);
								}
							}
						}

						if (item.getColorFind() != null || item.getColorReplace() != null) {
							dat.writeByte(40);
							dat.writeByte(item.getColorFind().length);

							for (int i = 0; i < item.getColorFind().length; i++) {
								dat.writeShort(item.getColorReplace()[i]);
								dat.writeShort(item.getColorFind()[i]);
							}
						}

						if (item.getMaleModel2() != 0) {
							dat.writeByte(78);
							dat.writeShort(item.getMaleModel2());
						}

						if (item.getFemaleModel2() != 0) {
							dat.writeByte(79);
							dat.writeShort(item.getFemaleModel2());
						}

						if (item.getMaleHeadModel() != 0) {
							dat.writeByte(90);
							dat.writeShort(item.getMaleHeadModel());
						}

						if (item.getFemaleHeadModel() != 0) {
							dat.writeByte(91);
							dat.writeShort(item.getFemaleHeadModel());
						}

						if (item.getMaleHeadModel2() != 0) {
							dat.writeByte(92);
							dat.writeShort(item.getMaleHeadModel2());
						}

						if (item.getFemaleHeadModel2() != 0) {
							dat.writeByte(93);
							dat.writeShort(item.getFemaleHeadModel2());
						}

						if (item.getZan2d() != 0) {
							dat.writeByte(95);
							dat.writeShort(item.getZan2d());
						}

						if (item.getUnnotedId() != 0) {
							dat.writeByte(97);
							dat.writeShort(item.getUnnotedId());
						}

						if (item.getNotedId() != 0) {
							dat.writeByte(98);
							dat.writeShort(item.getNotedId());
						}

						if (item.getCountObj() != null) {
							for (int i = 0; i < item.getCountObj().length; i++) {
								dat.writeByte(100 + i);
								dat.writeShort(item.getCountObj()[i]);
								dat.writeShort(item.getCountCo()[i]);
							}
						}

						if (item.getResizeX() != 0) {
							dat.writeByte(110);
							dat.writeShort(item.getResizeX());
						}

						if (item.getResizeY() != 0) {
							dat.writeByte(111);
							dat.writeShort(item.getResizeY());
						}

						if (item.getResizeZ() != 0) {
							dat.writeByte(112);
							dat.writeShort(item.getResizeZ());
						}

						if (item.getAmbient() != 0) {
							dat.writeByte(113);
							dat.writeByte(item.getAmbient());
						}

						if (item.getContrast() != 0) {
							dat.writeByte(114);
							dat.writeByte(item.getContrast());
						}

						if (item.getTeam() != 0) {
							dat.writeByte(115);
							dat.writeByte(item.getTeam());
						}

						dat.writeByte(0);

						int end = dat.size();

						idx.writeShort(end - start);

						double progress = ((double) (index + 1) / list.size()) * 100;

						updateMessage(String.format("%.2f%s", progress, "%"));
						updateProgress((index + 1), list.size());

					}

					System.out.println(String.format("Dumped %s item definitions into 317 format.", list.size()));

					Platform.runLater(() -> {
						Dialogue.openDirectory("Would you like to view these files?", dir);
					});

				}

				return null;
			}

		});
	}

	@FXML
	private void dumpNpcDefs() {

		if (!cacheDirectory.isPresent()) {
			DirectoryChooser cacheChooser = new DirectoryChooser();
			cacheChooser.setTitle("Select osrs cache.");

			Optional<File> cacheResult = Optional.ofNullable(cacheChooser.showDialog(App.getStage()));

			if (!cacheResult.isPresent()) {
				return;
			}

			cacheDirectory = cacheResult;
		}

		createTask(new Task<Void>() {

			@Override
			protected Void call() throws Exception {
				try (Cache cache = new Cache(FileStore.open(cacheDirectory.get().toPath().toString()))) {

					File dir = new File("./dump/");

					if (!dir.exists()) {
						dir.mkdirs();
					}

					NpcTypeList list = new NpcTypeList();

					list.initialize(cache);

					try (DataOutputStream dat = new DataOutputStream(new FileOutputStream(new File(dir, "npc.dat")));
							DataOutputStream idx = new DataOutputStream(
									new FileOutputStream(new File(dir, "npc.idx")))) {

						int size = list.size();

						idx.writeShort(size);
						dat.writeShort(size);

						for (int index = 0; index < list.size(); index++) {

							NpcType npc = list.list(index);

							val start = dat.size();

							if (npc.getModels() != null) {
								dat.writeByte(1);
								dat.writeByte(npc.getModels().length);

								for (int i = 0; i < npc.getModels().length; i++) {
									dat.writeShort(npc.getModels()[i]);
								}
							}

							if (npc.getName() != null && !npc.getName().equalsIgnoreCase("Dwarf Remains")
									&& !npc.getName().equalsIgnoreCase("null") && !npc.getName().isEmpty()) {
								dat.writeByte(2);
								dat.write(npc.getName().getBytes());
								dat.writeByte(10);
							}

							if (npc.getTileSpacesOccupied() != -1) {
								dat.writeByte(12);
								dat.writeByte(npc.getTileSpacesOccupied());
							}

							if (npc.getStanceAnimation() != -1) {
								dat.writeByte(13);
								dat.writeShort(npc.getStanceAnimation());
							}

							if (npc.getWalkAnimation() != -1) {
								dat.writeByte(14);
								dat.writeShort(npc.getWalkAnimation());
							}

							if (npc.getWalkAnimation() != -1 || npc.getRotate180Animation() != -1
									|| npc.getRotate90LeftAnimation() != -1 || npc.getRotate90RightAnimation() != -1) {
								dat.writeByte(17);
								dat.writeShort(npc.getWalkAnimation());
								dat.writeShort(npc.getRotate180Animation());
								dat.writeShort(npc.getRotate90RightAnimation());
								dat.writeShort(npc.getRotate90LeftAnimation());
							}

							if (!ArrayUtils.isEmpty(npc.getOptions())) {
								for (int i = 0; i < npc.getOptions().length; i++) {
									if (npc.getOptions()[i] != null) {
										dat.writeByte(i + 30);
										dat.write(npc.getOptions()[i].getBytes());
										dat.writeByte(10);
									}
								}
							}

							if (npc.getRecolorToFind() != null || npc.getRecolorToReplace() != null) {
								dat.writeByte(40);
								dat.writeByte(npc.getRecolorToFind().length);

								for (int i = 0; i < npc.getRecolorToFind().length; i++) {
									dat.writeShort(npc.getRecolorToFind()[i]);
									dat.writeShort(npc.getRecolorToReplace()[i]);
								}

							}

							if (npc.getModels_2() != null) {
								dat.writeByte(60);
								dat.writeByte(npc.getModels_2().length);

								for (int i = 0; i < npc.getModels_2().length; i++) {
									dat.writeShort(npc.getModels_2()[i]);
								}
							}

							if (!npc.isRenderOnMinimap()) {
								dat.writeByte(93);
							}

							if (npc.getCombatLevel() != -1) {
								dat.writeByte(95);
								dat.writeShort(npc.getCombatLevel());
							}

							if (npc.getResizeX() != 128) {
								dat.writeByte(97);
								dat.writeShort(npc.getResizeX());
							}

							if (npc.getResizeY() != 128) {
								dat.writeByte(98);
								dat.writeShort(npc.getResizeY());
							}

							if (npc.isHasRenderPriority()) {
								dat.writeByte(99);
							}

							if (npc.getAmbient() != 0) {
								dat.writeByte(100);
								dat.writeByte(npc.getAmbient());
							}

							if (npc.getContrast() != 0) {
								dat.writeByte(101);
								dat.writeByte(npc.getContrast());
							}

							if (npc.getHeadIcon() != -1) {
								dat.writeByte(102);
								dat.writeShort(npc.getHeadIcon());
							}

							if (npc.getAnInt2156() != 32) {
								dat.writeByte(103);
								dat.writeShort(npc.getAnInt2156());
							}

							if (npc.getAnInt2174() != -1 || npc.getAnInt2187() != -1
									|| npc.getAnIntArray2185() != null) {
								dat.writeByte(106);
								dat.writeShort(npc.getAnInt2174());
								dat.writeShort(npc.getAnInt2187());
								dat.writeByte(npc.getAnIntArray2185().length - 1);

								for (int i = 0; i < npc.getAnIntArray2185().length; i++) {
									dat.writeShort(npc.getAnIntArray2185()[i]);
								}
							}

							if (!npc.isClickable()) {
								dat.writeByte(107);
							}

							dat.writeByte(0);

							final int end = dat.size();

							idx.writeShort(end - start);

							double progress = ((double) (index + 1) / list.size()) * 100;

							updateMessage(String.format("%.2f%s", progress, "%"));
							updateProgress((index + 1), list.size());

						}

						Platform.runLater(() -> {
							Dialogue.openDirectory("Would you like to view these files?", dir);
						});

						System.out.println(String.format("Dumped %d Npc Definitions into 317 format.", list.size()));

					}
				}
				return null;
			}

		});

	}

	@FXML
	private void dumpObjectDefs() {

		if (!cacheDirectory.isPresent()) {
			DirectoryChooser cacheChooser = new DirectoryChooser();
			cacheChooser.setTitle("Select osrs cache.");

			Optional<File> cacheResult = Optional.ofNullable(cacheChooser.showDialog(App.getStage()));

			if (!cacheResult.isPresent()) {
				return;
			}

			cacheDirectory = cacheResult;
		}

		createTask(new Task<Void>() {

			@Override
			protected Void call() throws Exception {
				try (Cache cache = new Cache(FileStore.open(cacheDirectory.get().toPath().toString()))) {

					File dir = new File("./dump/");

					if (!dir.exists()) {
						dir.mkdirs();
					}

					ObjectTypeList list = new ObjectTypeList();

					list.initialize(cache);

					try (DataOutputStream dat = new DataOutputStream(new FileOutputStream(new File(dir, "loc.dat")));
							DataOutputStream idx = new DataOutputStream(
									new FileOutputStream(new File(dir, "loc.idx")))) {

						val size = list.size();

						idx.writeShort(size);
						dat.writeShort(size);

						for (int index = 0; index < list.size(); index++) {

							val start = dat.size();

							val obj = list.list(index);

							if (obj.getObjectModels() != null) {
								if (obj.getObjectTypes() != null) {
									dat.writeByte(1);
									dat.writeByte(obj.getObjectModels().length);
									if (obj.getObjectModels().length > 0) {

										for (int i = 0; i < obj.getObjectModels().length; i++) {
											dat.writeShort(obj.getObjectModels()[i]);
											dat.writeByte(obj.getObjectTypes()[i]);
										}

									}
								} else {
									dat.writeByte(5);
									dat.writeByte(obj.getObjectModels().length);
									if (obj.getObjectModels().length > 0) {

										for (int i = 0; i < obj.getObjectModels().length; i++) {
											dat.writeShort(obj.getObjectModels()[i]);
										}

									}
								}
							}

							if (obj.getName() != null && !obj.getName().equalsIgnoreCase("null")) {
								dat.writeByte(2);
								dat.write(obj.getName().getBytes());
								dat.writeByte(10);
							}

							if (obj.getSizeX() != 1) {
								dat.writeByte(14);
								dat.writeByte(obj.getSizeX());
							}

							if (obj.getSizeY() != 1) {
								dat.writeByte(15);
								dat.writeByte(obj.getSizeY());
							}

							if (obj.getAnInt2094() == 0) {
								dat.writeByte(17);
							}

							if (!obj.isaBool2114()) {
								dat.writeByte(18);
							}

							if (obj.getAnInt2088() == 1) {
								dat.writeByte(19);
								dat.writeByte(1);
							}

							if (obj.getAnInt2105() >= 0) {
								dat.writeByte(21);
							}

							if (obj.isNonFlatShading()) {
								dat.writeByte(22);
							}

							if (obj.isaBool2111()) {
								dat.writeByte(23);
							}

							if (obj.getAnimationID() != -1) {
								dat.writeByte(24);
								dat.writeShort(obj.getAnimationID());
							}

							if (obj.getAnInt2069() != 16) {
								dat.writeByte(28);
								dat.writeByte(obj.getAnInt2069());
							}

							if (obj.getAmbient() != 0) {
								dat.writeByte(29);
								dat.writeByte(obj.getAmbient());
							}

							if (obj.getContrast() != 0) {
								dat.writeByte(39);
								dat.writeByte(obj.getContrast());
							}

							if (!ArrayUtils.isEmpty(obj.getActions())) {
								for (int i = 0; i < obj.getActions().length; i++) {
									if (obj.getActions()[i] != null) {
										dat.writeByte(i + 30);
										dat.write(obj.getActions()[i].getBytes());
										dat.writeByte(10);
									}
								}
							}

							if (obj.getRecolorToFind() != null || obj.getRecolorToReplace() != null) {
								dat.writeByte(40);
								dat.writeByte(obj.getRecolorToFind().length);

								for (int i = 0; i < obj.getRecolorToFind().length; i++) {
									dat.writeShort(obj.getRecolorToFind()[i]);
									dat.writeShort(obj.getRecolorToReplace()[i]);
								}

							}

							if (obj.getRetextureToFind() != null || obj.getTextureToReplace() != null) {
								dat.writeByte(41);
								dat.writeByte(obj.getRetextureToFind().length);

								for (int i = 0; i < obj.getRetextureToFind().length; i++) {
									dat.writeShort(obj.getRetextureToFind()[i]);
									dat.writeShort(obj.getTextureToReplace()[i]);
								}

							}

							if (obj.isaBool2108()) {
								dat.writeByte(62);
							}

							if (!obj.isaBool2097()) {
								dat.writeByte(64);
							}

							if (obj.getModelSizeX() != 128) {
								dat.writeByte(65);
								dat.writeShort(obj.getModelSizeX());
							}

							if (obj.getModelSizeHeight() != 128) {
								dat.writeByte(66);
								dat.writeShort(obj.getModelSizeHeight());
							}

							if (obj.getModelSizeY() != 128) {
								dat.writeByte(67);
								dat.writeShort(obj.getModelSizeY());
							}

							if (obj.getMapSceneID() != -1) {
								dat.writeByte(68);
								dat.writeShort(obj.getMapSceneID());
							}

							if (obj.getAnInt768() != -1) {
								dat.writeByte(69);
								dat.writeByte(obj.getAnInt768());
							}

							if (obj.getOffsetX() != 0) {
								dat.writeByte(70);
								dat.writeShort(obj.getOffsetX());
							}

							if (obj.getOffsetHeight() != 0) {
								dat.writeByte(71);
								dat.writeShort(obj.getOffsetHeight());
							}

							if (obj.getOffsetY() != 0) {
								dat.writeByte(72);
								dat.writeShort(obj.getOffsetY());
							}

							if (obj.isaBool2104()) {
								dat.writeByte(73);
							}

							if (obj.isSolid()) {
								dat.writeByte(74);
							}

							if (obj.getAnInt2106() == 1) {
								dat.writeByte(75);
								dat.writeByte(obj.getAnInt2106());
							}

							// TODO fix varbits

							// if (obj.getVarpID() != -1 || obj.getConfigId() !=
							// -1 || obj.getConfigChangeDest() != null) {
							// dat.writeByte(77);
							// dat.writeShort(obj.getVarpID());
							// dat.writeShort(obj.getConfigId());
							// dat.writeByte(obj.getConfigChangeDest().length);
							//
							// for (int i = 0; i <
							// obj.getConfigChangeDest().length; i++) {
							// dat.writeShort(obj.getConfigChangeDest()[i]);
							// }
							//
							// }

							if (obj.getMapAreaId() != -1) {
								dat.writeByte(82);
								dat.writeShort(obj.getMapAreaId());
							}

							dat.writeByte(0);

							int end = dat.size();

							idx.writeShort(end - start);

							double progress = ((double) (index + 1) / list.size()) * 100;

							updateMessage(String.format("%.2f%s", progress, "%"));
							updateProgress((index + 1), list.size());

						}

						Platform.runLater(() -> {
							Dialogue.openDirectory("Would you like to view these files?", dir);
						});

						System.out.println(String.format("Dumped %d Object Definitions into 317 format.", list.size()));

					}

				}
				return null;
			}

		});

	}

	@FXML
	private void dumpAnimationDefs() {

		if (!cacheDirectory.isPresent()) {
			DirectoryChooser cacheChooser = new DirectoryChooser();
			cacheChooser.setTitle("Select osrs cache.");

			Optional<File> cacheResult = Optional.ofNullable(cacheChooser.showDialog(App.getStage()));

			if (!cacheResult.isPresent()) {
				return;
			}

			cacheDirectory = cacheResult;
		}

		createTask(new Task<Void>() {

			@Override
			protected Void call() throws Exception {
				try (Cache cache = new Cache(FileStore.open(cacheDirectory.get().toPath().toString()))) {

					File dir = new File("./dump/");

					if (!dir.exists()) {
						dir.mkdirs();
					}

					SequenceTypeList list = new SequenceTypeList();

					list.initialize(cache);

					try (DataOutputStream dat = new DataOutputStream(new FileOutputStream(new File(dir, "seq.dat")))) {

						dat.writeShort(list.size());

						for (int index = 0; index < list.size(); ++index) {
							SequenceType anim = list.list(index);
							if (anim != null) {

								if (anim.getFrameIDs() != null) {
									dat.writeByte(1);
									dat.writeShort(anim.getFrameIDs().length);

									for (int i = 0; i < anim.getFrameIDs().length; i++) {
										dat.writeInt(anim.getFrameIDs()[i]);
									}

									for (int i = 0; i < anim.getFrameIDs().length; i++) {
										dat.writeByte(anim.getFrameLengths()[i]);
									}
								}

								if (anim.getLoopOffset() != -1) {
									dat.writeByte(2);
									dat.writeShort(anim.getLoopOffset());
								}

								if (anim.getInterleaveOrder() != null) {
									dat.writeByte(3);

									dat.writeByte(anim.getInterleaveOrder().length - 1);

									for (int i = 0; i < anim.getInterleaveOrder().length - 1; i++) {
										dat.writeByte(anim.getInterleaveOrder()[i]);
									}
								}

								if (anim.isStretches()) {
									dat.writeByte(4);
								}

								if (anim.getPriority() != 5) {
									dat.writeByte(5);
									dat.writeByte(anim.getPriority());
								}

								if (anim.getLeftHandItem() != -1) {
									dat.writeByte(6);
									dat.writeShort(anim.getLeftHandItem());
								}

								if (anim.getRightHandItem() != -1) {
									dat.writeByte(7);
									dat.writeShort(anim.getRightHandItem());
								}

								if (anim.getMaximumLoops() != 99) {
									dat.writeByte(8);
									dat.writeByte(anim.getMaximumLoops());
								}

								if (anim.getAnimatingPrecedence() != -1) {
									dat.writeByte(9);
									dat.writeByte(anim.getAnimatingPrecedence());
								}

								if (anim.getWalkingPrecedence() != -1) {
									dat.writeByte(10);
									dat.writeByte(anim.getWalkingPrecedence());
								}

								if (anim.getReplayMode() != 2) {
									dat.writeByte(11);
									dat.writeByte(anim.getReplayMode());
								}
							}

							dat.writeByte(0);

							double progress = ((double) (index + 1) / list.size()) * 100;

							updateMessage(String.format("%.2f%s", progress, "%"));
							updateProgress((index + 1), list.size());

						}

						Platform.runLater(() -> {
							Dialogue.openDirectory("Would you like to view this file?", dir);
						});

						System.out.println(String.format("Dumped %d animation definitions.", list.size()));

					}

				}
				return null;
			}

		});

	}

	@FXML
	private void dumpGraphicDefs() {

		if (!cacheDirectory.isPresent()) {
			DirectoryChooser cacheChooser = new DirectoryChooser();
			cacheChooser.setTitle("Select osrs cache.");

			Optional<File> cacheResult = Optional.ofNullable(cacheChooser.showDialog(App.getStage()));

			if (!cacheResult.isPresent()) {
				return;
			}

			cacheDirectory = cacheResult;
		}

		createTask(new Task<Void>() {

			@Override
			protected Void call() throws Exception {
				try (Cache cache = new Cache(FileStore.open(cacheDirectory.get().toPath().toString()))) {

					File dir = new File("./dump/");

					if (!dir.exists()) {
						dir.mkdirs();
					}

					SpotAnimTypeList list = new SpotAnimTypeList();

					list.initialize(cache);

					try (DataOutputStream dat = new DataOutputStream(
							new FileOutputStream(new File(dir, "spotanim.dat")))) {

						dat.writeShort(list.size());

						for (int i = 0; i < list.size(); i++) {
							SpotAnimType spotanim = list.list(i);

							if (spotanim == null) {
								spotanim = list.list(1);
							}

							if (spotanim.getModelID() != 0) {
								dat.writeByte(1);
								dat.writeShort(spotanim.getModelID());
							}

							if (spotanim.getAnimationID() != -1) {
								dat.writeByte(2);
								dat.writeShort(spotanim.getAnimationID());
							}

							if (spotanim.getResizeX() != 128) {
								dat.writeByte(4);
								dat.writeShort(spotanim.getResizeX());
							}

							if (spotanim.getResizeY() != 128) {
								dat.writeByte(5);
								dat.writeShort(spotanim.getResizeY());
							}

							if (spotanim.getRotation() != 0) {
								dat.writeByte(6);
								dat.writeShort(spotanim.getRotation());
							}

							if (spotanim.getAmbient() != 0) {
								dat.writeByte(7);
								dat.writeShort(spotanim.getAmbient());
							}

							if (spotanim.getContrast() != 0) {
								dat.writeByte(8);
								dat.writeShort(spotanim.getContrast());
							}

							if (spotanim.getRecolorToFind() != null) {
								dat.writeByte(40);
								dat.writeByte(spotanim.getRecolorToFind().length);

								for (int count = 0; count < spotanim.getRecolorToFind().length; count++) {
									dat.writeShort(spotanim.getRecolorToFind()[count]);
									dat.writeShort(spotanim.getRecolorToReplace()[count]);
								}
							}

							if (spotanim.getRetextureToFind() != null) {
								dat.writeByte(41);
								dat.writeByte(spotanim.getRetextureToFind().length);

								for (int count = 0; count < spotanim.getRetextureToFind().length; count++) {
									dat.writeShort(spotanim.getRetextureToFind()[count]);
									dat.writeShort(spotanim.getRetextureToReplace()[count]);
								}
							}

							dat.writeByte(0);

							double progress = ((double) (i + 1) / list.size()) * 100;

							updateMessage(String.format("%.2f%s", progress, "%"));
							updateProgress((i + 1), list.size());
						}

						Platform.runLater(() -> {
							Dialogue.openDirectory("Would you like to view this file?", dir);
						});

						System.out.println(String.format("Dumped %d graphic definitions.", list.size()));

					}

				}
				return null;
			}

		});

	}

	@FXML
	private void dumpVarbitDefs() {

		if (!cacheDirectory.isPresent()) {
			DirectoryChooser cacheChooser = new DirectoryChooser();
			cacheChooser.setTitle("Select osrs cache.");

			Optional<File> cacheResult = Optional.ofNullable(cacheChooser.showDialog(App.getStage()));

			if (!cacheResult.isPresent()) {
				return;
			}

			cacheDirectory = cacheResult;
		}

		createTask(new Task<Void>() {

			@Override
			protected Void call() throws Exception {
				try (Cache cache = new Cache(FileStore.open(cacheDirectory.get().toPath().toString()))) {

					File dir = new File("./dump/");

					if (!dir.exists()) {
						dir.mkdirs();
					}

					VarBitTypeList list = new VarBitTypeList();

					list.initialize(cache);

					@Cleanup
					DataOutputStream dat = new DataOutputStream(new FileOutputStream(new File(dir, "varbit.dat")));

					dat.writeShort(list.size());

					for (int i = 0; i < list.size(); i++) {
						VarBitType varbit = list.list(i);

						if (varbit == null) {
							varbit = list.list(1);
						}

						dat.writeShort(varbit.getConfigID());
						dat.writeByte(varbit.getLeastSigBit());
						dat.writeByte(varbit.getMostSigBit());

						double progress = ((double) (i + 1) / list.size()) * 100;

						updateMessage(String.format("%.2f%s", progress, "%"));
						updateProgress((i + 1), list.size());

					}

					Platform.runLater(() -> {
						Dialogue.openDirectory("Would you like to view this file?", dir);
					});

					System.out.println(String.format("Dumped %d varbits", list.size()));

				}
				return null;
			}

		});

	}

	@FXML
	private void dumpFloorDefs() {

		if (!cacheDirectory.isPresent()) {
			DirectoryChooser cacheChooser = new DirectoryChooser();
			cacheChooser.setTitle("Select osrs cache.");

			Optional<File> cacheResult = Optional.ofNullable(cacheChooser.showDialog(App.getStage()));

			if (!cacheResult.isPresent()) {
				return;
			}

			cacheDirectory = cacheResult;
		}

		createTask(new Task<Void>() {

			@Override
			protected Void call() throws Exception {
				try (Cache cache = new Cache(FileStore.open(cacheDirectory.get().toPath().toString()))) {
					File dir = new File("./dump/");

					if (!dir.exists()) {
						dir.mkdirs();
					}

					try (DataOutputStream dat = new DataOutputStream(new FileOutputStream(new File(dir, "flo.dat")))) {
						UnderlayTypeList underlays = new UnderlayTypeList();
						OverlayTypeList overlays = new OverlayTypeList();

						underlays.initialize(cache);
						overlays.initialize(cache);

						dat.writeShort(underlays.size());

						for (int i = 0; i < underlays.size(); i++) {
							UnderlayType underlay = underlays.list(i);

							if (underlay != null && underlay.getRgbColor() != 0) {
								dat.writeByte(1);
								dat.writeByte(underlay.getRgbColor() >> 16);
								dat.writeByte(underlay.getRgbColor() >> 8);
								dat.writeByte(underlay.getRgbColor());
							}

							dat.writeByte(0);

						}

						dat.writeShort(overlays.size());

						for (int i = 0; i < overlays.size(); i++) {
							OverlayType overlay = overlays.list(i);

							if (overlay != null) {
								if (overlay.getRgbColor() != 0) {
									dat.writeByte(1);
									dat.writeByte(overlay.getRgbColor() >> 16);
									dat.writeByte(overlay.getRgbColor() >> 8);
									dat.writeByte(overlay.getRgbColor());
								}

								if (overlay.getTexture() != 0) {
									dat.writeByte(2);
									dat.writeByte(overlay.getTexture());
								}

								if (!overlay.isHideUnderlay()) {
									dat.writeByte(5);
								}

								if (overlay.getSecondaryRgbColor() != 0) {
									dat.writeByte(7);
									dat.writeByte(overlay.getSecondaryRgbColor() >> 16);
									dat.writeByte(overlay.getSecondaryRgbColor() >> 8);
									dat.writeByte(overlay.getSecondaryRgbColor());
								}
							}

							dat.writeByte(0);

							double progress = ((double) (i + 1) / overlays.size()) * 100;

							updateMessage(String.format("%.2f%s", progress, "%"));
							updateProgress((i + 1), overlays.size());

						}

						Platform.runLater(() -> {
							Dialogue.openDirectory("Would you like to view this file?", dir);
						});

						System.out.println(String.format("Dumped %d underlays and %d overlays into 317 format.",
								underlays.size(), overlays.size()));

					}

				}
				return null;
			}

		});

	}

	@FXML
	private void dumpModels() {

		if (!cacheDirectory.isPresent()) {
			DirectoryChooser cacheChooser = new DirectoryChooser();
			cacheChooser.setTitle("Select osrs cache.");

			Optional<File> cacheResult = Optional.ofNullable(cacheChooser.showDialog(App.getStage()));

			if (!cacheResult.isPresent()) {
				return;
			}

			cacheDirectory = cacheResult;
		}

		createTask(new Task<Void>() {

			@Override
			protected Void call() throws Exception {
				try (Cache cache = new Cache(FileStore.open(cacheDirectory.get().toPath().toString()))) {
					File dir = new File("./dump/index1/");

					if (!dir.exists()) {
						dir.mkdirs();
					}

					ReferenceTable table = cache.getReferenceTable(7);

					for (int i = 0; i < table.capacity(); i++) {
						if (table.getEntry(i) == null)
							continue;

						Container container = cache.read(7, i);
						byte[] bytes = new byte[container.getData().limit()];
						container.getData().get(bytes);

						try (DataOutputStream dos = new DataOutputStream(
								new FileOutputStream(new File(dir, i + ".gz")))) {
							dos.write(CompressionUtils.gzip(bytes));
						}

						double progress = (double) i / table.capacity() * 100;

						updateMessage(String.format("%.2f%s", progress, "%"));
						updateProgress((i + 1), table.capacity());

						System.out.printf("%.2f%s\n", progress, "%");
					}

					Platform.runLater(() -> {
						Dialogue.openDirectory("Would you like to view these files?", dir);
					});

					System.out.println(String.format("Dumped %d models.", table.capacity()));

				}
				return null;
			}

		});

	}

	@FXML
	private void dumpAnimations() {

		if (!cacheDirectory.isPresent()) {
			DirectoryChooser cacheChooser = new DirectoryChooser();
			cacheChooser.setTitle("Select osrs cache.");

			Optional<File> cacheResult = Optional.ofNullable(cacheChooser.showDialog(App.getStage()));

			if (!cacheResult.isPresent()) {
				return;
			}

			cacheDirectory = cacheResult;
		}

		createTask(new Task<Void>() {

			@Override
			protected Void call() throws Exception {
				try (Cache cache = new Cache(FileStore.open(cacheDirectory.get().toPath().toString()))) {
					File dir = new File("./dump/anims/");

					if (!dir.exists()) {
						dir.mkdirs();
					}

					ReferenceTable skeletonTable = cache.getReferenceTable(CacheIndex.SKELETONS);

					Skeleton[][] skeletons = new Skeleton[skeletonTable.capacity()][];

					for (int mainSkeletonId = 0; mainSkeletonId < skeletonTable.capacity(); mainSkeletonId++) {

						if (skeletonTable.getEntry(mainSkeletonId) == null) {
							continue;
						}

						ByteArrayOutputStream bos = new ByteArrayOutputStream();

						@Cleanup
						DataOutputStream dat = new DataOutputStream(bos);

						Archive skeletonArchive = Archive.decode(
								cache.read(CacheIndex.SKELETONS, mainSkeletonId).getData(),
								skeletonTable.getEntry(mainSkeletonId).size());

						// System.out.println(skeletonId + " has " +
						// archive.size() + " sub skeletons.");

						int subSkeletonCount = skeletonArchive.size();

						boolean headerPacked = false;

						skeletons[mainSkeletonId] = new Skeleton[subSkeletonCount];

						for (int subSkeletonId = 0; subSkeletonId < subSkeletonCount; subSkeletonId++) {

							ByteBuffer skeletonBuffer = skeletonArchive.getEntry(subSkeletonId);

							if (skeletonBuffer.remaining() == 0) {
								continue;
							}

							int skinId = (skeletonBuffer.array()[0] & 255) << 8 | skeletonBuffer.array()[1] & 255;

							Container skinContainer = cache.read(CacheIndex.SKINS, skinId);

							ByteBuffer skinBuffer = skinContainer.getData();

							if (skeletonBuffer == null || skinBuffer == null) {
								continue;
							}

							Skin skin = Skin.decode(skinBuffer);

							if (!headerPacked) {
								dat.writeShort(skin.count);

								int skinr;
								for (skinr = 0; skinr < skin.count; ++skinr) {
									dat.writeShort(skin.transformationTypes[skinr]);
								}

								for (skinr = 0; skinr < skin.count; ++skinr) {
									dat.writeShort(skin.skinList[skinr].length);
								}

								for (skinr = 0; skinr < skin.count; ++skinr) {
									for (int subSkin = 0; subSkin < skin.skinList[skinr].length; ++subSkin) {
										dat.writeShort(skin.skinList[skinr][subSkin]);
									}
								}

								dat.writeShort(subSkeletonCount);
								headerPacked = true;
							}

							dat.writeShort(subSkeletonId);

							skeletons[mainSkeletonId][subSkeletonId] = Skeleton.decode(skeletonBuffer, skin, dat);

						}

						@Cleanup
						FileOutputStream fos = new FileOutputStream(new File(dir, mainSkeletonId + ".gz"));
						fos.write(CompressionUtils.gzip(bos.toByteArray()));

						double progress = (double) (mainSkeletonId + 1) / skeletonTable.capacity() * 100;

						updateMessage(String.format("%.2f%s", progress, "%"));
						updateProgress((mainSkeletonId + 1), skeletonTable.capacity());

					}

					System.out.println(String.format("Dumped %d skeletons.", skeletonTable.capacity()));

				}
				return null;

			}

		});

	}

	@FXML
	private void dumpMidis() {

		if (!cacheDirectory.isPresent()) {
			DirectoryChooser cacheChooser = new DirectoryChooser();
			cacheChooser.setTitle("Select osrs cache.");

			Optional<File> cacheResult = Optional.ofNullable(cacheChooser.showDialog(App.getStage()));

			if (!cacheResult.isPresent()) {
				return;
			}

			cacheDirectory = cacheResult;
		}

		createTask(new Task<Void>() {

			@Override
			protected Void call() throws Exception {
				try (Cache cache = new Cache(FileStore.open(cacheDirectory.get().toPath().toString()))) {
					File dir = new File("./dump/index3/");

					if (!dir.exists()) {
						dir.mkdirs();
					}

					Tracks.initialize(cache);

					for (int i = 0; i < Tracks.getTrack1Count(); i++) {
						Track track = Tracks.getTrack1(i);

						if (track == null) {
							continue;
						}

						try (DataOutputStream dos = new DataOutputStream(
								new FileOutputStream(new File(dir, i + ".gz")))) {
							dos.write(CompressionUtils.gzip(track.getDecoded()));
						}

						double progress = (double) (i + 1) / Tracks.getTrack1Count() * 100;

						updateMessage(String.format("%.2f%s", progress, "%"));
						updateProgress((i + 1), Tracks.getTrack1Count());

						System.out.printf("dumping track1 %d out of %d %.2f%s\n", (i + 1), Tracks.getTrack1Count(),
								progress, "%");
					}

					Platform.runLater(() -> {
						Dialogue.openDirectory("Would you like to view these files?", dir);
					});

					System.out.println(String.format("Dumped %d midi files.", Tracks.getTrack1Count()));

				}
				return null;
			}

		});

	}

	@FXML
	private void dumpSprites() {

		if (!cacheDirectory.isPresent()) {
			DirectoryChooser cacheChooser = new DirectoryChooser();
			cacheChooser.setTitle("Select osrs cache.");

			Optional<File> cacheResult = Optional.ofNullable(cacheChooser.showDialog(App.getStage()));

			if (!cacheResult.isPresent()) {
				return;
			}

			cacheDirectory = cacheResult;
		}

		createTask(new Task<Void>() {

			@Override
			protected Void call() throws Exception {
				try (Cache cache = new Cache(FileStore.open(cacheDirectory.get().toPath().toString()))) {
					File dir = new File("./dump/sprites/");

					if (!dir.exists()) {
						dir.mkdirs();
					}

					ReferenceTable table = cache.getReferenceTable(8);
					for (int i = 0; i < table.capacity(); i++) {
						if (table.getEntry(i) == null)
							continue;

						Container container = cache.read(8, i);
						Sprite sprite = Sprite.decode(container.getData());

						for (int frame = 0; frame < sprite.size(); frame++) {
							File file = new File(dir, i + "_" + frame + ".png");

							BufferedImage image = ImageUtils.createColoredBackground(
									ImageUtils.makeColorTransparent(sprite.getFrame(frame), Color.WHITE),
									new java.awt.Color(0xFF00FF, false));

							ImageIO.write(image, "png", file);
						}

						double progress = (double) (i + 1) / table.capacity() * 100;

						updateMessage(String.format("%.2f%s", progress, "%"));
						updateProgress((i + 1), table.capacity());

						System.out.printf("%d out of %d %.2f%s\n", (i + 1), table.capacity(), progress, "%");

					}

					Container container = cache.read(10, cache.getFileId(10, "title.jpg"));
					byte[] bytes = new byte[container.getData().remaining()];
					container.getData().get(bytes);
					Files.write(new File(dir, "title.jpg").toPath(), bytes);

					Platform.runLater(() -> {
						Dialogue.openDirectory("Would you like to view these files?", dir);
					});

					System.out.println(String.format("Dumped %d sprites.", table.capacity()));

					return null;
				}

			}

		});

	}

	@FXML
	private void dumpMaps() {

		DirectoryChooser xteaChooser = new DirectoryChooser();
		xteaChooser.setTitle("Select directory containing XTEAs");

		Optional<File> xResult = Optional.ofNullable(xteaChooser.showDialog(App.getStage()));

		if (!xResult.isPresent()) {
			return;
		}

		if (!XTEAManager.load(xResult.get())) {
			Dialogue.showWarning("Could not read XTEAs").showAndWait();
			return;
		}

		if (!cacheDirectory.isPresent()) {
			DirectoryChooser cacheChooser = new DirectoryChooser();
			cacheChooser.setTitle("Select directory containing osrs cache");

			Optional<File> cacheResult = Optional.ofNullable(cacheChooser.showDialog(App.getStage()));

			if (!cacheResult.isPresent()) {
				return;
			}

			cacheDirectory = cacheResult;
		}

		createTask(new Task<Boolean>() {

			@Override
			protected Boolean call() throws Exception {

				File dir = new File("./dump/");

				if (!dir.exists()) {
					dir.mkdirs();
				}

				File mapDir = new File("./dump/index4/");

				if (!mapDir.exists()) {
					mapDir.mkdirs();
				}

				@Cleanup
				Cache cache = new Cache(FileStore.open(cacheDirectory.get().toPath().toString()));

				@Cleanup
				RandomAccessFile raf = new RandomAccessFile(new File(dir, "map_index").toPath().toString(), "rw");

				System.out.println("Generating map_index...");

				int total = 0;
				raf.seek(2L);

				int end;

				int mapCount = 0;
				int landCount = 0;

				for (end = 0; end < 256; end++) {
					for (int i = 0; i < 256; i++) {
						int var17 = end << 8 | i;
						int x = cache.getFileId(5, "m" + end + "_" + i);
						int y = cache.getFileId(5, "l" + end + "_" + i);
						if ((x != -1) && (y != -1)) {
							raf.writeShort(var17);
							raf.writeShort(x);
							raf.writeShort(y);
							total++;
						}
					}
				}

				end = (int) raf.getFilePointer();
				raf.seek(0L);
				raf.writeShort(total);
				raf.seek(end);
				raf.close();
				System.out.println("Done dumping map_index.");

				for (int i = 0; i < MAX_REGION; i++) {
					int[] var171 = XTEAManager.lookupMap(i);
					int x = i >> 8;
					int y = i & 0xFF;
					int map = cache.getFileId(5, "m" + x + "_" + y);
					int land = cache.getFileId(5, "l" + x + "_" + y);

					if (map != -1) {
						Container container = cache.read(5, map);
						byte[] bytes = new byte[container.getData().limit()];
						container.getData().get(bytes);

						File var18 = new File(mapDir, map + ".gz");

						@Cleanup
						FileOutputStream fos = new FileOutputStream(var18);
						fos.write(CompressionUtils.gzip(bytes));

						mapCount++;

					}

					if (land != -1) {
						try {
							Container container = cache.read(5, land, var171);
							byte[] bytes = new byte[container.getData().limit()];
							container.getData().get(bytes);
							File file = new File(mapDir, land + ".gz");

							@Cleanup
							FileOutputStream fos = new FileOutputStream(file);
							fos.write(CompressionUtils.gzip(bytes));

							System.out.println("Succesfully decrypted objectmap: " + land);

							landCount++;

						} catch (Exception localException) {
						}

					}

					double progress = (double) (i + 1) / MAX_REGION * 100;

					updateMessage(String.format("%.2f%s", progress, "%"));
					updateProgress((i + 1), 32768);

				}

				Platform.runLater(() -> {
					Dialogue.openDirectory("Would you like to view these files?", dir);
				});

				int totalCount = mapCount + landCount;

				System.out.println(String.format("Dumped %d map count %d land count %d total count", mapCount,
						landCount, totalCount));

				return true;
			}

		});

	}

	private static final int MAX_REGION = 32768;
	private static final int MAP_SCALE = 2;

	private static final boolean LABEL = true;
	private static final boolean OUTLINE = true;
	private static final boolean FILL = true;

	@FXML
	private void dumpMapImage() {
		
		File dir = new File("./dump/");

		if (!dir.exists()) {
			dir.mkdirs();
		}
		
		DirectoryChooser xteaChooser = new DirectoryChooser();
		xteaChooser.setTitle("Select directory containing XTEAs");

		Optional<File> xResult = Optional.ofNullable(xteaChooser.showDialog(App.getStage()));

		if (!xResult.isPresent()) {
			return;
		}

		if (!XTEAManager.load(xResult.get())) {
			Dialogue.showWarning("Could not read XTEAs").showAndWait();
			return;
		}
		
		if (!cacheDirectory.isPresent()) {
			DirectoryChooser cacheChooser = new DirectoryChooser();
			cacheChooser.setTitle("Select directory containing osrs cache");

			Optional<File> cacheResult = Optional.ofNullable(cacheChooser.showDialog(App.getStage()));

			if (!cacheResult.isPresent()) {
				return;
			}

			cacheDirectory = cacheResult;
		}

		createTask(new Task<Void>() {

			@Override
			protected Void call() throws Exception {

				@Cleanup
				Cache cache = new Cache(FileStore.open(cacheDirectory.get().toPath().toString()));

				TypeListManager.initialize(cache);
				Textures.initialize(cache);
				Sprites.initialize(cache);
				XTEAManager.touch();

				final List<Integer> flags = new ArrayList<>();
				final List<Region> regions = new ArrayList<>();
				final Map<Integer, Image> mapIcons = new HashMap<>();

				Region lowestX = null;
				Region lowestY = null;
				Region highestX = null;
				Region highestY = null;

				for (int i = 0; i < MAX_REGION; i++) {
					final Region region = new Region(i);

					int map = cache.getFileId(5, region.getTerrainIdentifier());
					int loc = cache.getFileId(5, region.getLocationsIdentifier());
					
					double progress = ((double)(i + 1) / MAX_REGION) * 100;
					
					if (map == -1 && loc == -1) {
						updateMessage(String.format("%.2f%s", progress, "%"));
						updateProgress((i + 1), MAX_REGION);
						continue;
					}

					if (map != -1) {
						region.loadTerrain(cache.read(5, map).getData());
					}

					if (loc != -1) {
						ByteBuffer buffer = cache.getStore().read(5, loc);
						try {
							region.loadLocations(Container.decode(buffer, XTEAManager.lookupMap(i)).getData());
						}

						catch (Exception e) {
							if (buffer.limit() != 32) {
								flags.add(i);
							}
						}
					}

					regions.add(region);

					if (lowestX == null || region.getBaseX() < lowestX.getBaseX()) {
						lowestX = region;
					}

					if (highestX == null || region.getBaseX() > highestX.getBaseX()) {
						highestX = region;
					}

					if (lowestY == null || region.getBaseY() < lowestY.getBaseY()) {
						lowestY = region;
					}

					if (highestY == null || region.getBaseY() > highestY.getBaseY()) {
						highestY = region;
					}
					
					
					
					updateMessage(String.format("%.2f%s", progress, "%"));
					updateProgress((i + 1), MAX_REGION);					

				}

				final Sprite mapscene = Sprites.getSprite("mapscene");

				for (int i = 0; i < mapscene.size(); i++) {
					mapIcons.put(i, mapscene.getFrame(i).getScaledInstance(4, 5, 0));
				}

				int minX = lowestX.getBaseX();
				int minY = lowestY.getBaseY();

				int maxX = highestX.getBaseX() + 64;
				int maxY = highestY.getBaseY() + 64;

				int dimX = maxX - minX;
				int dimY = maxY - minY;

				int boundX = dimX - 1;
				int boundY = dimY - 1;

				dimX *= MAP_SCALE;
				dimY *= MAP_SCALE;

				BufferedImage baseImage = new BufferedImage(dimX, dimY, BufferedImage.TYPE_INT_RGB);
				BufferedImage fullImage = new BufferedImage(dimX, dimY, BufferedImage.TYPE_INT_RGB);

				Graphics2D graphics = fullImage.createGraphics();

				// Draw Underlay Map - Pass 1
				for (Region region : regions) {

					int baseX = region.getBaseX();
					int baseY = region.getBaseY();
					int drawBaseX = baseX - lowestX.getBaseX();
					int drawBaseY = highestY.getBaseY() - baseY;

					for (int x = 0; x < 64; ++x) {
						int drawX = drawBaseX + x;

						for (int y = 0; y < 64; ++y) {
							int drawY = drawBaseY + (63 - y);

							int overlayId = region.getOverlayId(0, x, y) - 1;
							int underlayId = region.getUnderlayId(0, x, y) - 1;
							int rgb = 0;

							if (overlayId > -1) {
								OverlayType overlay = TypeListManager.lookupOver(overlayId);
								if (!overlay.isHideUnderlay() && underlayId > -1) {
									UnderlayType underlay = TypeListManager.lookupUnder(underlayId);
									rgb = underlay.getRgbColor();
								}

								else {
									rgb = Color.MAGENTA.getRGB();
								}
							}

							else if (underlayId > -1) {
								UnderlayType underlay = TypeListManager.lookupUnder(underlayId);
								rgb = underlay.getRgbColor();
							}

							else {
								rgb = Color.MAGENTA.getRGB();
							}

							drawMapSquare(baseImage, drawX, drawY, rgb);
						}
					}
				}
				
				double progress = ((double) 1 / 6) * 100;

				updateMessage(String.format("%.2f%s", progress, "%"));
				updateProgress(1, 6);

				// Blend Underlay Map - Pass 2
				for (Region region : regions) {

					int baseX = region.getBaseX();
					int baseY = region.getBaseY();
					int drawBaseX = baseX - lowestX.getBaseX();
					int drawBaseY = highestY.getBaseY() - baseY;

					for (int x = 0; x < 64; ++x) {
						int drawX = drawBaseX + x;

						for (int y = 0; y < 64; ++y) {
							int drawY = drawBaseY + (63 - y);

							Color c = getMapSquare(baseImage, drawX, drawY);

							if (c.equals(Color.MAGENTA))
								continue;

							int tRed = 0, tGreen = 0, tBlue = 0;
							int count = 0;

							int maxDY = Math.min(boundY, drawY + 3);
							int maxDX = Math.min(boundX, drawX + 3);
							int minDY = Math.max(0, drawY - 3);
							int minDX = Math.max(0, drawX - 3);

							for (int dy = minDY; dy < maxDY; dy++) {
								for (int dx = minDX; dx < maxDX; dx++) {
									c = getMapSquare(baseImage, dx, dy);

									if (c.equals(Color.MAGENTA))
										continue;

									tRed += c.getRed();
									tGreen += c.getGreen();
									tBlue += c.getBlue();
									count++;
								}
							}

							if (count > 0) {
								c = new Color(tRed / count, tGreen / count, tBlue / count);
								drawMapSquare(fullImage, drawX, drawY, c.getRGB());
							}
						}
					}
				}
				
				progress = ((double) 2 / 6) * 100;

				updateMessage(String.format("%.2f%s", progress, "%"));
				updateProgress(2, 6);

				// Draw Overlay Map - Pass 3
				for (Region region : regions) {

					int baseX = region.getBaseX();
					int baseY = region.getBaseY();
					int drawBaseX = baseX - lowestX.getBaseX();
					int drawBaseY = highestY.getBaseY() - baseY;

					for (int x = 0; x < 64; ++x) {
						int drawX = drawBaseX + x;

						for (int y = 0; y < 64; ++y) {
							int drawY = drawBaseY + (63 - y);

							int overlayId = region.getOverlayId(0, x, y) - 1;
							int rgb = -1;

							if (overlayId > -1) {
								OverlayType overlay = TypeListManager.lookupOver(overlayId);
								if (overlay.isHideUnderlay()) {
									rgb = overlay.getRgbColor();
								}

								if (overlay.getSecondaryRgbColor() > -1) {
									rgb = overlay.getSecondaryRgbColor();
								}

								if (overlay.getTexture() > -1) {
									rgb = Textures.getColors(overlay.getTexture());
								}

							}

							if (rgb > -1)
								drawMapSquare(fullImage, drawX, drawY, rgb);
						}
					}
				}
				
				progress = ((double) 3 / 6) * 100;

				updateMessage(String.format("%.2f%s", progress, "%"));
				updateProgress(3, 6);

				// Draw Locations Map - Pass 4
				for (Region region : regions) {

					int baseX = region.getBaseX();
					int baseY = region.getBaseY();
					int drawBaseX = baseX - lowestX.getBaseX();
					int drawBaseY = highestY.getBaseY() - baseY;

					for (Location location : region.getLocations()) {
						if (location.getPosition().getHeight() != 0) {
							// continue;
						}

						ObjectType objType = TypeListManager.lookupObject(location.getId());

						int localX = location.getPosition().getX() - region.getBaseX();
						int localY = location.getPosition().getY() - region.getBaseY();

						int drawX = drawBaseX + localX;
						int drawY = drawBaseY + (63 - localY);

						if (objType.getMapSceneID() != -1) {
							Image spriteImage = mapIcons.get(objType.getMapSceneID());
							graphics.drawImage(spriteImage, drawX * MAP_SCALE, drawY * MAP_SCALE, null);
						}
					}
				}
				
				progress = ((double) 4 / 6) * 100;

				updateMessage(String.format("%.2f%s", progress, "%"));
				updateProgress(4, 6);

				// Draw Icons Map - Pass 5
				for (Region region : regions) {

					int baseX = region.getBaseX();
					int baseY = region.getBaseY();
					int drawBaseX = baseX - lowestX.getBaseX();
					int drawBaseY = highestY.getBaseY() - baseY;

					for (Location location : region.getLocations()) {
						if (location.getPosition().getHeight() != 0) {
							// continue;
						}

						ObjectType objType = TypeListManager.lookupObject(location.getId());

						int localX = location.getPosition().getX() - region.getBaseX();
						int localY = location.getPosition().getY() - region.getBaseY();

						int drawX = drawBaseX + localX;
						int drawY = drawBaseY + (63 - localY);

						if (objType.getMapAreaId() != -1) {
							AreaType areaType = TypeListManager.lookupArea(objType.getMapAreaId());
							Image spriteImage = Sprites.getSprite(areaType.getSpriteId()).getFrame(0);
							graphics.drawImage(spriteImage, (drawX - 1) * MAP_SCALE, (drawY - 1) * MAP_SCALE, null);
						}
					}
				}
				
				progress = ((double) 5 / 6) * 100;

				updateMessage(String.format("%.2f%s", progress, "%"));
				updateProgress(5, 6);

				// Label/Outline/Fill regions - Pass 6
				for (Region region : regions) {

					int baseX = region.getBaseX();
					int baseY = region.getBaseY();
					int drawBaseX = baseX - lowestX.getBaseX();
					int drawBaseY = highestY.getBaseY() - baseY;

					if (LABEL) {
						graphics.setColor(Color.RED);
						graphics.drawString(String.valueOf(region.getRegionID()), drawBaseX * MAP_SCALE,
								drawBaseY * MAP_SCALE + graphics.getFontMetrics().getHeight());
					}

					if (OUTLINE) {
						graphics.setColor(Color.RED);
						graphics.drawRect(drawBaseX * MAP_SCALE, drawBaseY * MAP_SCALE, 64 * MAP_SCALE, 64 * MAP_SCALE);
					}

					if (FILL) {
						if (flags.contains(region.getRegionID())) {
							graphics.setColor(new Color(255, 0, 0, 80));
							graphics.fillRect(drawBaseX * MAP_SCALE, drawBaseY * MAP_SCALE, 64 * MAP_SCALE,
									64 * MAP_SCALE);
						}
					}

				}
				
				progress = ((double) 6 / 6) * 100;

				updateMessage(String.format("%.2f%s", progress, "%"));
				updateProgress(6 + 1, 6);

				graphics.dispose();

				ImageIO.write(baseImage, "png", new File(dir, "base_image.png"));
				ImageIO.write(fullImage, "png", new File(dir, "full_image.png"));

			Platform.runLater(() -> {
				Dialogue.openDirectory("Would you like to view these files?", dir);
			});

				return null;
			}

		});

	}

	private static void drawMapSquare(BufferedImage image, int x, int y, int rgb) {
		x *= MAP_SCALE;
		y *= MAP_SCALE;

		for (int dx = 0; dx < MAP_SCALE; ++dx) {
			for (int dy = 0; dy < MAP_SCALE; ++dy) {
				image.setRGB(x + dx, y + dy, rgb);
			}
		}
	}

	private static Color getMapSquare(BufferedImage image, int x, int y) {
		x *= MAP_SCALE;
		y *= MAP_SCALE;

		return new Color(image.getRGB(x, y));
	}

	@FXML
	private void credits(ActionEvent e) {
		MenuItem mi = (MenuItem) e.getSource();

		Misc.launchURL("https://www.rune-server.ee/members/" + mi.getText() + "/");
	}

	private void createTask(Task<?> task) {

		progressBar.setVisible(true);

		progressBar.progressProperty().unbind();
		progressBar.progressProperty().bind(task.progressProperty());

		progressText.textProperty().unbind();
		progressText.textProperty().bind(task.messageProperty());

		new Thread(task).start();

		task.setOnSucceeded(e -> {

			PauseTransition pause = new PauseTransition(Duration.seconds(1));

			pause.setOnFinished(event -> {
				progressBar.setVisible(false);
				progressText.textProperty().unbind();
				progressText.setText("");
			});

			pause.play();
		});

		task.setOnFailed(e -> {

			PauseTransition pause = new PauseTransition(Duration.seconds(1));

			pause.setOnFinished(event -> {
				progressBar.setVisible(false);
				progressText.textProperty().unbind();
				progressText.setText("");
			});

			pause.play();

		});
	}

	@FXML
	private void handleMouseDragged(MouseEvent event) {

		Stage stage = App.getStage();

		stage.setX(event.getScreenX() - xOffset);
		stage.setY(event.getScreenY() - yOffset);

	}

	@FXML
	private void handleMousePressed(MouseEvent event) {
		xOffset = event.getSceneX();
		yOffset = event.getSceneY();
	}

	@FXML
	private void minimizeProgram() {

		if (App.getStage() == null) {
			return;
		}

		App.getStage().setIconified(true);
	}

	@FXML
	private void closeProgram() {
		Platform.exit();
	}

}
