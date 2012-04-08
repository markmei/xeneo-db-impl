/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.xeneo.db;

import org.xeneo.db.JdbcCaseEngine;
import java.util.HashMap;
import java.util.Map;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import org.xeneo.core.activity.OldActivity;
import org.xeneo.core.task.Case;
import org.xeneo.core.task.CaseEngine;
import org.xeneo.core.task.CaseType;
import org.xeneo.core.task.Task;

import java.util.Calendar;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 *
 * @author Stefan Huber
 */

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations="/test-config.xml")
public class JdbcActivityTest {
    Logger logger = Logger.getLogger(JdbcActivityTest.class);
        
    @Autowired
    private JdbcCaseEngine session;
    
    public JdbcActivityTest() {}

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    @Test
    public void testActivityStory() {       
        CaseType ct = session.createCaseType("Some Case Type", "Case Type Description");
        Case cs1= session.createCase(ct.getCaseTypeURI(), "A Case", "My Case Description");
        Case cs2 = session.createCase(ct.getCaseTypeURI(), "B Case", "My 2. Case Description");
        
        Task t1 = session.createTask("Task 1", "Task 1 Description");
        Task t2 = session.createTask("Task 2", "Task 2 Description");
        Task t3 = session.createTask("Task 3", "Task 3 Description");
        Task t4 = session.createTask("Task 4", "Task 4 Description");
        Task t5 = session.createTask("Task 5", "Task 5 Description");
        
        List<String> taskList1 = new ArrayList<String>();
        taskList1.add(t1.getTaskURI());
        taskList1.add(t2.getTaskURI());
        taskList1.add(t3.getTaskURI());        
        taskList1.add(t5.getTaskURI());
        
        List<String> taskList2 = new ArrayList<String>();
        taskList2.add(t1.getTaskURI());
        taskList2.add(t5.getTaskURI());
        taskList2.add(t4.getTaskURI());
        
        Map<String,Collection<String>> map = new HashMap<String,Collection<String>>();
        map.put(cs1.getCaseURI(), taskList1);
        map.put(cs2.getCaseURI(), taskList2);       
               
        OldActivity a1 = session.createActivity("my activity", "http://stefanhuber.at/user/stefan", Calendar.getInstance().getTime(), map);
        
        taskList1.remove(t3.getTaskURI());
        assertTrue(taskList1.size() == 3);        
        
        a1.removeTaskContexts(cs1.getCaseURI(), taskList1);
                       
        Collection<String> tC1 = a1.getTaskContextsByCaseURI(cs1.getCaseURI());
        Collection<String> tC2 = a1.getTaskContextsByCaseURI(cs2.getCaseURI());
        assertTrue(tC1.size() == 1 && tC2.size() == 3);
                
        Collection<String> cont1 = a1.getTaskContextsByCaseURI("http://stefanhuber.at/blub/ahahah");        
        assertTrue(cont1.isEmpty());
        
        // add TaskContexts which already exist
        a1.addTaskContexts(cs2.getCaseURI(), tC2);
        assertTrue(a1.getTaskContextsByCaseURI(cs2.getCaseURI()).size() == 3);
        a1.addTaskContexts(cs2.getCaseURI(), tC2);
        assertTrue(a1.getTaskContextsByCaseURI(cs2.getCaseURI()).size() == 3);
                
        a1.addTaskContexts(map);
        assertTrue(a1.getTaskContexts().size() == map.size());
        a1.addTaskContexts(map);
        assertTrue(a1.getTaskContexts().size() == map.size());
        
        // delete TaskContexts which doesn't exist
        Collection<String> dummyList = new ArrayList<String>();
        dummyList.add("hallo");
        dummyList.add("blab");
        
        a1.removeTaskContexts("blub", dummyList);
        assertTrue(a1.getTaskContexts().size() == map.size());
        
        Map<String,Collection<String>> dummyMap = new HashMap<String,Collection<String>>();
        dummyMap.put("puhh", dummyList);
        dummyMap.put("pähh", dummyList);
        
        a1.removeTaskContexts(dummyMap);
        assertTrue(a1.getTaskContexts().size() == map.size());
        
        
       // TODO: really test remove and get counts okay and data okay, add something remove and get with all different variants (in depht)
        
    }
}