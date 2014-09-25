package eu.celarcloud.jcatascopia.probepack.probeLibrary;

public class PowershellHelper {

       public static String makePowershellStatsCommand(String[] stats) {

	       StringBuilder sb = new StringBuilder();
	       sb.append("(Get-Counter -Counter @(");

	       for(int i = 0, ilim = stats.length; i != ilim; ++i) {

		       if(i != 0)
			       sb.append(", ");
		       sb.append(String.format("\"%s\"", stats[i]));

	       }

	       sb.append(")).CounterSamples");

	       // Get the CookedValue for each sample:
	       sb.append("| Select-Object -ExpandProperty CookedValue |");

	       // Format each value as a US-english double (i.e. use . for the decimal)
	       sb.append("ForEach-Object -process {$_.ToString((New-Object Globalization.CultureInfo \"\"))}");

	       String result = sb.toString();

	       // double-escape quotes, as Java's process builder strips them again
	       result = result.replace("\"", "\\\"");
	       return result;

       }      

}
