package cn.nhcqc.sshex;

import java.io.*;
import java.util.*;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.transport.TransportException;
import net.schmizz.sshj.transport.verification.FingerprintVerifier;

/**
 * SCP with password
 *
 * @author CHEN Qingcan
 */
public class Scp {

	private static class Location {
		String     host;
		String     username;
		String     path;

		@Override
		public String toString () {
			return host != null ?
				String.format ("%s@%s:%s", username, host, path) :
				path;
		}
	}
	private static class Config {
		Location   from;
		Location   to;
		Integer    port             = PORT_DEFAULT;
		String     password;
		Integer    timeoutConnect   = 0;

		@Override
		public String toString () {
			return String.format (
				"--password \"%s\" --connect-timeout %d %s -> %s",
				password, timeoutConnect, from, to
			);
		}
	}

	final static int PORT_MIN = 1, PORT_MAX = 0xFFFF, PORT_DEFAULT = 22, MS_SEC = 1000;
	final static PrintStream STDOUT = System.out, STDERR = System.err;
	final static private Logger logger = LoggerFactory.getLogger (Scp.class);

	//------------------------------------------------------------------------
	public static void main (final String[] args) {
		try {
			var  scp       = new Scp   ();
			var  config    = scp.parseArgs (args);
			scp.run        (config);
			STDOUT.printf  ("%s -> %s%n", config.from, config.to);
			System.exit    (0);

		} catch (Exception e) {
			STDERR.println (e.getMessage ());
//			logger.error   ("", e);
			System.exit    (1);
		}
	}

	public void run (final Config config)
	throws IOException {
		assert config != null;
		var username = config.from.username != null ? config.from.username : config.to.username;
		logger.trace ("config: {}", config);

		try (SSHClient ssh = connect (config)) {
			logger.trace ("auth: {}", username);
			ssh.authPassword (username, config.password);
			if (config.from.host != null) {
				ssh.newSCPFileTransfer ().download (config.from.path, config.to.path);
			} else {
				ssh.newSCPFileTransfer ().upload   (config.from.path, config.to.path);
			}
		}
	}

	//------------------------------------------------------------------------
	private SSHClient connect (final Config config)
	throws IOException {
		assert config != null;
		var host = config.from.host != null ? config.from.host : config.to.host;
		var ssh = new SSHClient ();
		String fingerprint = null;

		// TODO find a better way to get fingerprint or bypass the host key verification.
		try {
			logger.trace ("connect: {}:{} timeout: {} sec.", host, config.port, config.timeoutConnect);
			setSSHtimeout (ssh, config);
			ssh.loadKnownHosts ();
			ssh.connect (host, config.port);
			return ssh;
		} catch (TransportException e) {
			// Could not verify `ssh-ed25519` host key with fingerprint `xx:xx:xx:xx:xx:xx:xx:xx:xx:xx:xx:xx:xx:xx:xx:xx` for `{host}` on port {port}
			logger.trace (e.getMessage ());
			fingerprint = StringUtils.substringBetween (e.getMessage (), "fingerprint `", "`");
			logger.trace ("fingerprint: {}", fingerprint);
			ssh.disconnect ();
		}

		ssh = new SSHClient ();
		ssh.addHostKeyVerifier (FingerprintVerifier.getInstance (fingerprint));
		logger.trace ("connect: {}:{} timeout: {} sec.", host, config.port, config.timeoutConnect);
		setSSHtimeout (ssh, config);
		ssh.connect (host, config.port);
		return ssh;
	}

	//------------------------------------------------------------------------
	private SSHClient setSSHtimeout (final SSHClient ssh, final Config config) {
		assert ssh != null && config != null;
		if (config.timeoutConnect > 0) {
			ssh.setConnectTimeout (config.timeoutConnect * MS_SEC);
			ssh.setTimeout (config.timeoutConnect * MS_SEC);
		}
		return ssh;
	}

	//------------------------------------------------------------------------
	private Config parseArgs (final String[] args) {
		var options = new Options ();
		var optionm = buildOptions (options);
		try {
			CommandLine cmd = new DefaultParser ().parse (options, args);
			Config   parsed = new Config ();
			parseOptions  (cmd, optionm, parsed);
			parseLeftOver (cmd, parsed);
			parseCheck    (parsed);
			return parsed;

		} catch (ParseException e) {
			parseError (options, e);
			return null; // never here
		}
	}

	//------------------------------------------------------------------------
	/**
	 * @param options [out]
	 */
	private Map<String, Option> buildOptions (final Options options) {
		var map = new HashMap<String,Option>();
		map.put ("connect-timeout",
			Option.builder ()
			.longOpt ("connect-timeout")
			.hasArg ()
			.desc ("equals to -o ConnectTimeout=X")
			.build ());
		map.put ("option",
			Option.builder ("o")
			.longOpt ("option")
			.hasArg ()
			.desc ("ConnectTimeout=X (where X is in seconds)")
			.build ());
		map.put ("port",
			Option.builder ("P")
			.longOpt ("port")
			.hasArg ()
			.desc ("port")
			.build ());
		map.put ("password",
			Option.builder ()
			.longOpt ("password")
			.hasArg ()
			.desc ("password")
			.build ());
		map.put ("help",
			Option.builder ("h")
			.longOpt ("help")
			.desc ("usage")
			.build ());
		assert options != null;
		map.values ().stream ().forEach (a -> options.addOption (a));
		return map;
	}

	//------------------------------------------------------------------------
	private void parseOptions (final CommandLine cmd, final Map<String, Option> optionm, final Config parsed)
	throws ParseException {
		if (cmd.hasOption (optionm.get ("help")))
			throw new ParseException ("");
		String value;

		value = cmd.getOptionValue (optionm.get ("port"));
		if (value != null) parsed.port = NumberUtils.toInt (value);

		value = cmd.getOptionValue (optionm.get ("password"));
		if (value != null) parsed.password = value;

		value = cmd.getOptionValue (optionm.get ("option"));
		if (value != null) {
			value = StringUtils.substringAfter (value, "ConnectTimeout=");
			parsed.timeoutConnect = NumberUtils.toInt (value);
		}

		value = cmd.getOptionValue (optionm.get ("connect-timeout"));
		if (value != null) parsed.timeoutConnect = NumberUtils.toInt (value);
	}

	//------------------------------------------------------------------------
	private void parseLeftOver (final CommandLine cmd, final Config parsed)
	throws ParseException {
		assert cmd != null && parsed != null;
		if (cmd.getArgs ().length != 2) return;
		parsed.from = arg2location (cmd.getArgs ()[0]);
		parsed.to   = arg2location (cmd.getArgs ()[1]);
	}

	//------------------------------------------------------------------------
	private Location arg2location (String arg) {
		assert arg != null;
		var loc = new Location ();

		int i = arg.indexOf ('@');
		if (i >= 0) {
			loc.username = arg.substring (0, i);
			arg = arg.substring (i + 1);
		}

		i = arg.indexOf (':');
		if (i >= 0 && arg.substring (0, i).matches ("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
			loc.path = arg.substring (i + 1);
			loc.host = arg.substring (0, i);
		} else {
			loc.path = arg;
		}

		return loc;
	}

	//------------------------------------------------------------------------
	private void parseCheck (final Config parsed) throws ParseException {
		if (parsed.from == null || parsed.to == null)
			throw new ParseException ("file required");
		if (parsed.from.host == null && parsed.to.host == null)
			throw new ParseException ("host required");
		if (parsed.from.host != null && parsed.to.host != null)
			throw new ParseException ("local path required");
		if (! Range.between (PORT_MIN, PORT_MAX).contains (parsed.port))
			throw new ParseException ("port required");
		if (parsed.from.username == null && parsed.to.username == null)
			throw new ParseException ("user required");
		if (parsed.password == null)
			throw new ParseException ("password required");
		if (parsed.from.path == null && parsed.to.path == null)
			throw new ParseException ("path required");
	}

	//------------------------------------------------------------------------
	private void parseError (final Options options, final ParseException e) {
		if (! e.getMessage ().isEmpty ()) STDERR.println (e.getMessage ());
		STDOUT.println ();
		usage (options);
		System.exit (1);
	}

	//------------------------------------------------------------------------
	private void usage (final Options options) {
		String cmd = "scp-password [[user@]host:]file_from [[user@]host:]file_to",
			header = "\nSCP with password\n\n",
			footer = "";
		boolean usage = true;
		new HelpFormatter().printHelp (cmd, header, options, footer, usage);
	}

}
