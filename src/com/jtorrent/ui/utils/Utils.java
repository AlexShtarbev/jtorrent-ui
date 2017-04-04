package com.jtorrent.ui.utils;

public class Utils {
	 
	public static String formatFileName(String fileName) {
    	// FIrst change the path to be POSIX friendly.
    	int idx = fileName.replaceAll("\\\\", "/").lastIndexOf("/");
    	// Extract the name, including the file extension.
    	String nameAndExt = idx >= 0 ? fileName.substring(idx + 1) : fileName;
    	// Find where the name ends.
    	idx = nameAndExt.lastIndexOf('.');
    	// Extract the name of the file.
    	String name = nameAndExt.substring(0, idx);
    	return name;
    }

	public static String formatSize(long bytes, boolean si) {
	    int unit = si ? 1000 : 1024;
	    if (bytes < unit) return bytes + " B";
	    int exp = (int) (Math.log(bytes) / Math.log(unit));
	    String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
	    return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
	}
}
