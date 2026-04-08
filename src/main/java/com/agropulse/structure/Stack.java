package com.agropulse.structure;

public class Stack<T> {
    private LinkedList<T> list = new LinkedList<>();
    
    public void push(T item) {
        list.add(item);
    }
    
    public T pop() {
        if (list.isEmpty()) return null;
        T item = list.get(list.size() - 1);
        return item;
    }
    
    public T peek() {
        if (list.isEmpty()) return null;
        return list.get(list.size() - 1);
    }
    
    public boolean isEmpty() {
        return list.isEmpty();
    }
    
    public int size() {
        return list.size();
    }
}
