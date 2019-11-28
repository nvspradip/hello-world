package idc.psr.helidon;


import io.helidon.config.Config;
import io.helidon.webserver.RequestPredicate;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;

import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;

import io.jaegertracing.internal.JaegerTracer;

import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;

import java.io.InputStreamReader;

import java.lang.management.ManagementFactory;

import java.net.InetAddress;

import java.net.UnknownHostException;

import java.util.List;
import java.util.Map;

import javax.enterprise.context.RequestScoped;


import javax.inject.Inject;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;

import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.metrics.annotation.Metered;

@Path("hello")
@RequestScoped //allows us to use CDI injection
public class HelloWorld implements Service {
    
//    @Context
  //  private SpanContext spanContext;
//  @Inject
//  private ServerRequest request;
    private boolean test;
    private String recID;
    @Context
    io.helidon.webserver.ServerRequest serverRequest;
    
   @GET
   @Metered
   public String hello() {
       String text="";
        Map<String, List<String>> map = serverRequest.queryParams().toMap();
        if(map.containsKey("diag.jfr"))
          text=this.startJFR().toString();
        text=text+"<br>"+" Local Instance check default is false, Check "+test+" ";
        
        Tracer tracer = GlobalTracer.get();
     //  JaegerTracer jTracer=(JaegerTracer)tracer; fires exception 
     
       Span localSpan = tracer
                           .buildSpan("helloWorld").asChildOf(serverRequest.spanContext())
                           .start();
       
                localSpan.setTag("Test tag","Test Value" );
            //    System.out.println("Tracer Information "+GlobalTracer.get().toString() +" \n "+jTracer );
                  //tracer.scopeManager().activate(localSpan,true);
                localSpan.log("Test String");
                localSpan.setTag("http.status_code", 200);
     
       
       //System.out.println(tracer.toString());
       String env="";
        Map<String, String> env_2 = System.getenv();
        for(String key:env_2.keySet()){
            env=env+" "+key+" "+env_2.get(key)+"<br>";
        }
        InetAddress addr=null;
        try {
            addr = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
        }
        String host="";
       
        if(addr!=null)
        host=addr.getHostName()+" "+addr.getHostAddress()+" "+addr.getCanonicalHostName()+" <br><br>Ignore below for a while <br><br>"+env;
      //  System.out.println("test "+env);
      localSpan.finish();
        try {
            Thread.currentThread().sleep(400);
        } catch (InterruptedException e) {
        }
        if(map.containsKey("diag.jfr") && recID!=null )
             this.stopJFR(recID);
        return text+"<br><br>"+"Hello World with Span version: "+Config.builder().build().get("version")  +" "+ host;
   }

    @Override
    public void update(Routing.Rules rules) {
        // TODO Implement this method
        System.out.println("Testing if jax and routing can be overrided at once");
    }
    
    @Path("/startJFR")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject startJFR(){//ServerRequest request,ServerResponse response){
        test=true;
        JsonObjectBuilder builder = Json.createObjectBuilder();
        String env = System.getenv("JAVA_HOME");
        String psrLoc=System.getProperty("psrLoc");
        if(psrLoc==null){
            System.out.println("Could not get system property psrLoc");
            psrLoc="/scratch";
        }
        //Runtime.getRuntime().exec("");
        File f = new File ( psrLoc+File.separator+"file_"+System.getenv("HOSTNAME")+"_"+System.currentTimeMillis()+".jfr" );
        try {
            f.createNewFile();
            FileWriter fw=new FileWriter(f.getAbsolutePath());
            fw.write("This is a test entry "+System.currentTimeMillis());
            fw.close();
        } catch (IOException e) {
        }
        String cmdName=env+File.separator+"bin"+File.separator+"jcmd "+ManagementFactory.getRuntimeMXBean().getName().split("@")[0] +" JFR.start duration=10m filename="+f.getAbsolutePath() ;
        builder.add("cmd", cmdName  );
        StringBuilder output = new StringBuilder();
        try{
            Process exec = Runtime.getRuntime().exec(cmdName);
            
            BufferedReader reader = new BufferedReader(
            new InputStreamReader(exec.getInputStream()));
            String line;
                   while ((line = reader.readLine()) != null) {
                                    output.append(line + "\n");
                            }

                            int exitVal = exec.waitFor();
                            if (exitVal == 0) {
                                builder.add("Command Result", exec.exitValue()+" "+output.toString().replaceAll("\n", "<br>")  );
                            } else {
                                    //abnormal...
                                builder.add("Command Result", exec.exitValue()+" Command could not succeed");
                            }

           
        }catch(Exception e){
            builder.add("Command Result", "Exception Could Not Execute JFR " +e.toString() );
            
        }
        recID=output.toString().replaceAll("\n", " ttt ").split("ttt")[1].replaceAll("[^0-9]", "");
        //System.out.println("Temp String Recording "+tmpString+" Stop \n"+output.toString());
        builder.add("JFR Recording Path", "Have Started JFR and it is available to download at  <a href="+System.getenv("psr_mount")+File.separator+f.getName()+">"+System.getenv("psr_mount")+File.separator+f.getName()+"</a>"); 
        builder.add("Recording ID ", recID);
        return builder.build();
     //   response.send("This is registered next foo request !!");
    }
    void stopJFR(String recordingID){
        String cmdName=System.getenv("JAVA_HOME")+File.separator+"bin"+File.separator+"jcmd "+ManagementFactory.getRuntimeMXBean().getName().split("@")[0] +" JFR.stop recording="+recordingID ;
        try{
            Process exec = Runtime.getRuntime().exec(cmdName);
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
