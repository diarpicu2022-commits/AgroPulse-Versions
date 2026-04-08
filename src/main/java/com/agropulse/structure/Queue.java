package com.agropulse.structure;

public class Queue<T> {
    private LinkedList<T> list = new LinkedList<>();
    
    public void enqueue(T item) {
        list.add(item);
    }
    
    public T dequeue() {
        if (list.isEmpty()) return null;
        T item = list.get(0);
        return item;
    }
    
    public T front() {
        if (list.isEmpty()) return null;
        return list.get(0);
    }
    
    public boolean isEmpty() {
        return list.isEmpty();
    }
    
    public int size() {
        return list.size();
    }
}
