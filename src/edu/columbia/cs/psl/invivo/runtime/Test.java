package edu.columbia.cs.psl.invivo.runtime;

public class Test {

	static int b;
	
	static boolean[] a = {true};
	
	static int a_fill = 0;
	
	static String g() {
		return "";
	}
	
	public static void main(int x, int y) throws InterruptedException {
		
		boolean b;
		g();
		b = a[a_fill++];
	}
}
