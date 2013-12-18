package org.safehaus.kiskis.mgmt.server.communication;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.safehaus.kiskis.mgmt.shared.protocol.Command;
import org.safehaus.kiskis.mgmt.shared.protocol.CommandJson;
import org.safehaus.kiskis.mgmt.shared.protocol.api.ResponseListener;
import org.safehaus.kiskis.mgmt.shared.protocol.api.CommandTransportInterface;
import javax.jms.*;
import org.apache.activemq.broker.region.policy.AbortSlowAckConsumerStrategy;
import org.apache.activemq.broker.region.policy.DeadLetterStrategy;
import org.apache.activemq.broker.region.policy.PolicyEntry;
import org.apache.activemq.broker.region.policy.PolicyMap;
import org.apache.activemq.broker.region.policy.SharedDeadLetterStrategy;
import org.apache.activemq.pool.PooledConnectionFactory;
//check branch

public class CommandTransport implements CommandTransportInterface {

    private static final Logger LOG = Logger.getLogger(CommandTransport.class.getName());
    private BrokerService broker;
    private PooledConnectionFactory pooledConnectionFactory;
    private CommunicationMessageListener communicationMessageListener;
    private ExecutorService exec;
    private String amqBindAddress;
    private String amqServiceQueue;
    private String amqBrokerCertificateName;
    private String amqBrokerTrustStoreName;
    private String amqBrokerCertificatePwd;
    private String amqBrokerTrustStorePwd;
    private int amqPort;
    private int amqMaxMessageToAgentTtlSec;
    private int amqMaxOfflineAgentQueueTtlSec;
    private int amqMaxSlowAgentConnectionTtlSec;
    private int amqMaxPooledConnections;
    private int amqMaxSenderPoolSize;

    public void setAmqPort(int amqPort) {
        this.amqPort = amqPort;
    }

    public void setAmqBindAddress(String amqBindAddress) {
        this.amqBindAddress = amqBindAddress;
    }

    public void setAmqServiceQueue(String amqServiceQueue) {
        this.amqServiceQueue = amqServiceQueue;
    }

    public void setAmqBrokerCertificateName(String amqBrokerCertificateName) {
        this.amqBrokerCertificateName = amqBrokerCertificateName;
    }

    public void setAmqBrokerTrustStoreName(String amqBrokerTrustStoreName) {
        this.amqBrokerTrustStoreName = amqBrokerTrustStoreName;
    }

    public void setAmqBrokerCertificatePwd(String amqBrokerCertificatePwd) {
        this.amqBrokerCertificatePwd = amqBrokerCertificatePwd;
    }

    public void setAmqBrokerTrustStorePwd(String amqBrokerTrustStorePwd) {
        this.amqBrokerTrustStorePwd = amqBrokerTrustStorePwd;
    }

    public void setAmqMaxMessageToAgentTtlSec(int amqMaxMessageToAgentTtlSec) {
        this.amqMaxMessageToAgentTtlSec = amqMaxMessageToAgentTtlSec;
    }

    public void setAmqMaxOfflineAgentQueueTtlSec(int amqMaxOfflineAgentQueueTtlSec) {
        this.amqMaxOfflineAgentQueueTtlSec = amqMaxOfflineAgentQueueTtlSec;
    }

    public void setAmqMaxSlowAgentConnectionTtlSec(int amqMaxSlowAgentConnectionTtlSec) {
        this.amqMaxSlowAgentConnectionTtlSec = amqMaxSlowAgentConnectionTtlSec;
    }

    public void setAmqMaxPooledConnections(int amqMaxPooledConnections) {
        this.amqMaxPooledConnections = amqMaxPooledConnections;
    }

    public void setAmqMaxSenderPoolSize(int amqMaxSenderPoolSize) {
        this.amqMaxSenderPoolSize = amqMaxSenderPoolSize;
    }

    @Override
    public void sendCommand(Command command) {
        exec.submit(new CommandProducer(command));
    }

    public class CommandProducer implements Runnable {

        Command command;

        public CommandProducer(Command command) {
            this.command = command;
        }

        public void run() {
            Connection connection = null;
            Session session = null;
            MessageProducer producer = null;
            try {
                connection = pooledConnectionFactory.createConnection();
                connection.start();
                session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                Destination destination = session.createQueue(command.getCommand().getUuid().toString());
                producer = session.createProducer(destination);
                producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
                producer.setTimeToLive(amqMaxMessageToAgentTtlSec * 1000);
                String json = CommandJson.getJson(command);
                //LOG.info("Sending: " + json);
                System.out.println("\nSending: " + json);
                TextMessage message = session.createTextMessage(json);
                producer.send(message);
            } catch (JMSException ex) {
                LOG.log(Level.SEVERE, "Error in CommandProducer.run", ex);
            } finally {
                if (producer != null) {
                    try {
                        producer.close();
                    } catch (Exception e) {
                    }
                }
                if (session != null) {
                    try {
                        session.close();
                    } catch (Exception e) {
                    }
                }
                if (connection != null) {
                    try {
                        connection.close();
                    } catch (Exception e) {
                    }
                }
            }
        }
    }

    public void init() {

        if (pooledConnectionFactory != null) {
            try {
                pooledConnectionFactory.stop();
            } catch (Exception e) {
            }
        }
        if (broker != null) {
            try {
                broker.stop();
                broker.waitUntilStopped();
            } catch (Exception e) {
            }
        }

        try {
            System.setProperty("javax.net.ssl.keyStore", System.getProperty("karaf.base") + "/" + this.amqBrokerCertificateName);
            System.setProperty("javax.net.ssl.keyStorePassword", this.amqBrokerCertificatePwd);
            System.setProperty("javax.net.ssl.trustStore", System.getProperty("karaf.base") + "/" + this.amqBrokerTrustStoreName);
            System.setProperty("javax.net.ssl.trustStorePassword", this.amqBrokerTrustStorePwd);

            broker = new BrokerService();
            //***policy
            PolicyMap policy = new PolicyMap();
            PolicyEntry pentry = new PolicyEntry();
            //abort consumers not acking message within this period of time
            AbortSlowAckConsumerStrategy slowConsumerStrategy = new AbortSlowAckConsumerStrategy();
            slowConsumerStrategy.setMaxTimeSinceLastAck(amqMaxSlowAgentConnectionTtlSec * 1000);
            pentry.setSlowConsumerStrategy(slowConsumerStrategy);
            //drop expired messages instead of sending to DLQ
            DeadLetterStrategy deadLetterStrategy = new SharedDeadLetterStrategy();
            deadLetterStrategy.setProcessExpired(false);
            pentry.setDeadLetterStrategy(deadLetterStrategy);
            policy.setDefaultEntry(pentry);
            //drop queues whose clients are offline for this amount of time
            broker.setOfflineDurableSubscriberTimeout(amqMaxOfflineAgentQueueTtlSec * 1000);
            broker.setDestinationPolicy(policy);
            //***policy
            broker.setPersistent(true);
            broker.setUseJmx(false);
            broker.addConnector("ssl://" + this.amqBindAddress + ":" + this.amqPort + "?needClientAuth=true");
            broker.start();
            broker.waitUntilStarted();
            //executor service setup
            exec = Executors.newFixedThreadPool(amqMaxSenderPoolSize);
            //pooled connection factory setup
            ActiveMQConnectionFactory amqFactory = new ActiveMQConnectionFactory("vm://localhost?create=false");
            amqFactory.setCheckForDuplicates(true);
            pooledConnectionFactory = new PooledConnectionFactory(amqFactory);
            pooledConnectionFactory.setMaxConnections(amqMaxPooledConnections);
            pooledConnectionFactory.start();
            setupListener();
            System.out.println("ActiveMQ started...");
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Error in init", ex);
        }

    }

    public void destroy() {
        try {
            if (pooledConnectionFactory != null) {
                try {
                    pooledConnectionFactory.stop();
                } catch (Exception e) {
                }
            }
            if (broker != null) {
                try {
                    broker.stop();
                } catch (Exception e) {
                }
            }
            if (communicationMessageListener != null) {
                try {
                    communicationMessageListener.destroy();
                } catch (Exception e) {
                }
            }
            System.out.println("ActiveMQ stopped...");
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Error in destroy", ex);
        }
    }

    private void setupListener() {
        try {
            Connection connection = pooledConnectionFactory.createConnection();
            //don not close this connection to not return it to the pool so no consumers are closed
            connection.start();
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Destination adminQueue = session.createQueue(this.amqServiceQueue);
            MessageConsumer consumer = session.createConsumer(adminQueue);
            communicationMessageListener = new CommunicationMessageListener();
            consumer.setMessageListener(communicationMessageListener);
        } catch (JMSException ex) {
            LOG.log(Level.SEVERE, "Error in setupListener", ex);
        }
    }

    @Override
    public void addListener(ResponseListener listener) {
        try {
            communicationMessageListener.addListener(listener);
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Error in addListener", ex);
        }
    }

    @Override
    public void removeListener(ResponseListener listener) {
        try {
            communicationMessageListener.removeListener(listener);
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Error in removeListener", ex);
        }
    }
}
