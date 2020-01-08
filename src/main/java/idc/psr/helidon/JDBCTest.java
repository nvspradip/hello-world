package idc.psr.helidon;

import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.lang.StringBuilder;

import java.net.InetAddress;
import java.net.UnknownHostException;

import java.sql.Statement;

import java.util.HashMap;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import oracle.ucp.jdbc.PoolDataSource;
import oracle.ucp.jdbc.PoolDataSourceFactory;
import oracle.ucp.jdbc.ValidConnection;

import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;

public class JDBCTest {
    public JDBCTest() {
        super();
        InetAddress addr=null;
        try {
            addr = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
        }
        if(addr!=null)
        host=addr.getHostName()+" "+addr.getHostAddress()+" "+addr.getCanonicalHostName()+"";
        System.out.println("JDBCTest Initialized");
    }

    public static void main(String[] args) {
        init("ocs_soainfra");
        JDBCTest jDBCTest = new JDBCTest();
        String result=jDBCTest.executeSQL("Select to_char(sysdate,'dd-mon-yyy hh24:mi:ss'),sys_context( 'userenv', 'session_user' ) from dual ","ocs_soainfra");
        System.out.println(result);
        
    }
    static HashMap<String,PoolDataSource> pdsMap=new HashMap<String,PoolDataSource>();
    static String host="";
    static void init(String user){
        if(pdsMap.containsKey(user))
            return;
        try{   
               PoolDataSource pds = PoolDataSourceFactory.getPoolDataSource();
                System.out.println("testing for every entry "+ pds.toString()+ "  "+ pds.UCP_DESCRIPTION );
             //   pds.setURL("jdbc:oracle:thin:@//slc09sxu.us.oracle.com:1521/IDM");  // (1) slcn10cn11.us.oracle.com:1530/SYS$BACKGROUND
                pds.setURL("jdbc:oracle:thin:@//slcn10cn11.us.oracle.com:1521/zgmc_dc1_gsi_f:POOLED");  //zgmc_dc1_gsi_f:POOLED
                pds.setConnectionFactoryClassName("oracle.jdbc.pool.OracleDataSource"); 
                pds.setInitialPoolSize(1);
                pds.setUser(user);
                pds.setPassword("welcome123"); //manager
                pds.setMinPoolSize(2);
                pds.setMaxPoolSize(30);
                pds.setMaxStatements(20);
                pds.setInactiveConnectionTimeout(600);
             //   pds.setConnectionProperty("oracle.jdbc.ReadTimeout", "5000");
                pdsMap.put(user, pds);
 
        }catch(SQLException e){
            e.printStackTrace();
        }
        
    }
    void setStatus(ServerRequest request, ServerResponse response) {
        StringBuilder sb=new StringBuilder();
        Set<String> keySet = pdsMap.keySet();
        sb.append("Host Name : "+host+"\n");
        for(String key:keySet){
            PoolDataSource dataSource = pdsMap.get(key);
            
            try {
                sb.append(key+"  "+dataSource.getDataSourceName() + " user "+ dataSource.getUser() +"  url " + dataSource.getURL() + "  available  " +
                          dataSource.getAvailableConnectionsCount() + " borrowed " +
                          dataSource.getBorrowedConnectionsCount()+" total "+dataSource.getStatistics().getTotalConnectionsCount() +"\n");
            } catch (SQLException e) {
            }
        }
        response.send(sb.toString());
    }
    void ocs_soainfra(ServerRequest request, ServerResponse response) {
        //init("ocs_soainfra");
        String user="spectra1";
//        System.out.println("request params "+ request.queryParams().toMap().size()+ "   toString "+request.queryParams().toString() );
        Map<String, List<String>> map = request.queryParams().toMap();
        for(String key:map.keySet()){
            if(key.equalsIgnoreCase("user")){
                List<String> list = map.get(key);
//                System.out.println("List size "+list.size()+"  first value "+list.get(0));
                if( list.get(0)!=null )
                    user=list.get(0);
                break;
            }
                
        }

        init(user);
        String sql="Select to_char(sysdate,'dd-mon-yyy hh24:mi:ss'),sys_context( 'userenv', 'session_user' ) from dual ";
        String res = this.executeSQL(sql,user);
        if(res.length()==0)
            response.status(500).send("Exception faced");
        else
            response.send(res);

    }
    void test_adf(ServerRequest request, ServerResponse response) {
        //init("test_adf");
        String user="ocs_soainfra";
        //        System.out.println("request params "+ request.queryParams().toMap().size()+ "   toString "+request.queryParams().toString() );
        Map<String, List<String>> map = request.queryParams().toMap();
        for(String key:map.keySet()){
            if(key.equalsIgnoreCase("user")){
                List<String> list = map.get(key);
        //                System.out.println("List size "+list.size()+"  first value "+list.get(0));
                if( list.get(0)!=null ){
                    user=list.get(0);
                    if(pdsMap.containsKey(user))
                     pdsMap.remove(user);
                    response.send("removed pool belonging to "+user);
                    return;
                }
                break;
            }
                
        }
        String sql="Select to_char(sysdate,'dd-mon-yyy hh24:mi:ss'),sys_context( 'userenv', 'session_user' ) from dual ";
        String res = this.executeSQL(sql,user);
        init(user);
        if(res.length()==0)
            response.status(500).send("Exception faced");
        else
            response.send(res);

    }
    
    
    public String executeSQL(String sql,String user) {
        StringBuilder sb=new StringBuilder();
        try{
        Connection connection = pdsMap.get(user).getConnection();
        connection.setClientInfo("OCSID.MODULE", "ucpspectra");
        Statement stmt = connection.createStatement();
        ResultSet res = stmt.executeQuery(sql);
        while(res.next()){
            sb.append("  "+res.getString(1)+" "+res.getString(2)+"  ");
        }
            stmt.close();
            if(user.toLowerCase().contains("spectra") ){
            stmt = connection.createStatement();
            stmt.setFetchSize(100);
            ResultSet res1=stmt.executeQuery("select * from  fusion.per_keywords where rownum<400");
       //     int i=0;
            while(res1.next()){
                //Do nothing 
             //   System.out.println("loop "+(i++));
            }
            
            stmt.close();
            stmt = connection.createStatement();
            stmt.execute("select count(*) from fusion.per_all_assignments_m");
            stmt.close();
            stmt=connection.createStatement();
            stmt.execute("select count(*) from fusion.per_users");
            stmt.close();
        //    System.out.println("executed addln sqls ");
            }else if(user.equalsIgnoreCase("test_adf")){
         /*       stmt=connection.createStatement();
                stmt.execute("select * from psr_bugs where update_date < (sysdate-10) and rownum < 10");
                stmt.close();
                stmt=connection.createStatement();
                stmt.execute("select * from psr_emp");
                stmt.close();*/
               // System.out.println("executed addln sqls ");
                
            }
            Thread.currentThread().sleep(100 );  //  (int)(1000.0 * Math.random())
            connection.close();
            
        }catch(Exception e){
            e.printStackTrace();
            
        }
        
        return sb.toString();

    }
    
    public static HealthCheckResponseBuilder getStatus(){
        HealthCheckResponseBuilder builder=HealthCheckResponse.named("UCPDatasource");
        Set<String> keySet = pdsMap.keySet();
        for(String key:keySet){
            PoolDataSource dataSource = pdsMap.get(key);
            Connection connection = null;
            try{
            connection=dataSource.getConnection();    
            connection.isValid(30);
            builder.up().withData(dataSource.getURL()+" "+dataSource.getUser(),"Success" );    
            connection.close();
            }catch(Exception e){
                builder.down().withData(dataSource.getURL()+" "+dataSource.getUser()," Exception "+e.toString() );
                break;
            }
        }
        return builder;
        
    }
        
    
}
