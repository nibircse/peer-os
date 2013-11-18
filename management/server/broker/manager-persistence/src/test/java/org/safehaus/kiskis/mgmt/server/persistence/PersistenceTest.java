/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.safehaus.kiskis.mgmt.server.persistence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.safehaus.kiskis.mgmt.shared.protocol.Agent;
import org.safehaus.kiskis.mgmt.shared.protocol.Command;
import org.safehaus.kiskis.mgmt.shared.protocol.OutputRedirection;
import org.safehaus.kiskis.mgmt.shared.protocol.Request;
import org.safehaus.kiskis.mgmt.shared.protocol.Response;
import org.safehaus.kiskis.mgmt.shared.protocol.enums.RequestType;
import org.safehaus.kiskis.mgmt.shared.protocol.enums.ResponseType;

/**
 *
 * @author bahadyr
 */
public class PersistenceTest {

    public PersistenceTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of getAgentList method, of class Persistence.
     */
//    @Test
    public void testGetAgentList() {
        System.out.println("getAgentList");
        Persistence instance = new Persistence();
        List<Agent> result = instance.getAgentList();
        Agent agent = new Agent();
        agent.setUuid("uuid");
        assertEquals(true, result.contains(agent));
        // TODO review the generated test code and remove the default call to fail.
    }

    /**
     * Test of saveAgent method, of class Persistence.
     */
//    @Test
    public void testSaveAgent() {
        System.out.println("saveAgent");
        Agent agent = new Agent();
        agent.setHostname("hostname");
        agent.setLXC(true);
        List<String> listip = new ArrayList<String>();
        listip.add("0.0.0.0");
        agent.setListIP(listip);
        agent.setMacAddress("mac");
        agent.setUuid("uuid");
        Persistence instance = new Persistence();
        boolean expResult = true;
        boolean result = instance.saveAgent(agent);
        assertEquals(expResult, result);

        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
    }

    /**
     * Test of init method, of class Persistence.
     */
//    @Test
    public void testInit() {
        System.out.println("init");
        Persistence instance = new Persistence();
        instance.init();
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of destroy method, of class Persistence.
     */
//    @Test
    public void testDestroy() {
        System.out.println("destroy");
        Persistence instance = new Persistence();
        instance.destroy();
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getCommandList method, of class Persistence.
     */
//    @Test
    public void testGetCommandList() {
        System.out.println("getCommandList");
        Agent agent = null;
        Persistence instance = new Persistence();
        List<Command> expResult = null;
        List<Command> result = instance.getCommandList(agent);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of saveCommand method, of class Persistence.
     */
//    @Test
    public void testSaveCommand() {
        System.out.println("saveCommand");
        Request request = new Request();
        request.setType(RequestType.REGISTRATION_REQUEST_DONE);
//        request.setSource("source");
//        List<String> args = new ArrayList<String>();
//        args.add("arg1");
//        args.add("arg2");
//        request.setArgs(args);
//        Map<String, String> env = new HashMap<String, String>();
//        env.put("envkey", "envvalue");
//        request.setEnvironment(env);
//        request.setEnvironment(null);
//        request.setPid(Integer.SIZE);
//        request.setProgram("program");
//        request.setRequestSequenceNumber(Long.MIN_VALUE);
//        request.setRunAs("runas");
//        request.setSource("source");
        request.setStdErr(OutputRedirection.RETURN);
//        request.setStdErrPath("path");
        request.setStdOut(OutputRedirection.NO);
//        request.setStdOutPath("path");
//        request.setTimeout(Long.MIN_VALUE);
//        request.setUuid("uuid");
//        request.setWorkingDirectory("/working/directory");
        Command command = new Command(request);
        Persistence instance = new Persistence();
        boolean expResult = true;
        boolean result = instance.saveCommand(command);
        assertEquals(expResult, result);
    }

    /**
     * Test of saveResponse method, of class Persistence.
     */
//    @Test
    public void testSaveResponse() {
        System.out.println("saveResponse");
        Response response = new Response();
        response.setUuid("uuid");
        response.setStdOut("stdout");
        response.setResponseSequenceNumber(Long.MIN_VALUE);
        response.setExitCode(Integer.SIZE);
        response.setStdErr("errout");
        List<String> listip = new ArrayList<String>();
        listip.add("0.0.0.0");
        response.setIps(listip);
        response.setMacAddress("macaddress");
        response.setPid(Integer.SIZE);
        response.setRequestSequenceNumber(Long.MIN_VALUE);
        response.setType(ResponseType.EXECUTE_RESPONSE);
        response.setSource("source");
        Persistence instance = new Persistence();
        boolean expResult = true;
        boolean result = instance.saveResponse(response);
        assertEquals(expResult, result);
    }

}