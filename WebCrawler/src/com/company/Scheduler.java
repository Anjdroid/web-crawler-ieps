package com.company;


import java.util.*;

public class Scheduler {

    // frontier
    private static Queue<String> frontier;

    //URLs already visited
    private static Set<Integer> visited;

    HashMap<String, ArrayList<String>> allow = new HashMap<>();
    HashMap<String, ArrayList<String>> disallow = new HashMap<>();
    HashMap<String, Integer> crawlDelay = new HashMap<>();
    HashMap<String, String> parentChild = new HashMap<>();
    //HashMap<String, ArrayList<String>> sitemap = new HashMap<>();


    public Scheduler() {
        frontier = new LinkedList<>();
        visited = new HashSet<>();
        frontier.add("podatki.gov.si");
        frontier.add("evem.gov.si");
        frontier.add("e-uprava.gov.si");
        frontier.add("e-prostor.gov.si");
        frontier.add("stopbirokraciji.gov.si");
        frontier.add("mnz.gov.si");
        frontier.add("mddsz.gov.si");
        frontier.add("mop.gov.si");
        frontier.add("mzi.gov.si");
    }

    public  HashMap<String, ArrayList<String>>  getAllowed() {
        return allow;
    }

    public  HashMap<String, String>  getParentChild() {
        return parentChild;
    }

    public  HashMap<String, ArrayList<String>>  getDissallowed() {
        return disallow;
    }

    public  HashMap<String,Integer>  getCrawlDelay() {
        return crawlDelay;
    }

    public Queue<String> getFrontier() {
        return frontier;
    }

    public Set<Integer> getVisited() {
        return visited;
    }

    public static boolean isDuplicate(String page) {
        // check if URL in visited pages or in frontier
        return !visited.contains(page.hashCode()) && !frontier.contains(page);
    }

}

