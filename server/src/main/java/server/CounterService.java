package server;

public class CounterService {

    private int count = 0;

    public int getAndIncrease() {
        return count++;
    }
}

