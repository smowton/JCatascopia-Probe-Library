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

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import eu.celarcloud.jcatascopia.probepack.Probe;
import eu.celarcloud.jcatascopia.probepack.ProbeMetric;
import eu.celarcloud.jcatascopia.probepack.ProbePropertyType;


public class RabbitMQProbe extends Probe{
	
	private static final String CONFIG_PATH = "rabbitmq.properties";
	private Properties config;
	private String[] queues;
	private String auth_header;
	private String rabbitmq_url;
	private HashMap<String,Integer> fieldIDMap;

	public RabbitMQProbe(String name, int freq) {
		super(name, freq);
		
		parseConfig();
		String ip = config.getProperty("rabbitmq.ip", "localhost");
	    String port = config.getProperty("rabbitmq.port", "5672");
	    String user = config.getProperty("rabbitmq.username", "guest");
	    String pass = config.getProperty("rabbitmq.password", "guest");
	    String vhost = config.getProperty("queue.vhost", "%2F");
	    queues = config.getProperty("queue.name", "myqueue").split(",");
	    String creds = user+":"+pass;
	    auth_header = "Basic " + new sun.misc.BASE64Encoder().encode(creds.getBytes());
	    rabbitmq_url = "http://"+ip+":"+port+"/api/queues/"+vhost+"/";
	    
		fieldIDMap = new HashMap<String, Integer>();
		int f = 0;
	    for (int i=0;i<queues.length;i++) {
		    this.addProbeProperty(f,queues[i]+".memory",ProbePropertyType.INTEGER,"KB","Total amount of RAM used by this queue in KB");
		    fieldIDMap.put(queues[i]+".memory", f++);
			this.addProbeProperty(f,queues[i]+".consumers",ProbePropertyType.INTEGER,"#","Number of consumers this queue has");
			fieldIDMap.put(queues[i]+".consumers", f++);
			this.addProbeProperty(f,queues[i]+".msgs",ProbePropertyType.INTEGER,"#","Total number of messages in this queue");
			fieldIDMap.put(queues[i]+".msgs", f++);
			this.addProbeProperty(f,queues[i]+".dlvr_rate",ProbePropertyType.DOUBLE,"msgs/s","Rate at which messages are delivered");
			fieldIDMap.put(queues[i]+".dlvr_rate", f++);
			this.addProbeProperty(f,queues[i]+".pub_rate",ProbePropertyType.DOUBLE,"msgs/s","Rate at which messages are published to the queue");
			fieldIDMap.put(queues[i]+".pub_rate", f++);
	    }
	}
	
	public RabbitMQProbe(){
		this("RabbitMQProbe",30);
	}
	
	@Override
	public String getDescription() {
		return "RabbitMQProbe collect's RabbitMQ queue usage stats";
	}

	@Override
	public ProbeMetric collect() {
		HashMap<Integer,Object> values = new HashMap<Integer,Object>();
		for (String q : queues){
			try{
				URL obj = new URL(rabbitmq_url+q.trim());
				HttpURLConnection conn = (HttpURLConnection) obj.openConnection();
				conn.setRequestMethod("GET");		
				conn.setRequestProperty("Authorization", auth_header);
				BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
				StringBuffer sb = new StringBuffer();
				String line;
				if(conn.getResponseCode() == 200){
					while ((line = in.readLine()) != null)
						sb.append(line);
					
					JSONParser parser = new JSONParser(); 
					JSONObject json = (JSONObject) parser.parse(sb.toString());
					
					int memory = ((Long) json.get("memory")).intValue()/1024; //KB
					int consumers = ((Long) json.get("consumers")).intValue(); 
					int msgs = ((Long) json.get("messages")).intValue(); 
					JSONObject msg_stats = (JSONObject) json.get("message_stats");
					double dlvr_rate = (Double) ((JSONObject) msg_stats.get("deliver_details")).get("rate");
					double pub_rate = (Double) ((JSONObject) msg_stats.get("publish_details")).get("rate");
					
					values.put(fieldIDMap.get(q+".memory"), memory);
					values.put(fieldIDMap.get(q+".consumers"), consumers);
					values.put(fieldIDMap.get(q+".msgs"), msgs);
					values.put(fieldIDMap.get(q+".dlvr_rate"), dlvr_rate);
					values.put(fieldIDMap.get(q+".pub_rate"), pub_rate);	
				}
			}
			catch(Exception e){
				this.writeToProbeLog(Level.WARNING, e);
				continue;
			}
		}
//	    for (Entry<Integer,Object> entry : values.entrySet()){
//			System.out.println(entry.getKey()+" "+entry.getValue());
//		}
		return new ProbeMetric(values);
	}	
	
	//parse the configuration file
	private void parseConfig() {
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
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		RabbitMQProbe probe = new RabbitMQProbe();
		probe.activate();
	}
}
