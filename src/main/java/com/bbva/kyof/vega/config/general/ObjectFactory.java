//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4-2 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2016.04.27 at 03:43:08 PM CEST 
//


package com.bbva.kyof.vega.config.general;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the com.bbva.kyof.zmq.config.general package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _ZmqConfig_QNAME = new QName("http://www.bbva.com/zeromq/config", "zmq_config");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.bbva.kyof.zmq.config.general
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link GlobalConfiguration }
     * 
     */
    public GlobalConfiguration createGlobalConfiguration() {
        return new GlobalConfiguration();
    }

    /**
     * Create an instance of {@link PubTopicConfig }
     * 
     */
    public PubTopicConfig createPubTopicConfig() {
        return new PubTopicConfig();
    }

    /**
     * Create an instance of {@link ReqSocketSchema }
     * 
     */
    public ReqSocketSchema createReqSocketSchema() {
        return new ReqSocketSchema();
    }

    /**
     * Create an instance of {@link SubTopicConfig }
     * 
     */
    public SubTopicConfig createSubTopicConfig() {
        return new SubTopicConfig();
    }

    /**
     * Create an instance of {@link InstanceConfig }
     * 
     */
    public InstanceConfig createInstanceConfig() {
        return new InstanceConfig();
    }

    /**
     * Create an instance of {@link RespSocketSchema }
     * 
     */
    public RespSocketSchema createRespSocketSchema() {
        return new RespSocketSchema();
    }

    /**
     * Create an instance of {@link SubSocketSchema }
     * 
     */
    public SubSocketSchema createSubSocketSchema() {
        return new SubSocketSchema();
    }

    /**
     * Create an instance of {@link ReqTopicConfig }
     * 
     */
    public ReqTopicConfig createReqTopicConfig() {
        return new ReqTopicConfig();
    }

    /**
     * Create an instance of {@link AutoDiscoveryConfig }
     * 
     */
    public AutoDiscoveryConfig createAutoDiscoveryConfig() {
        return new AutoDiscoveryConfig();
    }

    /**
     * Create an instance of {@link PubSocketSchema }
     * 
     */
    public PubSocketSchema createPubSocketSchema() {
        return new PubSocketSchema();
    }

    /**
     * Create an instance of {@link RespTopicConfig }
     * 
     */
    public RespTopicConfig createRespTopicConfig() {
        return new RespTopicConfig();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GlobalConfiguration }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.bbva.com/zeromq/config", name = "zmq_config")
    public JAXBElement<GlobalConfiguration> createZmqConfig(GlobalConfiguration value) {
        return new JAXBElement<GlobalConfiguration>(_ZmqConfig_QNAME, GlobalConfiguration.class, null, value);
    }

}
