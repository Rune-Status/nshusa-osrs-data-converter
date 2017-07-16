package io.freyr.app.osdc;

import java.nio.ByteBuffer;

public class Skin {
	public int id;
	public int count;
	public int[] transformationTypes;
	public int[][] skinList;
	
	public Skin() {
		
	}

	Skin(ByteBuffer buffer) {

		this.count = buffer.get() & 0xFF;
		this.transformationTypes = new int[this.count];
		this.skinList = new int[this.count][];

		int i3;
		for (i3 = 0; i3 < this.count; ++i3) {
			this.transformationTypes[i3] = buffer.get() & 0xFF;
		}

		for (i3 = 0; i3 < this.count; ++i3) {
			this.skinList[i3] = new int[buffer.get() & 0xFF];
		}

		for (i3 = 0; i3 < this.count; ++i3) {
			for (int i5 = 0; i5 < this.skinList[i3].length; ++i5) {
				this.skinList[i3][i5] = buffer.get() & 0xFF;
			}
		}

	}
	
	public static Skin decode(ByteBuffer buffer) {
		Skin skin = new Skin();
		
		skin.count = buffer.get() & 0xFF;
		skin.transformationTypes = new int[skin.count];
		skin.skinList = new int[skin.count][];

		int i3;
		for (i3 = 0; i3 < skin.count; ++i3) {
			skin.transformationTypes[i3] = buffer.get() & 0xFF;
		}

		for (i3 = 0; i3 < skin.count; ++i3) {
			skin.skinList[i3] = new int[buffer.get() & 0xFF];
		}

		for (i3 = 0; i3 < skin.count; ++i3) {
			for (int i5 = 0; i5 < skin.skinList[i3].length; ++i5) {
				skin.skinList[i3][i5] = buffer.get() & 0xFF;
			}
		}
		
		return skin;
	}

}
