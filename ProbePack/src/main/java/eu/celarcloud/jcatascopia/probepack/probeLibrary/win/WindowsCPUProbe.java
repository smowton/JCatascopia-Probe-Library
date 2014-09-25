/*******************************************************************************
 * Copyright 2014, Laboratory of Internet Computing (LInC), Department of Computer Science, University of Cyprus
 * 
 * For any information relevant to JCatascopia Monitoring System,
 * please contact Demetris Trihinas, trihinas{at}cs.ucy.ac.cy
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package eu.celarcloud.jcatascopia.probepack.probeLibrary;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.logging.Level;

import eu.celarcloud.jcatascopia.probepack.Probe;
import eu.celarcloud.jcatascopia.probepack.ProbeMetric;
import eu.celarcloud.jcatascopia.probepack.ProbePropertyType;

/**
 * 
 * @author Chris Smowton
 *
 */
public class WindowsCPUProbe extends Probe{
	
	//private static String cpuTotalCommand = "(Get-Counter -Counter @(\\\"\\Processor(_Total)\\% Processor Time\\\", \\\"\\Processor(_Total)\\% User Time\\\", \\\"\\Processor(_Total)\\% Privileged Time\\\", \\\"\\Processor(_Total)\\% Idle Time\\\")).CounterSamples | Select-Object -ExpandProperty CookedValue | ForEach-Object -process {$_.ToString((New-Object Globalization.CultureInfo \\\"\\\"))}";

	private static String cpuTotalCommand = PowershellHelper.makePowershellStatsCommand(
	  new String[]{
			  "\\Processor(_Total)\\% Processor Time",
			  "\\Processor(_Total)\\% User Time",
			  "\\Processor(_Total)\\% Privileged Time",
			  "\\Processor(_Total)\\% Idle Time"
	  });
	
	public WindowsCPUProbe(String name, int freq){
		super(name,freq);
		this.addProbeProperty(0,"cpuTotal",ProbePropertyType.DOUBLE,"%","Total system CPU usage");
		this.addProbeProperty(1,"cpuUser",ProbePropertyType.DOUBLE,"%","system USER usage");
		this.addProbeProperty(2,"cpuSystem",ProbePropertyType.DOUBLE,"%","system SYSTEM usage");
		this.addProbeProperty(3,"cpuIdle",ProbePropertyType.DOUBLE,"%","system IDLE Usage");
	}
	
	public WindowsCPUProbe(){
		this("Windows CPUProbe",10);
	}
		
	@Override
	public String getDescription() {
		return "Windows CPUProbe collects CPU usage stats.";
	}

	@Override
	public ProbeMetric collect() {

		try {
			return collectOrThrow();
		}
		catch(Exception e) {
			System.err.println("Windows CPU probe failed: " + e.toString());
			return new ProbeMetric(new HashMap<Integer, Object>());
		}

	}

	public ProbeMetric collectOrThrow() throws IOException {

		Process collector = new ProcessBuilder("Powershell.exe", "-Command", String.format("& {%s}", cpuTotalCommand)).start();
		
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

			double val = Double.parseDouble(line);
			values.put(i, val);
			++i;

		}

		br.close();

		InputStream eis = collector.getErrorStream();
		InputStreamReader eisr = new InputStreamReader(eis);
		int c;

		while((c = eisr.read()) >= 0)
			System.err.write(c);

		eisr.close();
		
		return new ProbeMetric(values);

	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		WindowsCPUProbe cpuprobe = new WindowsCPUProbe();
		cpuprobe.activate();
	}
}
