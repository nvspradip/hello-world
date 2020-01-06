package idc.psr.helidon;

import io.helidon.health.checks.HealthChecks;

import java.util.Map;
import java.util.Optional;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Liveness;

@Liveness 
public class PSRHealthCheck implements HealthCheck {
    public PSRHealthCheck() {
        super();
    }

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder builder = HealthCheckResponse.named("CustomHealthCheck");

        HealthCheckResponseBuilder jBuilder = JDBCTest.getStatus();
        Optional<Map<String, Object>> data = jBuilder.build().getData();
        HealthCheck check = HealthChecks.diskSpaceCheck();
        if(data!=null)
           if(data.isPresent())
        for(String s:data.get().keySet())
            builder.withData(s, data.get().get(s).toString());

        if(jBuilder.build().getState() ==  HealthCheckResponse.State.DOWN  ){
            builder.down();
            
        }else{
            builder.up();
        }
        // TODO Implement this method
        return builder.build();
    }
}
