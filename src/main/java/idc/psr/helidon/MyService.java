package idc.psr.helidon;


import io.helidon.webserver.RequestHeaders;
import io.helidon.webserver.RequestPredicate;
import io.helidon.webserver.ResponseHeaders;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;

import io.jaegertracing.Configuration;

import io.jaegertracing.internal.JaegerSpanContext;
import io.jaegertracing.internal.JaegerTracer;

import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;
import oracle.dms.context.ExecutionContext;

import java.io.File;

import java.io.IOException;

import java.lang.management.ManagementFactory;

import java.nio.file.Path;

import java.text.SimpleDateFormat;

import java.util.Base64;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import java.util.Map.Entry;
import java.util.TimeZone;

import java.util.concurrent.atomic.AtomicInteger;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import net.jodah.failsafe.Execution;

import oracle.dms.context.RID;

import oracle.jrockit.jfr.FlightRecorder;
import oracle.jrockit.jfr.Recording;


public class MyService implements Service {
    private boolean resSent;
    JDBCTest jTest=new JDBCTest();
    public MyService(){
       // JDBCTest.init("ocs_soainfra");
    }

    @Override
    public void update(Routing.Rules rules) {
        //  String test="";
        // TODO Implement this method
        System.out.println("entered this method");
        rules.get("/", this::getDefault);
        rules.get("/myService", this::getHandler); // /expense ,, /invoice
        rules.get("/foo",
                  RequestPredicate.create()
                                          .containsQueryParameter("test")
                                          .thenApply((req, res) -> {
                                                     // Some logic
                                                     System.out.println("matched criteria  " + res.toString());
                                                     //  test="matched criteria<br>";
                                                     //   res.send("Have quailified for first criteria");
                                                     req.next(); //testing if request flows to next block
                                                     System.out.println("Sent to foo.. this is afer that execution " +
                                                                        res.requestId());
            if (!resSent) {
                res.send("matched argument " + req.spanContext() + " " + req.remoteAddress());
                System.out.println("Response is already sent If Loop .... ");
            }
        }).otherwise((req, resp) -> { /* Otherwise logic */
                     System.out.println("default criteria");
            resp.send("Have defaulted the search");
        })); // Optional. Default logic is req.next()
        //this is to test .next filter
        rules.get("/foo", this::getFooNext);
        rules.get("/timeout", this::timeout);
        rules.get("/callwls", this::callwls);
        rules.get("/callSpectra", this::callSpectra);
        rules.get("/ocs_soainfra",jTest::ocs_soainfra);
        rules.get("/test_adf",jTest::test_adf);
        rules.get("/ds_status",jTest::setStatus);
        

    }

    private void getHandler(ServerRequest request, ServerResponse response) {
        Configuration.SamplerConfiguration samplerConfig = Configuration.SamplerConfiguration
                                                                        .fromEnv()
                                                                        .withType("const")
                                                                        .withParam(1);
        Configuration.ReporterConfiguration reporterConfig = Configuration.ReporterConfiguration
                                                                          .fromEnv()
                                                                          .withLogSpans(true);
        Configuration config =
            new Configuration("jaeger tutorial").withSampler(samplerConfig).withReporter(reporterConfig);

        System.out.println("sampler configuration   ");

        JaegerTracer jTracer = config.getTracer();

        // Tracer tracer = Configuration.fromEnv().getTracer();


        Span localSpan = null;
        Tracer tracer = GlobalTracer.get();
        localSpan = jTracer.buildSpan("getActivityHistory").start();

        localSpan.setTag("Test tag", "Test Value");
        //    System.out.println("Tracer Information "+GlobalTracer.get().toString() +" \n "+jTracer );
        //tracer.scopeManager().activate(localSpan,true);
        localSpan.log("Test String");
        localSpan.setTag("http.status_code", 200);
        System.out.println(jTracer + "  \n " + jTracer.toString());

        try {
            Thread.currentThread().sleep(20);
        } catch (InterruptedException e) {
        } finally {

        }
        this.innerMethod(jTracer, localSpan);
        localSpan.finish();

        String client = request.remoteAddress() + ":" + request.remotePort();
        Map<String, List<String>> map = request.headers().toMap();
        for (String s : map.keySet()) {
            System.out.println("headers " + s + " ");
            map.get(s).forEach(s1 -> System.out.print(s1 + "   "));
        }

        response.send("This is from Service Hanlder !! " + client);


    }

    private void innerMethod(JaegerTracer tracer, Span rootSpan) {
        Span span = tracer.buildSpan("service inner method")
                          .asChildOf(rootSpan)
                          .start();
        try {
            Thread.currentThread().sleep(40);
        } catch (InterruptedException e) {

        } finally {
            span.finish();
        }


    }

    private void getDefault(ServerRequest request, ServerResponse response) {
        response.send("This is default Handler and modified on Oct 9 !! " + request.headers().toString());
    }

    private void getFooNext(ServerRequest request, ServerResponse response) {
        response.send("This is registered next foo request !!");
        response.whenSent().thenRun(() -> {
            System.out.println("Response is done");
            this.setResSent(true);
        });

    }

    private JsonObject dumpJFR(ServerRequest request, ServerResponse response) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("JFR Recording Path",
                    "Have Started JFR and it is available to download at /scratch/jfr/jvm_flightRecording279_i565.jfr ");
        //Runtime.getRuntime().exec("");
        File f = new File("file_" + System.currentTimeMillis() + ".jfr");
        try {
            f.createNewFile();
        } catch (IOException e) {
        }

        return builder.build();
        //   response.send("This is registered next foo request !!");
    }


    public void setResSent(boolean resSent) {
        this.resSent = resSent;
    }

    public boolean isResSent() {
        return resSent;
    }

    private void timeout(ServerRequest request, ServerResponse response) {
        long duration = 0;
        try {
            long start = System.currentTimeMillis();
            Thread.currentThread().sleep(62000);
            long end = System.currentTimeMillis();
            duration = end - start;
        } catch (InterruptedException e) {
            //e.printStackTrace();
        } finally {
            response.send("This is to test timeout method, Time took " + duration + " millis");
        }

    }
    private String useDMS(String ecidContext){
     //   ecidContext="1.test;kXigv0ZCLILIHVAPnJPRLPJBaHRO_JVB6LVS_VIT_IUTdLRT";
        String cID = ExecutionContext.extractECID(ecidContext);
        System.out.println("usedms "+cID+ "  ecid-header "+ecidContext);
        //boolean b=false;
        boolean b = ExecutionContext.unwrap(ecidContext);
        ExecutionContext context = ExecutionContext.get();
//        String childWrappedContext = ExecutionContext.getChildWrappedContext(ecidContext);
        
        System.out.println("usedms wrap otuput "+b+"  getwrapcontext "+context.wrapContext()+"  ecid  "+ExecutionContext.ECID+"  "+context.wrapContext()+" limited wrap "+context.limitedWrap()+" "+context.toString()
                           
                           );
        
        String newCtx=context.generateNewWrappedContext();
        System.out.println("usedms trying to create new context "+newCtx+" value  "+context.getValue(newCtx)+" "+context.wrap() );
              
        
        System.out.println(context.getRIDasString()+"   "+context.wrap()+"   " );
        RID childRID = context.getRID().createChildRID();
        System.out.println("usedms rid "+childRID.getRID()+ " contextRID "+context.getRID()+"  checkrootID "+context.getRID().isRoot());
        
        //ExecutionContext.pushNewContext();
     //   ExecutionContext pushChild = context.pushChild();
//        System.out.println("usedms context push child "+pushChild.wrapContext()+"  rid "+pushChild.getRIDasString() );
     //   ExecutionContext createChild = context.createChild();
//        System.out.println("usedms context create child "+createChild.wrapContext()+"  rid "+createChild.getRIDasString() );
        ExecutionContext child = context.pushChild();
        System.out.println("usedms pushChild "+child.getRIDasString()+"  "+child.wrapContext()+"  ecid  "+child.toString() );
        //context.pop();
        //System.out.println("usedms pod "+context.getRIDasString()+"  "+context.wrapContext()+"  ecid  "+context.toString() );
        
        //return child.wrapContext();
        return ecidContext;

    }

    private void callwls(ServerRequest request, ServerResponse sResponse) {
        Span localSpan = GlobalTracer.get()
                                     .buildSpan("Span_CallWLS")
                                     .asChildOf(request.spanContext())
                                     .start();
        localSpan.setTag("Class ", "idc.psr.helidon.MyService.callwls");
        localSpan.log("CallWLS Method called by " + request.remoteAddress() + "  " + request.remotePort());
        String method = "CallWLS Method called by " + request.remoteAddress() + "  " + request.remotePort() + "<br>";
        JaegerSpanContext jSpan = (JaegerSpanContext) request.spanContext();
        String traceID = jSpan.getTraceId();
        
        String ecid_context="";
        
        String ecid = "";
                 Map<String, List<String>> map =  request.headers().toMap();
        if (map.containsKey("ECID-Context") || map.containsKey("ecid-context")) {
            if (map.containsKey("ECID-Context")) {
                ecid = map.get("ECID-Context").get(0);
                System.out.println("Interpreting ecid-context callwls");
                ecid_context=this.useDMS(ecid);
            } else {
                ecid = map.get("ecid-context").get(0);
                System.out.println("Interpreting ecid-context callwls");
                ecid_context=this.useDMS(ecid);
            }
            if(ecid!=""){
            ecid = ecid.substring(ecid.indexOf(".") + 1);
            if(ecid.indexOf(":")!=-1)
                ecid = ecid.substring(0, ecid.indexOf(";"));
            System.out.println("ECID Context header found " + ecid);
            }else{
                ecid=jSpan.getTraceId();
            }

        } else {
            ecid = jSpan.getTraceId();
            System.out.println("ECID Context not found " + ecid);
        }
        if(ecid_context=="")
           ecid_context="1." + ecid + ";" + getRID(request.requestId());
           System.out.println("ECID Context passed "+ecid_context);
            
        Client client = ClientBuilder.newClient();
        
        Response response = client.target("http://adc4110092.us.oracle.com:17001/testAssociation/AppModuleService")
                                  .request()
                                  //.header("ECID-Context", "1." + ecid + ";" + getRID(request.requestId()))
                                  .header("ECID-Context", ecid_context)
                                  .header("SOAPAction", "/model/common/invokeSpectra")
                                  .header("Content-Type","text/xml;charset=UTF-8")
                                  .header("User-Agent","Apache-HttpClient/4.1.1 (java 1.5)").post(Entity.entity("<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:typ=\"/model/common/types/\">\n" + 
                                  "   <soapenv:Header/>\n" + 
                                  "   <soapenv:Body>\n" + 
                                  "      <typ:invokeSpectra>\n" + 
                                  "         <typ:url>test</typ:url>\n" + 
                                  "      </typ:invokeSpectra>\n" + 
                                  "   </soapenv:Body>\n" + 
                                  "</soapenv:Envelope>", "text/xml") );
        
        //https://fuscdrmsmc17-fa-ext.us.oracle.com/fscmService/ReceivablesCustomerProfileService
        //http://adc4110092.us.oracle.com:18001/PSRADFBc-Model-context-root/TestADFBCService
        //        System.out.println(request.uri()+"   "+request.uri().hashCode()+"   "+Integer.toHexString((request.uri()+":").hashCode()) );
        //System.out.println(response );
        MultivaluedMap<String, Object> headers = response.getHeaders();
        String respH = "";
        for (String s : headers.keySet())
            respH = respH + "\n" + s + " " + headers.get(s);
      //  System.out.println(response.readEntity(String.class));
            
        respH =
            respH + "\n\n Spectra span/ecid headers sent \nSpan ID  " + localSpan.toString() + "    " + traceID +
            "<br>\n+ Header Passed " + "ECID-Context: 1." + ecid + ";" + getRID(request.requestId());
        localSpan.setBaggageItem("ECID-Context", ecid);
        localSpan.setTag("ECID-Context", ecid);
        localSpan.finish();

        System.out.println("span request " + request.spanContext() + "  trace " + jSpan.getTraceId() + "  span " +
                           jSpan.getSpanId() + " toString  " + jSpan.toString() + " parentID  " + jSpan.getParentId() +
                           " highID " + jSpan.getTraceIdHigh() + "   lowID " + jSpan.getTraceIdLow());
        sResponse.headers().add("ECID-Context", "1." + ecid + ";" + getRID(request.requestId()));
   //     this.useDMS( "1." + ecid + ";" + getRID(request.requestId()));        
        sResponse.send("Invoked Weblogic Request https://fuscdrmsmc17-fa-ext.us.oracle.com/fscmService/ReceivablesCustomerProfileService \nHeaders Received from wls:  " +
                       respH); //+response.readEntity(String.class));
    }

    private void callSpectra(ServerRequest request, ServerResponse sResponse) {
        Span localSpan = GlobalTracer.get()
                                     .buildSpan("Span_CallSpectra")
                                     .asChildOf(request.spanContext())
                                     .start();
        localSpan.setTag("Class ", "idc.psr.helidon.MyService.callSpectra");
        localSpan.log("callSpectra Method called by " + request.remoteAddress() + "  " + request.remotePort());
        String method =
            "callSpectra Method called by " + request.remoteAddress() + "  " + request.remotePort() + "<br>";
        JaegerSpanContext jSpan = (JaegerSpanContext) request.spanContext();
        String traceID = jSpan.getTraceId();
        String ecid_context="";
        
        String reqHeaders = "";
        RequestHeaders headers = request.headers();
       // request.headers().toMap().forEach((key,value)-> { reqHeaders=reqHeaders+" \n"+ key+" "+value.get(0); } );
        Map<String, List<String>> map = headers.toMap();
        for (String s : map.keySet()) {
            List<String> list = map.get(s);
            reqHeaders = reqHeaders + "\n" + s + "  ";
            for (String k : list)
                reqHeaders = reqHeaders + " " + k;
        }
        String ecid = "";
        if (map.containsKey("ECID-Context") || map.containsKey("ecid-context")) {
            if (map.containsKey("ECID-Context")) {
//                localSpan.setBaggageItem("ECID-Context", map.get("ECID-Context").get(0));
                ecid = map.get("ECID-Context").get(0);
                System.out.println("Interpreting ecid-context");
                ecid_context=this.useDMS(ecid);
            } else {
//                localSpan.setBaggageItem("ECID-Context", map.get("ecid-context").get(0));
                ecid = map.get("ecid-context").get(0);
                System.out.println("Interpreting ecid-context");
                ecid_context=this.useDMS(ecid);
            }

            if(ecid!=""){
            ecid = ecid.substring(ecid.indexOf(".") + 1);
            if(ecid.indexOf(":")!=-1)
                ecid = ecid.substring(0, ecid.indexOf(";"));
            System.out.println("ECID Context header found " + ecid);
            }else{
                ecid=jSpan.getTraceId();
            }

        } else {
            ecid = jSpan.getTraceId();
            System.out.println("ECID Context not found " + ecid);
        }
        if(ecid_context=="")
           ecid_context="1." + ecid + ";" + getRID(request.requestId());
        System.out.println("ECID Context passed "+ecid_context);
        sResponse.headers().add("ECID-Context", ecid_context);
        
        localSpan.setBaggageItem("ECID-Context", ecid);
        localSpan.setTag("ECID-Context", ecid);
        Iterator<Map.Entry<String, String>> iterator = jSpan.baggageItems().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, String> entry = iterator.next();
            System.out.println("Baggage items " + entry.getKey() + "  " + entry.getValue());
        }
        String resHeaders = "";
        ResponseHeaders headers_2 = sResponse.headers();

        map = headers_2.toMap();
        for (String s : map.keySet()) {
            List<String> list = map.get(s);
            resHeaders = resHeaders + "\n" + s + "  ";
            for (String k : list)
                resHeaders = resHeaders + " " + k;
        }

        localSpan.finish();
     //   this.useDMS( "1." + ecid + ";" + getRID(request.requestId()));
        sResponse.send(" Headers received from third party to Spectra :\n" + reqHeaders +
                       "\n\nUncommitted Response Headers from Spectra method, Original Response will have few more headers: \n" +
                       resHeaders); //+response.readEntity(String.class));
    }

    private static String getRID(long id) {
        int[] levels = new int[2];
        levels[0] = 0;
        levels[1] = (new Long(id)).intValue();
        ;
        byte[] rid = getWrapRid(levels);
        int size = rid.length + 1;
        byte[] raw = new byte[size];
        int index = copyBytes(raw, 0, rid, 0);
        raw[index] = 59;
        String encode = encode(raw);
        return encode;
    }

    private static String encode(byte[] data) {
        int size = (data.length * 4 + 2) / 3;
        char[] encode = new char[size];


        int eIndex = 0;
        int dIndex = 0;
        int length = data.length;
        while (length >= 3) {
            byte digit = (byte) (data[(dIndex + 0)] & 0x3F);
            encode[(eIndex + 0)] = byteEncode(digit);
            digit = (byte) ((data[(dIndex + 0)] & 0xC0) >>> 2 | data[(dIndex + 1)] & 0xF);

            encode[(eIndex + 1)] = byteEncode(digit);
            digit = (byte) ((data[(dIndex + 1)] & 0xF0) >>> 2 | data[(dIndex + 2)] & 0x3);

            encode[(eIndex + 2)] = byteEncode(digit);
            digit = (byte) ((data[(dIndex + 2)] & 0xFC) >>> 2);
            encode[(eIndex + 3)] = byteEncode(digit);
            eIndex += 4;
            dIndex += 3;
            length -= 3;
        }
        //         System.out.println(encode);
        if (length != 0) {
            byte digit = (byte) (data[(dIndex + 0)] & 0x3F);
            encode[(eIndex + 0)] = byteEncode(digit);
            if (length != 1) {
                digit = (byte) ((data[(dIndex + 0)] & 0xC0) >>> 2 | data[(dIndex + 1)] & 0xF);

                encode[(eIndex + 1)] = byteEncode(digit);
                digit = (byte) ((data[(dIndex + 1)] & 0xF0) >>> 2);
                encode[(eIndex + 2)] = byteEncode(digit);
                eIndex += 3;
            } else {
                digit = (byte) ((data[(dIndex + 0)] & 0xC0) >>> 2);
                encode[(eIndex + 1)] = byteEncode(digit);
                eIndex += 2;
            }
        }
        while (eIndex < size) {
            encode[eIndex] = '0';
            eIndex++;
        }
        //        System.out.println(encode);
        return new String(encode);
    }

    private static char byteEncode(int data) {
        return Base64[data];
    }
    private static final char[] Base64 = {
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L',
        'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '^', '_', 'a', 'b', 'c', 'd', 'e', 'f',
        'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'
    };

    private static byte[] getWrapRid(int[] levels) {
        int size = levels.length * 6;
        char[] rid = new char[size];
        int index = 0;
        for (int depth = 0; depth < levels.length; depth++) {
            int length = encodeInt64var(levels[depth], rid, index);
            index += length;
            int tmp46_45 = (index - 1);
            char[] tmp46_41 = rid;
            tmp46_41[tmp46_45] = ((char) (tmp46_41[tmp46_45] | 0xFF80));
        }
        size = index;
        byte[] raw = new byte[size];
        for (index = 0; index < size; index++) {
            raw[index] = ((byte) rid[index]);
        }
        return raw;
    }

    static int encodeInt64var(int value, char[] encode, int start) {
        int index = start;
        int shift = 36;
        int digit;
        do {
            shift -= 6;
            digit = value >>> shift & 0x3F;
        } while ((shift >= 0) && (digit == 0));
        encode[index] = byteEncode(digit);
        index++;
        while (shift >= 6) {
            shift -= 6;
            digit = value >>> shift & 0x3F;
            encode[index] = byteEncode(digit);
            index++;
        }
        return index - start;
    }

    private static int copyBytes(byte[] dest, int dIndex, byte[] source, int sIndex) {
        while (sIndex < source.length) {
            dest[dIndex] = source[sIndex];
            dIndex++;
            sIndex++;
        }
        return dIndex;
    }


}
