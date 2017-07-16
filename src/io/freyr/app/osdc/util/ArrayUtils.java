package io.freyr.app.osdc.util;

public class ArrayUtils {
	
	private ArrayUtils() {
		
	}
	
	public static boolean isEmpty(String[] array) {
		if (array == null) {
			return true;
		}
		
		for(int i = 0; i < array.length; i++) {
			if (array[i] != null) {
				return false;
			}
		}
		return true;
	}

}
