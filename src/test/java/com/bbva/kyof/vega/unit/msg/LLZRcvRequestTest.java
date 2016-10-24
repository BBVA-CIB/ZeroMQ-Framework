package com.bbva.kyof.vega.unit.msg;

import static org.junit.Assert.fail;

/**
 * Test the LLZRcvRequest class
 */
public class LLZRcvRequestTest
{
//    private static final String CONFIG_FILE = "llzManagerTestConfig.xml";
//    private static final String INSTANCE_NAME = "managerTestConfig";
//    private static LLZManagerParams MANAGER_PARAMS;
//    private static String MANAGER_CONFIG_FILE;
//    private ILLZManager manager = null;
//    private UUID sessionId;
//    private UUID requestId;
//    private LLZMsgHeader header;
//    private LLZAutodiscoveryHazelcastManager autodiscovery = new LLZAutodiscoveryHazelcastManager();
//
//    
//    
//    @org.junit.BeforeClass
//    public static void init()
//    {
//        MANAGER_CONFIG_FILE = ReqRespTest.class.getClassLoader().getResource(CONFIG_FILE).getPath();
//        MANAGER_PARAMS = new LLZManagerParams.Builder(INSTANCE_NAME, MANAGER_CONFIG_FILE).build();
//    }
//
//    
//    @Before
//    public void beforeTest()
//    {
//        
//        try
//        {
//            this.manager = LLZManager.createInstance(MANAGER_PARAMS);
//        }
//        catch (final LLZException e)
//        {
//            fail("Error creating the LLZManager Instance in the LLZManager test initialization. Error ["+e.getMessage()+"]");
//        }
//        
//        
//        this.sessionId = UUID.randomUUID();
//        this.requestId = UUID.randomUUID();
//
//        try
//        {
//            Thread.sleep(19000);
//        }
//        catch (InterruptedException e)
//        {
//        }
//        
//        try
//        {
//            this.header = new LLZMsgHeader(LLZMsgType.DATA_REQ, autodiscovery.createUniqueId(), autodiscovery.createUniqueId(), "2.0");
//        }
//        catch (LLZException e)
//        {
//            e.printStackTrace();
//        }
//        this.header.setRequestId(requestId);
//    }   
//
//    @After
//    public void afterTest() throws LLZException {
//        autodiscovery.stop();
//    }
//
//    @Test
//    public void testGettersSetters() throws LLZException
//    {
//        // Create the contents
//        final ByteBuffer msgContents = ByteBuffer.allocate(128);
//        final LLZInstanceContext instanceContext = new LLZInstanceContext(MANAGER_PARAMS);
//
//        // Create the required mocks
//        final ZFrame responseAddress = EasyMock.createMock(ZFrame.class);
//        
////    FIXME    final LLZResponder requestResponder = EasyMock.createMock(LLZResponder.class);
//        final LLZResponder requestResponder = new LLZResponder(instanceContext, "default", "respConnection"); 
//
//        
//        // Create the message
//        final LLZRcvRequest requestMsg = new LLZRcvRequest(this.header, msgContents, responseAddress, requestResponder, instanceContext);
//  
//        
//        Assert.assertEquals(LLZMsgType.DATA_REQ, requestMsg.getMessageType());
//        Assert.assertEquals(this.sessionId, requestMsg.getSessionId());
//        Assert.assertEquals("TopicTest", requestMsg.getTopicName());
//        Assert.assertEquals(11111, requestMsg.getInstanceId());
//        Assert.assertEquals(msgContents, requestMsg.getMessageContent());
//        Assert.assertEquals(this.requestId, requestMsg.getRequestId());
//        Assert.assertEquals(responseAddress, requestMsg.getResponseAddress());
//
//        requestMsg.sendResponse(ByteBuffer.allocate(128));
//    }
}