package Test;

import common.java.httpServer.booter;
import common.java.nlogger.nlogger;

public class TestInfo {
	public static void main(String[] args) {
		booter booter = new booter();
		try {
			System.out.println("GrapeInfoCollection");
			System.setProperty("AppName", "GrapeInfoCollection");
			booter.start(1006);
		} catch (Exception e) {
			nlogger.logout(e);
		}
	}
}
