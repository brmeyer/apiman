/*
 * Copyright 2014 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.apiman.gateway.test;

import io.apiman.gateway.engine.beans.Application;
import io.apiman.gateway.engine.beans.Service;
import io.apiman.gateway.engine.beans.exceptions.PublishingException;
import io.apiman.gateway.engine.beans.exceptions.RegistrationException;
import io.apiman.gateway.engine.components.IHttpClientComponent;
import io.apiman.gateway.engine.components.IPolicyFailureFactoryComponent;
import io.apiman.gateway.engine.components.IRateLimiterComponent;
import io.apiman.gateway.engine.components.ISharedStateComponent;
import io.apiman.gateway.engine.impl.InMemoryRateLimiterComponent;
import io.apiman.gateway.engine.impl.InMemoryRegistry;
import io.apiman.gateway.engine.impl.InMemorySharedStateComponent;
import io.apiman.gateway.engine.policy.PolicyFactoryImpl;
import io.apiman.gateway.platforms.servlet.PolicyFailureFactoryComponent;
import io.apiman.gateway.platforms.servlet.components.HttpClientComponentImpl;
import io.apiman.gateway.platforms.servlet.connectors.HttpConnectorFactory;
import io.apiman.gateway.platforms.war.WarEngineConfig;
import io.apiman.gateway.platforms.war.WarGateway;
import io.apiman.gateway.test.server.EchoServer;
import io.apiman.gateway.test.server.GatewayServer;
import io.apiman.test.common.util.TestPlanRunner;

import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * Base class for all Gateway tests.
 *
 * @author eric.wittmann@redhat.com
 */
public class AbstractGatewayTest {
    
    protected static final int ECHO_PORT = 7654;
    protected static final int GATEWAY_PORT = 8080;
    protected static final int GATEWAY_PROXY_PORT = 8081;
    protected static final boolean USE_PROXY = false; // if you set this to true you must start a tcp proxy on 8081
    
    static {
        configureGateway();
    }

    private static EchoServer echoServer = new EchoServer(ECHO_PORT);
    private static GatewayServer gatewayServer = new GatewayServer(GATEWAY_PORT);

    @BeforeClass
    public static void setup() throws Exception {
        echoServer.start();
        gatewayServer.start();
    }

    /**
     * Configures the gateway by settings system properties.
     */
    protected static void configureGateway() {
        System.setProperty(WarEngineConfig.APIMAN_GATEWAY_REGISTRY_CLASS, InMemoryRegistry.class.getName());
        System.setProperty(WarEngineConfig.APIMAN_GATEWAY_CONNECTOR_FACTORY_CLASS, HttpConnectorFactory.class.getName());
        System.setProperty(WarEngineConfig.APIMAN_GATEWAY_POLICY_FACTORY_CLASS, PolicyFactoryImpl.class.getName());
        
        // Register test components
        System.setProperty(WarEngineConfig.APIMAN_GATEWAY_COMPONENT_PREFIX + ISharedStateComponent.class.getSimpleName(), 
                InMemorySharedStateComponent.class.getName());
        System.setProperty(WarEngineConfig.APIMAN_GATEWAY_COMPONENT_PREFIX + IRateLimiterComponent.class.getSimpleName(), 
                InMemoryRateLimiterComponent.class.getName());
        System.setProperty(WarEngineConfig.APIMAN_GATEWAY_COMPONENT_PREFIX + IPolicyFailureFactoryComponent.class.getSimpleName(), 
                PolicyFailureFactoryComponent.class.getName());
        System.setProperty(WarEngineConfig.APIMAN_GATEWAY_COMPONENT_PREFIX + IHttpClientComponent.class.getSimpleName(), 
                HttpClientComponentImpl.class.getName());
    }

    @AfterClass
    public static void shutdown() throws Exception {
        gatewayServer.stop();
        echoServer.stop();
    }

    /**
     * @param service the service to publish
     * @throws PublishingException 
     */
    protected void publishService(Service service) throws PublishingException {
        WarGateway.engine.publishService(service);
    }
    
    /**
     * @param application the app to register for the test
     * @throws RegistrationException 
     */
    protected void registerApplication(Application application) throws RegistrationException {
        WarGateway.engine.registerApplication(application);
    }

    /**
     * Runs the given test plan.
     * @param planPath
     */
    protected void runTestPlan(String planPath) {
        System.setProperty("apiman-gateway-test.endpoints.echo", getEchoEndpoint()); //$NON-NLS-1$
        runTestPlan(planPath, getClass().getClassLoader());
    }
    
    /**
     * Runs the given test plan.
     * @param planPath
     * @param classLoader
     */
    protected void runTestPlan(String planPath, ClassLoader classLoader) {
        String baseApiUrl = getGatewayEndpoint();
        TestPlanRunner runner = new TestPlanRunner(baseApiUrl);
        runner.runTestPlan(planPath, classLoader);
    }

    /**
     * @return the gateway endpoint
     */
    protected String getGatewayEndpoint() {
        int port = GATEWAY_PORT;
        if (USE_PROXY) {
            port = GATEWAY_PROXY_PORT;
        }
        String baseApiUrl = "http://localhost:" + port; //$NON-NLS-1$
        return baseApiUrl;
    }
    
    /**
     * @return the echo server endpoint
     */
    protected String getEchoEndpoint() {
        return "http://localhost:" + ECHO_PORT; //$NON-NLS-1$
    }

}
