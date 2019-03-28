package com.company;


import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public class Scheduler {

    // frontier
    private static Queue<String> frontier;

    //URLs already visited
    private static Set<Integer> visited;

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

    public Queue<String> getFrontier() {
        return frontier;
    }

    public Set<Integer> getVisited() {
        return visited;
    }

    public static boolean isDuplicate(String page) {
        // check if URL in visited pages or in frontier
        if (!visited.contains(page.hashCode()) && !frontier.contains(page)) {
            return false;
        }
        return true;
    }

}
