package com.company;


import java.util.*;

public class Scheduler {

    // frontier
    private static Queue<String> frontier;

    // URLs already visited
    private static Set<String> visited;

    // allowed, disallowed pages from robots.txt & crawl delay for pages
    private static HashMap<String, ArrayList<String>> allow = new HashMap<>();
    private static HashMap<String, ArrayList<String>> disallow = new HashMap<>();
    private static HashMap<String, Integer> crawlDelay = new HashMap<>();

    // parent child relationship for pages
    private static HashMap<String, String> parentChild = new HashMap<>();

    public Scheduler() {
        // initialize frontier and visited pages
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

    public Set<String> getVisited() {
        return visited;
    }

    public static boolean isDuplicate(String page) {
        // check if URL is already in visited pages
        return !visited.contains(page);
    }

}

