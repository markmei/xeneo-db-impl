package org.xeneo.db;

import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.xeneo.core.activity.*;
import org.xeneo.core.activity.Object;

public class JdbcActivityManager implements ActivityManager {

    private Logger logger = LoggerFactory.getLogger(JdbcActivityManager.class);
    private JdbcTemplate jdbcTemplate;

    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    private static String ADD_ACTIVITY = "insert into Activity (ActivityURI,CreationDate,ActorURI,ActionURI,ObjectURI,TargetURI,Description,Summary, ActivityProviderURI) values (?,?,?,?,?,?,?,?,?)";
    private static String GET_ACTIVITY_BY_URI = "select count(*) from Activity where ActivityURI = ?";
    private static String GET_OBJECT_BY_URI = "select count(*) from Object where ObjectURI = ?";
    private static String UPDATE_OBJECT_BY_URI = "update Object set Name = ?, ObjectTypeURI = ? where ObjectURI = ?";
    private static String ADD_OBJECT = "insert into Object (ObjectURI,Name,ObjectTypeURI) values (?,?,?)";
    private static String ADD_TASK_CONTEXT = "insert into `TaskContext` (ActivityURI,TaskURI,CaseURI) values (?,?,?)";
    private static String REMOVE_TASK_CONTEXT = "delete from `TaskContext` where ActivityURI = ? and TaskURI = ? and CaseURI = ?";
    private static String TASK_CONTEXT_COUNT_QUERY = "select count(*) from `TaskContext` where ActivityURI = ? and TaskURI = ? and CaseURI = ?";
    private static String TASK_CONTEXT_BY_ACTIVITY_AND_CASE = "select * from `TaskContext` where ActivityURI = ? and CaseURI = ?";
    private static String TASK_CONTEXT_BY_ACTIVITY = "select * from `TaskContext` where ActivityURI = ?";
    private static String GET_ACTOR_BY_URI = "select count(*) from Actor where ActorURI = ?";
    private static String UPDATE_ACTOR_BY_URI = "update Actor set ActorName = ?, ActivityProviderURI = ? where ActorURI = ?";
    private static String ADD_ACTOR = "insert into Actor (ActorURI, ActorName, ActivityProviderURI) values(?,?,?)";
    private static String GET_ACTIVITYPROVIDER_BY_URI = "select count(*) from ActivityProvider where ActivityProviderURI = ?";
    private static String UPDATE_ACTIVITYPROVIDER_BY_URI = "update ActivityProvider set ActivityProviderName = ?, ActivityProviderType = ? where ActivityProviderURI = ?";
    private static String ADD_ACTIVITYPROVIDER = "insert into ActivityProvider (ActivityProviderURI, ActivityProviderName, ActivityProviderType) values(?,?,?)";

    protected boolean isExistingObject(String objectURI) {
        if (jdbcTemplate.queryForInt(GET_OBJECT_BY_URI, objectURI) > 0) {
            return true;
        }

        return false;
    }

    protected boolean isExistingActivityProvider(String activityProviderURI) {
        if (jdbcTemplate.queryForInt(GET_ACTIVITYPROVIDER_BY_URI, activityProviderURI) > 0) {
            return true;
        }

        return false;
    }

    protected boolean isExistingActor(String actorURI) {
        if (jdbcTemplate.queryForInt(GET_ACTOR_BY_URI, actorURI) > 0) {
            return true;
        }

        return false;
    }

    public void addActivity(Activity a) {
        Object obj = a.getObject();
        Object tar = a.getTarget();
        Actor acto = a.getActor();
        ActivityProvider ap = a.getActivityProvider();

        // check if object exists otherwise create one
        if (isExistingObject(obj.getObjectURI())) {
            jdbcTemplate.update(UPDATE_OBJECT_BY_URI, obj.getObjectName(), obj.getObjectTypeURI(), obj.getObjectURI());
        } else {
            jdbcTemplate.update(ADD_OBJECT, obj.getObjectURI(), obj.getObjectName(), obj.getObjectTypeURI());
        }

        // check if target exists otherwise create one
        if (isExistingObject(tar.getObjectURI())) {
            jdbcTemplate.update(UPDATE_OBJECT_BY_URI, tar.getObjectName(), tar.getObjectTypeURI(), tar.getObjectURI());
        } else {
            jdbcTemplate.update(ADD_OBJECT, tar.getObjectURI(), tar.getObjectName(), tar.getObjectTypeURI());
        }

        // check if ActivityProvider exists otherwise create one
        if (isExistingActivityProvider(ap.getActivityProviderURI())) {
            jdbcTemplate.update(UPDATE_ACTIVITYPROVIDER_BY_URI, ap.getActivityProviderName(), ap.getActivityProviderType(), ap.getActivityProviderURI());
        } else {
            jdbcTemplate.update(ADD_ACTIVITYPROVIDER, ap.getActivityProviderURI(), ap.getActivityProviderName(), ap.getActivityProviderType());
        }

        // check if actor exists otherwise create one
        if (isExistingActor(acto.getActorURI())) {
            jdbcTemplate.update(UPDATE_ACTOR_BY_URI, acto.getActorName(), acto.getActivityProviderURI(), acto.getActorURI());
        } else {
            jdbcTemplate.update(ADD_ACTOR, acto.getActorURI(), acto.getActorName(), acto.getActivityProviderURI());
        }

        jdbcTemplate.update(ADD_ACTIVITY, a.getActivityURI(), a.getCreationDate(), acto.getActorURI(), a.getActionURI(), obj.getObjectURI(), tar.getObjectURI(), a.getDescription(), a.getSummary(), ap.getActivityProviderURI());

    }

    public void addActivity(Activity activity, Map<String, Collection<String>> caseTaskURIs) {
        this.addActivity(activity);
        String activityURI = activity.getActivityURI();
        this.addTaskContexts(activityURI, caseTaskURIs);
    }

    public void addActivity(Activity activity, String caseURI, Collection<String> taskURIs) {
        this.addActivity(activity);
        String activityURI = activity.getActivityURI();
        this.addTaskContexts(activityURI, caseURI, taskURIs);
    }

    public void addTaskContexts(String activityURI, String caseURI, Collection<String> taskURIs) {
        Iterator<String> it = taskURIs.iterator();
        String taskURI;
        while (it.hasNext()) {
            taskURI = it.next();
            if (jdbcTemplate.queryForInt(TASK_CONTEXT_COUNT_QUERY, activityURI, taskURI, caseURI) < 1) {
                logger.info("Add following Task Context: " + activityURI + ", " + taskURI + ", " + caseURI);
                jdbcTemplate.update(ADD_TASK_CONTEXT, activityURI, taskURI, caseURI);
            }
        }
    }

    public void addTaskContexts(String activityURI, Map<String, Collection<String>> caseTaskURIs) {
        Iterator<String> it1 = caseTaskURIs.keySet().iterator();
        String caseURI, taskURI;
        while (it1.hasNext()) {
            caseURI = it1.next();
            Iterator<String> it2 = caseTaskURIs.get(caseURI).iterator();
            while (it2.hasNext()) {
                taskURI = it2.next();
                if (jdbcTemplate.queryForInt(TASK_CONTEXT_COUNT_QUERY, activityURI, taskURI, caseURI) < 1) {
                    logger.info("Add following Task Context: " + activityURI + ", " + taskURI + ", " + caseURI);
                    jdbcTemplate.update(ADD_TASK_CONTEXT, activityURI, taskURI, caseURI);
                }
            }
        }
    }

    public void removeTaskContexts(String activityURI, String caseURI, Collection<String> taskURIs) {
        Iterator<String> it = taskURIs.iterator();
        while (it.hasNext()) {
            jdbcTemplate.update(REMOVE_TASK_CONTEXT, activityURI, it.next(), caseURI);
        }
    }

    public void removeTaskContexts(String activityURI, Map<String, Collection<String>> caseTaskURIs) {
        Iterator<String> it1 = caseTaskURIs.keySet().iterator();
        while (it1.hasNext()) {
            String caseURI = it1.next();
            Iterator<String> it2 = caseTaskURIs.get(caseURI).iterator();

            while (it2.hasNext()) {
                jdbcTemplate.update(REMOVE_TASK_CONTEXT, activityURI, it2.next(), caseURI);
            }
        }
    }

    public Map<String, Collection<String>> getTaskContexts(String activityURI) {
        Map<String, Collection<String>> response = new HashMap<String, Collection<String>>();

        List<Map<String, java.lang.Object>> result = jdbcTemplate.queryForList(TASK_CONTEXT_BY_ACTIVITY, activityURI);
        if (!result.isEmpty()) {
            Iterator<Map<String, java.lang.Object>> it = result.iterator();
            Map<String, java.lang.Object> map;
            String caseURI, taskURI;
            Collection<String> tasks;
            while (it.hasNext()) {
                map = it.next();
                if (map.containsKey("CaseURI") && map.containsKey("TaskURI")) {
                    caseURI = (String) map.get("CaseURI");
                    taskURI = (String) map.get("TaskURI");
                    if (response.containsKey(caseURI)) {
                        tasks = response.get(caseURI);
                        if (!tasks.contains(taskURI)) {
                            tasks.add(taskURI);
                            response.put(caseURI, tasks);
                        }
                    } else {
                        tasks = new ArrayList<String>();
                        tasks.add(taskURI);
                        response.put(caseURI, tasks);
                    }
                }
            }
        }

        return response;
    }

    /*
     * Maybe this could be used in the future...
     *
     * public Collection<String> getTaskContextsByCaseURI(String caseURI) {
     * Collection<String> tasks = new ArrayList<String>(); List<Map<String,
     * Object>> result =
     * getJdbcTemplate().queryForList(TASK_CONTEXT_BY_ACTIVITY_AND_CASE,
     * activityURI, caseURI); if (!result.isEmpty()) { Iterator<Map<String,
     * Object>> it = result.iterator(); Map<String, Object> map; while
     * (it.hasNext()) { map = it.next(); if (map.containsKey("TaskURI")) {
     * tasks.add((String) map.get("TaskURI")); } } }
     *
     * return tasks; }
     */
    public boolean isExistingActivity(String activityURI) {
        if (jdbcTemplate.queryForInt(GET_ACTIVITY_BY_URI, activityURI) > 0) {
            return true;
        }

        return false;
    }
}
