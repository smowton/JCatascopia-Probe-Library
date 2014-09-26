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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.logging.Level;

import eu.celarcloud.jcatascopia.probepack.Probe;
import eu.celarcloud.jcatascopia.probepack.ProbeMetric;
import eu.celarcloud.jcatascopia.probepack.ProbePropertyType;

/**
 * 
 * @author Christopher Smowton
 *
 */
public class WindowsNetworkProbe extends Probe{
	
    	private static String netCommand = PowershellHelper.makePowershellGroupedStatsCommand(
		new String[] {
		    "\\Network Interface(*)\\Bytes Received/sec",
		    "\\Network Interface(*)\\Packets Received/sec",
		    "\\Network Interface(*)\\Bytes Sent/sec",
		    "\\Network Interface(*)\\Packets Sent/sec"
 		});

    public WindowsNetworkProbe(String name, int freq){
		super(name,freq);
		this.addProbeProperty(0,"netBytesIN",ProbePropertyType.DOUBLE,"bytes/s","Bytes IN per Second");
		this.addProbeProperty(1,"netPacketsIN",ProbePropertyType.DOUBLE,"packets/s","Packets IN per Second");
		this.addProbeProperty(2,"netBytesOUT",ProbePropertyType.DOUBLE,"bytes/s","Bytes OUT per Second");
		this.addProbeProperty(3,"netPacketsOut",ProbePropertyType.DOUBLE,"packets/s","Packets OUT per Second");
	}
	
	public WindowsNetworkProbe(){
		this("WindowsNetworkProbe",10);
	}
	
	@Override
	public String getDescription() {
		return "WindowsNetworkProbe collects Network usage stats.";
	}

	public ProbeMetric collectOrThrow() throws IOException {

		ProbePropertyType[] types = new ProbePropertyType[] { ProbePropertyType.DOUBLE, ProbePropertyType.DOUBLE, ProbePropertyType.DOUBLE, ProbePropertyType.DOUBLE };
		HashMap<Integer, Object> values = PowershellHelper.powershellToStats(netCommand, types);
		return new ProbeMetric(values);

	}
	
	public ProbeMetric collect() {

		try {
			return collectOrThrow();
		}
		catch(Exception e) {
			System.err.println("Windows network probe failed: " + e.toString());
			return new ProbeMetric(new HashMap<Integer, Object>());
		}

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		WindowsNetworkProbe netprobe = new WindowsNetworkProbe();
		netprobe.activate();
	}
}
