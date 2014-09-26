package eu.celarcloud.jcatascopia.probepack.probeLibrary;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;

import eu.celarcloud.jcatascopia.probepack.ProbePropertyType;

public class PowershellHelper {

	public static StringBuilder getPowershellSamples(String[] stats) {

		StringBuilder sb = new StringBuilder();
		sb.append("(Get-Counter -Counter @(");
		
		for(int i = 0, ilim = stats.length; i != ilim; ++i) {
			
			if(i != 0)
				sb.append(", ");
			sb.append(String.format("\"%s\"", stats[i]));
			
		}
		
		sb.append(")).CounterSamples");
		return sb;

	}

	public static String escapeSB(StringBuilder sb) {

		return sb.toString(); //.replace("\"", "\\\"");

	}
    
    
	public static String makePowershellGroupedStatsCommand(String[] stats) {

		StringBuilder sb = getPowershellSamples(stats);

		// Paint each sample with the counter variety:
		sb.append(" | Add-Member CounterSuffix \"\" -PassThru | % { $_.CounterSuffix = $_.Path.split(\"\\\")[-1]; $_ }");

		// Group by counter variety, sum within groups
		sb.append(" | group -Property CounterSuffix | % { $_.Group | select -ExpandProperty CookedValue | measure-object -Sum | select -expandproperty sum }");

		return escapeSB(sb);

	}

	public static String makePowershellStatsCommand(String[] stats) {

		StringBuilder sb = getPowershellSamples(stats);
		
		// Get the CookedValue for each sample:
		sb.append(" | Select-Object -ExpandProperty CookedValue |");
		
		// Format each value as a US-english double (i.e. use . for the decimal)
		sb.append("ForEach-Object -process {$_.ToString((New-Object Globalization.CultureInfo \"\"))}");
		
		return escapeSB(sb);
		
	}
	
	public static HashMap<Integer, Object> powershellToStats(String command, ProbePropertyType[] types) throws IOException {

		command = String.format("& {%s}", command);
		
		Process collector = new ProcessBuilder("Powershell.exe", "-Command", "-").start();
		//Process collector = new ProcessBuilder("c:\\cygwin64\\bin\\Python2.7.exe", "c:\\temp\\argecho.py", String.format("& {%s}", command)).start();

		OutputStream os = collector.getOutputStream();
		OutputStreamWriter osw = new OutputStreamWriter(os);
		osw.write(command, 0, command.length());
		osw.close();
		
		InputStream is = collector.getInputStream();
		InputStreamReader isr = new InputStreamReader(is);
		BufferedReader br = new BufferedReader(isr);

		HashMap<Integer,Object> values = new HashMap<Integer,Object>();
		
		String line;
		int i = 0;
		while((line = br.readLine()) != null) {

			line = line.trim();
			if(line.equals(""))
				continue;

			Object val;
			if(types[i] == ProbePropertyType.DOUBLE) 
				val = Double.parseDouble(line);
			else if(types[i] == ProbePropertyType.INTEGER || types[i] == ProbePropertyType.LONG)
				val = Long.parseLong(line);
			else
				throw new IOException("Type not implemented " + types[i]);
			
			values.put(i++, val);

		}

		br.close();

		InputStream eis = collector.getErrorStream();
		InputStreamReader eisr = new InputStreamReader(eis);
		int c;

		while((c = eisr.read()) >= 0)
			System.err.write(c);

		eisr.close();

		return values;

	}

}
