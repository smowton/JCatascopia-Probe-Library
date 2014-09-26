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
import java.util.logging.Level;

import eu.celarcloud.jcatascopia.probepack.Probe;
import eu.celarcloud.jcatascopia.probepack.ProbeMetric;
import eu.celarcloud.jcatascopia.probepack.ProbePropertyType;

/**
 * 
 * @author Chris Smowton
 *
 */
public class WindowsMemoryProbe extends Probe{

	private static String memoryCommand = PowershellHelper.makePowershellStatsCommand(
		new String[] {
			"\\Memory\\CommittedBytes",
			"\\Memory\\AvailableBytes",
			"\\Paging File(_total)\\% Usage"
		});
    
	
	public WindowsMemoryProbe(String name, int freq){
		super(name,freq);
		this.addProbeProperty(0, "memUsed",ProbePropertyType.INTEGER,"B","Memory Used");
		this.addProbeProperty(1, "memFree",ProbePropertyType.INTEGER,"B","Memory Free");
		this.addProbeProperty(2, "swapUsedPercent", ProbePropertyType.DOUBLE, "%", "Swapfile used percentage");
	}
	
	public WindowsMemoryProbe(){
		this("WindowsMemoryProbe",20);
	}
		
	public String getDescription(){
		return "Memory Probe collects memory stats";
	}

	public ProbeMetric collectOrThrow() throws IOException {

		ProbePropertyType[] types = new ProbePropertyType[] { ProbePropertyType.INTEGER, ProbePropertyType.INTEGER, ProbePropertyType.DOUBLE };
		HashMap<Integer, Object> values = PowershellHelper.powershellToStats(memoryCommand, types);
		return new ProbeMetric(values);

	}
	
	public ProbeMetric collect() {

		try {
			return collectOrThrow();
		}
		catch(Exception e) {
			System.err.println("Windows memory probe failed: " + e.toString());
			return new ProbeMetric(new HashMap<Integer, Object>());
		}

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		WindowsMemoryProbe memprobe = new WindowsMemoryProbe();
		memprobe.activate();
	}
}
