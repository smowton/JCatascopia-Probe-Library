import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Properties;
import java.util.logging.Level;

import eu.celarcloud.jcatascopia.probepack.Probe;
import eu.celarcloud.jcatascopia.probepack.ProbeMetric;
import eu.celarcloud.jcatascopia.probepack.ProbePropertyType;

/**
 * 
 * @author Demetris Trihinas
 *
 */
public class HAProxyProbe extends Probe{
	
	private static final String CONFIG_PATH = "haproxy.properties";
	private static String auth_header;
	private static String haproxy_url;
	
	private String proxy_name;
	private long lastSessionCount;
	private long lasttime;
	
	private Properties config;
	
	@SuppressWarnings("restriction")
	public HAProxyProbe(String name, int freq){
		super(name,freq);
		this.addProbeProperty(0,"currentSessions",ProbePropertyType.INTEGER,"","");
		this.addProbeProperty(1,"requestThroughput",ProbePropertyType.DOUBLE,"req/s","");
		this.addProbeProperty(2,"servers",ProbePropertyType.INTEGER,"","");
	    
		parseConfig();
	    String user = config.getProperty("haproxy_username", "user");
	    String pass = config.getProperty("haproxy_password", "password");
	    String ip = config.getProperty("haproxy_ip", "localhost");
	    String port = config.getProperty("haproxy_port", "30000");
	    proxy_name = config.getProperty("haproxy_proxy_name", "myproxy")+",";

	    String creds = user+":"+pass;
		auth_header = "Basic " + new sun.misc.BASE64Encoder().encode(creds.getBytes());
		haproxy_url = "http://"+ip+":"+port+"/haproxy?stats;csv";
		
		lastSessionCount = 0;
		lasttime = System.currentTimeMillis()/1000;		
	}
	
	public HAProxyProbe(){
		this("HAProxyProbe",20);
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
		
	@Override
	public String getDescription() {
		return "HAProxyProbe collect's HAProxy Load Balancer usage stats.";
	}

	@Override
	public ProbeMetric collect() {
		HashMap<String,Integer> curValues = this.calcValues();
		long curtime = System.currentTimeMillis()/1000;

		int scur = 0;
	    long sessionCount = 0;
	    double requestThroughput = 0.0;
	    int servers = 0;
	    long t = curtime-lasttime;
	    
		if (curValues != null && t >= 0){
			scur = curValues.get("scur");
		    sessionCount = curValues.get("stot");
		    requestThroughput = (1.0*(sessionCount - lastSessionCount))/t;
		    servers = curValues.get("servers");
		}
		else{
			scur = 0;
			requestThroughput = 0.0;
			sessionCount = 0;
		}
			
		
        HashMap<Integer,Object> values = new HashMap<Integer,Object>();
        values.put(0, scur);
        values.put(1, requestThroughput);
        values.put(2, servers);
	    
        this.lasttime = curtime;
        this.lastSessionCount = sessionCount;
        
//	    for (Entry<Integer,Object> entry : values.entrySet()){
//			System.out.println(entry.getKey()+" "+entry.getValue());
//		}
	    
		return new ProbeMetric(values);
	}
	
	private HashMap<String,Integer> calcValues(){
		HashMap<String,Integer> statMap = new HashMap<String,Integer>();
		try{
			URL obj = new URL(haproxy_url);
			HttpURLConnection conn = (HttpURLConnection) obj.openConnection();
			conn.setRequestMethod("GET");		
			conn.setRequestProperty("Authorization", auth_header);
			BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String line;
			int scur = 0;
			int stot = 0;
			int servers = 0;
			if(conn.getResponseCode() == 200){
				while ((line = in.readLine()) != null){
					if(line.contains(this.proxy_name) && !line.contains("BACKEND") && !line.contains("FRONTEND")){
						scur += Integer.parseInt(line.split(",")[4]);
						stot += Integer.parseInt(line.split(",")[7]);
						servers++;
						continue;
					}
				}
				statMap.put("scur", scur);
				statMap.put("stot", stot);
				statMap.put("servers", servers);
			}
			in.close();			
		}
		catch(Exception e){
			this.writeToProbeLog(Level.WARNING, e);
			return null;
		}
		return statMap;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		HAProxyProbe probe = new HAProxyProbe();
		probe.activate();
	}
}
