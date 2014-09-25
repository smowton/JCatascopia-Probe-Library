
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.logging.Level;

import eu.celarcloud.jcatascopia.probepack.Probe;
import eu.celarcloud.jcatascopia.probepack.ProbeMetric;
import eu.celarcloud.jcatascopia.probepack.ProbePropertyType;


public class CassandraProbe extends Probe{
	
	private static int DEFAULT_SAMPLING_PERIOD = 45;
	private static String DEFAULT_PROBE_NAME = "CassandraProbe";
	
	private static final String CONFIG_PATH = "cassandra.properties";
	private Properties config;
	private HashMap<String,Integer> keyspaces;
	private String nodetool_path;

	public CassandraProbe(String name, int freq) {
		super(name, freq);
		
		parseConfig();
		keyspaces = new HashMap<String,Integer>();
		String[] ks = this.config.getProperty("keyspaces", "").split(";");
		int i = 0, j = 0, c = 0;
		while(i < ks.length){
			this.addProbeProperty(j++,ks[i]+"_readLatency",ProbePropertyType.DOUBLE,"ms",ks[i]+" keyspace read latency");
			this.addProbeProperty(j++,ks[i]+"_writeLatency",ProbePropertyType.DOUBLE,"ms",ks[i]+" keyspace write latency");
			this.addProbeProperty(j++,ks[i]+"_load",ProbePropertyType.LONG,"Bytes",ks[i]+" keyspace total space used in bytes");
			keyspaces.put(ks[i], c);
			i++; c += 3;
		}
		nodetool_path = config.getProperty("nodetool_path", "nodetool");
	}
	
	public CassandraProbe(){
		this(DEFAULT_PROBE_NAME, DEFAULT_SAMPLING_PERIOD);
	}

	@Override
	public ProbeMetric collect() {
		HashMap<Integer,Object> values = new HashMap<Integer,Object>();
				
		HashMap<String,Double> temp = this.calcValues();
		double r,w;
		long b;
		for(Entry<String, Integer> entry:keyspaces.entrySet()){
			r = (temp.get(entry.getKey()+"_readLatency") != null) ? temp.get(entry.getKey()+"_readLatency") : Double.NaN;
			w = (temp.get(entry.getKey()+"_writeLatency") != null) ? temp.get(entry.getKey()+"_writeLatency") : Double.NaN;
			b = (temp.get(entry.getKey()+"_load") != null) ? temp.get(entry.getKey()+"_load").longValue() : -1;
			values.put(entry.getValue(), r);
			values.put(entry.getValue()+1, w);
			values.put(entry.getValue()+2, b);
		}

//		for(Entry<Integer,Object> entry:values.entrySet()){
//			System.out.println(entry.getKey()+" "+entry.getValue());
//		}
		
		return new ProbeMetric(values);
	}
	
	private HashMap<String,Double> calcValues(){
		HashMap<String,Double> stats = new HashMap<String,Double>();
		try{	
			String[] cmd = {"/bin/sh", "-c", nodetool_path + " cfstats"};
			Process p = Runtime.getRuntime().exec(cmd);
			p.waitFor();
			BufferedReader b = new BufferedReader(new InputStreamReader(p.getInputStream()));
			//BufferedReader b = new BufferedReader(new FileReader(new File("cassandra_example")));
			String line = "";
			while ((line = b.readLine()) != null){
				if (line.contains("Keyspace: ")){
					String s = line.split(" ")[1];
					if (keyspaces.containsKey(s)){
						b.readLine();
						line = b.readLine();
						stats.put(s+"_readLatency", Double.parseDouble(line.split("\\s+")[3]));
						b.readLine();
						line = b.readLine();
						stats.put(s+"_writeLatency", Double.parseDouble(line.split("\\s+")[3]));
						
						double size = 0;
						while (!(line = b.readLine()).contains("----------------") && line != null){
							if (line.contains("Space used (total), bytes:"))
								size += Double.parseDouble(line.split(":")[1].trim());
						}
						stats.put(s+"_load", size);	
					}
				}
			}
	        b.close();	
		}
		catch(Exception e){
			this.writeToProbeLog(Level.SEVERE, "Couldn't get Cassandra node stats"); 
		}
		
		return stats;
	}

	@Override
	public String getDescription() {
		return "Probe that collects read/write latency from Cassandra database";
	}
	
	//parse the configuration file
	private void parseConfig(){
		this.config = new Properties();
		//load config properties file
		try {				
			InputStream fis = getClass().getResourceAsStream(CONFIG_PATH);
			config.load(fis);
			if (fis != null)
	    		fis.close();
		} 
		catch (FileNotFoundException e){
			this.writeToProbeLog(Level.SEVERE,"config file not found");
		} 
		catch (IOException e){
			this.writeToProbeLog(Level.SEVERE,"config file parsing error");
		}
	}
	
	public static void main(String[] args) {
		CassandraProbe p = new CassandraProbe();
		p.activate();
	}
}
