package main.util;

import main.graph.Tuple;

import java.util.ArrayList;
import java.util.List;

public class Timer {

    private long startTime;
    private List<Tuple<String, Long>> times = new ArrayList<>();

    public Timer(){
        startTime = System.nanoTime();
        times.add(new Tuple<>("started", startTime));
    }

    public void time(String message){
        long t = System.nanoTime();
        System.out.println(String.format("TIMER:: %s since start: %s since previous: %s", message, (t-startTime)*10E-9, (t-times.get(times.size()-1).getT2())*10E-9));
        times.add(new Tuple<>(message, t));
    }



}
