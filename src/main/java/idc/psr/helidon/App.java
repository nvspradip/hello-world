package idc.psr.helidon;

import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import io.helidon.config.ConfigValue;
import io.helidon.config.spi.ConfigParser;
import io.helidon.health.HealthSupport;
import io.helidon.health.checks.HealthChecks;
import io.helidon.media.jsonp.server.JsonSupport;
import io.helidon.metrics.MetricsSupport;
import io.helidon.microprofile.server.Server;
import io.helidon.tracing.TracerBuilder;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerConfiguration;
import io.helidon.webserver.StaticContentSupport;
import io.helidon.webserver.WebServer;

import io.helidon.webserver.accesslog.AccessLogSupport;

import io.opentracing.Span;

import java.net.URI;

import java.nio.file.Paths;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        PSRHealthCheck psrHealthCheck=new PSRHealthCheck();
        WebServer webServer;
        try {
            
            HealthCheck hc = () -> HealthCheckResponse
                    .named("exampleHealthCheckLive")
                    .up()
                    .withData("time", System.currentTimeMillis())
                    .build();
            
            
            HealthCheck hc1 = () -> HealthCheckResponse
                    .named("exampleAddReadiness")
                    .up()
                    .withData("readinessTime", System.currentTimeMillis())
                    .build();

            HealthSupport health = HealthSupport.builder()
                .addLiveness(hc)
                .addReadiness(hc1)
                .addReadiness(HealthChecks.healthChecks() )
                .addReadiness(psrHealthCheck)
                .build();
            
            HealthSupport health1 = HealthSupport.builder()
                .addReadiness(hc1)
                .build();
            
      /*      ServerConfiguration.builder()
                               .tracer(TracerBuilder.create("my-application")                    
                                             .collectorUri(URI.create("http://127.0.0.1:14268"))  
                                             .build())
                               .build();
      */            
            webServer = WebServer.create(Routing.builder()
                                                .any((req, res) -> res.send("It works!"))
                                                .build())
                                 .start()
                                 .toCompletableFuture()
                                 .get(10, TimeUnit.SECONDS);
            System.out.println("Server started at: http://localhost:" + webServer.port()); 

            ServerConfiguration configuration = ServerConfiguration.builder()
                                                                   .port(8080)   //make it 8080 before deploying
                                                                   .build();
         //   final JsonBindingSupport jsonBindingSupport = JsonBindingSupport.create();
         
            Routing routing=Routing.builder()
                                   //.register(AccessLogSupport.create(config.get("server.access-log")))
                                   //.register(JsonSupport.create())
                                   //.get("/sayhello", Handler.create(JsonObject.class, this::sayHello))
                             //      .trace(TracerBuilder.create("my-application").collectorUri(URI.create("http://127.0.0.1:14269")).build())            
                                   
                                   .register(health)
                                   .register(AccessLogSupport.create(Config.builder().build().get("server.access-log")))
                               //    .register(health1)
                                   .register("/pics",StaticContentSupport.create(Paths.get("D:\\Pics\\Jun_2018\\Samsung") ) ).any((req,res) -> res.send("port 8080") )
                                   
                                   .build() ;
            WebServer webServer2 = WebServer.create(configuration,routing  );
            webServer2.start();
            ServerConfiguration configuration3 = ServerConfiguration.builder()
                                                                   .port(8081).workersCount(25)
                                                                   .build();
            
        
            
   //        health=HealthSupport.create();
            WebServer webserver3=WebServer.create(configuration3,
                                                  Routing.builder()
                                                         .register(AccessLogSupport.create(Config.builder().build().get("server.access-log")))
                                                         .register(MetricsSupport.create() )
                                                         .register(JsonSupport.create())
                                                         .register(health)
                                                      //   .register(health1)
                                                         .register("/myService",new MyService())
                                                         .build()  );
            
            
            webserver3.start();
            
            
                //Server server = Server.create().start(); 
                Server server = Server.builder().addResourceClass(HelloWorld.class).build().start();
            
            
            System.out.println("*****************http://localhost:" + server.port() + "/hello   host "+server.host() ); 
            Config config=Config.builder().build();
            
          //  ConfigValue<Map<String, String>> asMap = config.asMap();
//            System.out.println("asMap ----> "+asMap.key().name()+"  size "+asMap.get().size() );
//            asMap.get().forEach((key,value) -> System.out.println(key +" --> "+value));     
            System.out.println("config test "+config.get("greeting") );
            System.out.println("config test1 "+config.get("server.port") );
         //   config.asMap().get().forEach((key,value) -> System.out.println(" config key "+key+" - "+value));


            // Properties properties = System.getProperties();
//            properties.forEach( (k,v) -> System.out.println(k+"   "+v));
//            System.getenv().forEach((key,value) -> System.out.println(key+" "+value));

            //            routingService.register("/myService",new MyService());
            

        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            e.printStackTrace();
        }


    }
    
    HealthCheckResponse exampleHealthCheck(){
        return HealthCheckResponse
            .named("exampleHealthCheck")
            .up()
            .build();
    }
}
