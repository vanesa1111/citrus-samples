/*
 * Copyright 2006-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.consol.citrus.samples.flightbooking;

import com.consol.citrus.container.SequenceBeforeTest;
import com.consol.citrus.dsl.design.TestDesigner;
import com.consol.citrus.dsl.design.TestDesignerBeforeTestSupport;
import com.consol.citrus.dsl.endpoint.CitrusEndpoints;
import com.consol.citrus.http.server.HttpServer;
import com.consol.citrus.jms.endpoint.JmsEndpoint;
import com.consol.citrus.variable.GlobalVariables;
import com.consol.citrus.xml.XsdSchemaRepository;
import com.consol.citrus.xml.namespace.NamespaceContextBuilder;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.commons.dbcp.BasicDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.ContextConfiguration;
import org.xml.sax.SAXException;

import javax.jms.ConnectionFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.Collections;

/**
 * @author Christoph Deppisch
 */
@ContextConfiguration
@PropertySource(value = "citrus.properties")
public class CitrusEndpointConfig {

    @Value("${jms.broker.url}")
    private String jmsBrokerUrl;

    @Value("${travel.agency.request.queue}")
    private String travelAgencyRequestQueue;

    @Value("${travel.agency.response.queue}")
    private String travelAgencyResponseQueue;

    @Value("${smart.airline.request.queue}")
    private String smartAirlineBookingRequestEndpoint;

    @Value("${smart.airline.response.queue}")
    private String smartAirlineBookingResponseEndpoint;

    @Value("${jdbc.driver}")
    private String jdbcDiver;

    @Value("${jdbc.url}")
    private String jdbcUrl;

    @Value("${db.user}")
    private String dbUser;

    @Value("${db.password}")
    private String dbPassword;

    @Bean
    public XsdSchemaRepository schemaRepository() throws ParserConfigurationException, SAXException, IOException {
        XsdSchemaRepository schemaRepository = new XsdSchemaRepository();
        schemaRepository.getLocations().add("classpath:com/consol/citrus/samples/flightbooking/schema/FlightBookingSchema.xsd");
        return schemaRepository;
    }

    @Bean
    public GlobalVariables globalVariables() {
        GlobalVariables variables = new GlobalVariables();
        variables.getVariables().put("project.name", "Citrus FlightBooking sample");
        return variables;
    }

    @Bean
    public NamespaceContextBuilder namespaceContextBuilder() {
        NamespaceContextBuilder namespaceContextBuilder = new NamespaceContextBuilder();
        namespaceContextBuilder.setNamespaceMappings(Collections.singletonMap("fbs", "http://www.consol.com/schemas/flightbooking"));
        return namespaceContextBuilder;
    }

    @Bean
    public ConnectionFactory connectionFactory() {
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(jmsBrokerUrl);
        connectionFactory.setWatchTopicAdvisories(false);
        return connectionFactory;
    }

    @Bean
    public HttpServer royalAirlineServer() {
        return CitrusEndpoints.http()
                .server()
                .autoStart(true)
                .port(8074)
                .timeout(5000)
                .build();
    }

    @Bean
    public JmsEndpoint travelAgencyBookingRequestEndpoint() {
        return CitrusEndpoints.jms()
                .asynchronous()
                .destination(travelAgencyRequestQueue)
                .connectionFactory(connectionFactory())
                .build();
    }

    @Bean
    public JmsEndpoint travelAgencyBookingResponseEndpoint() {
        return CitrusEndpoints.jms()
                .asynchronous()
                .destination(travelAgencyResponseQueue)
                .connectionFactory(connectionFactory())
                .build();
    }

    @Bean
    public JmsEndpoint smartAirlineBookingRequestEndpoint() {
        return CitrusEndpoints.jms()
                .asynchronous()
                .destination(smartAirlineBookingRequestEndpoint)
                .connectionFactory(connectionFactory())
                .build();
    }

    @Bean
    public JmsEndpoint smartAirlineBookingResponseEndpoint() {
        return CitrusEndpoints.jms()
                .asynchronous()
                .destination(smartAirlineBookingResponseEndpoint)
                .connectionFactory(connectionFactory())
                .build();
    }

    @Bean(destroyMethod = "close")
    public BasicDataSource dataSource() {
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName(jdbcDiver);
        dataSource.setUrl(jdbcUrl);
        dataSource.setUsername(dbUser);
        dataSource.setPassword(dbPassword);
        dataSource.setInitialSize(1);
        dataSource.setMaxActive(5);
        dataSource.setMaxIdle(2);
        return dataSource;
    }

    @Bean
    public SequenceBeforeTest beforeTest() {
        return new TestDesignerBeforeTestSupport() {
            @Override
            public void beforeTest(TestDesigner testDesigner) {
                testDesigner.purgeQueues()
                    .connectionFactory(connectionFactory())
                    .queue(travelAgencyRequestQueue)
                    .queue(travelAgencyResponseQueue)
                    .queue(smartAirlineBookingRequestEndpoint)
                    .queue(smartAirlineBookingResponseEndpoint);
            }
        };
    }
}
