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
import java.io.InputStreamReader;
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
public class WindowsDiskProbe extends Probe{
	
    	private static String diskCommand = PowershellHelper.makePowershellStatsCommand(
		new String[] {
		    "\\LogicalDisk(C:)\\Free Megabytes",
		    "\\LogicalDisk(C:)\\% Free Space"
 		});
	
	public WindowsDiskProbe(String name, int freq){
		super(name,freq);
		this.addProbeProperty(0,"diskFree",ProbePropertyType.LONG,"MB","available disk space in MB");
		this.addProbeProperty(1,"diskFreePercent",ProbePropertyType.DOUBLE,"%","disk space free (%)");
	}
	
	public WindowsDiskProbe(){
		this("WindowsDiskProbe",60);
	}
	
	@Override
	public String getDescription() {
		return "WindowsDiskProbe collects Disk usage stats.";
	}

	public ProbeMetric collectOrThrow() throws IOException {

		ProbePropertyType[] types = new ProbePropertyType[] { ProbePropertyType.LONG, ProbePropertyType.DOUBLE };
		HashMap<Integer, Object> values = PowershellHelper.powershellToStats(diskCommand, types);
		return new ProbeMetric(values);

	}
	
	public ProbeMetric collect() {

		try {
			return collectOrThrow();
		}
		catch(Exception e) {
			System.err.println("Windows disk probe failed: " + e.toString());
			return new ProbeMetric(new HashMap<Integer, Object>());
		}

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {		
		WindowsDiskProbe diskprobe = new WindowsDiskProbe();
		diskprobe.activate();
	}
}
