package cn.nhcqc.sshextest;

import java.io.*;
import org.junit.jupiter.api.*;

import cn.nhcqc.sshex.Scp;

public class ScpTest {

	//------------------------------------------------------------------------
	private static PrintStream      OUT         = System.out;

	//------------------------------------------------------------------------
	@Test
	public void testRun () {
		var config      = new Scp.Config ();
		var from        = new Scp.Location ();
		var to          = new Scp.Location ();
		from  .path     = "c:\\hello.txt";
		to    .host     = "192.168.0.1";
		to    .username = "alibaba";
		config.from     = from;
		config.to       = to;
		config.port     = 22;
		config.password = "OpenSesame";
		config.timeoutConnect = 3;

		try                   { new Scp ().run (config); }
		catch (IOException e) { OUT.println (e.getMessage ()); }
	}

}
