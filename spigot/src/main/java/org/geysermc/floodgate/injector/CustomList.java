package org.geysermc.floodgate.injector;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

@AllArgsConstructor
public abstract class CustomList implements List {
    @Getter private final List originalList;

    public abstract void onAdd(Object o);

    @Override
    public synchronized int size() {
        return originalList.size();
    }

    @Override
    public synchronized boolean isEmpty() {
        return originalList.isEmpty();
    }

    @Override
    public synchronized boolean contains(Object o) {
        return originalList.contains(o);
    }

    @Override
    public synchronized Iterator iterator() {
        return listIterator();
    }

    @Override
    public synchronized Object[] toArray() {
        return originalList.toArray();
    }

    @Override
    public synchronized Object[] toArray(Object[] a) {
        return originalList.toArray(a);
    }

    @Override
    public boolean add(Object o) {
        onAdd(o);
        synchronized (this) {
            return originalList.add(o);
        }
    }

    @Override
    public synchronized boolean remove(Object o) {
        return originalList.remove(o);
    }

    @Override
    public synchronized boolean containsAll(Collection c) {
        return originalList.containsAll(c);
    }

    @Override
    public boolean addAll(Collection c) {
        for (Object o : c) {
            onAdd(o);
        }
        synchronized (this) {
            return originalList.addAll(c);
        }
    }

    @Override
    public boolean addAll(int index, Collection c) {
        for (Object o : c) {
            onAdd(o);
        }
        synchronized (this) {
            return originalList.addAll(index, c);
        }
    }

    @Override
    public synchronized boolean removeAll(Collection c) {
        return originalList.removeAll(c);
    }

    @Override
    public synchronized boolean retainAll(Collection c) {
        return originalList.retainAll(c);
    }

    @Override
    public synchronized void clear() {
        originalList.clear();
    }

    @Override
    public synchronized Object get(int index) {
        return originalList.get(index);
    }

    @Override
    public synchronized Object set(int index, Object element) {
        return originalList.set(index, element);
    }

    @Override
    public synchronized void add(int index, Object element) {
        originalList.add(index, element);
    }

    @Override
    public synchronized Object remove(int index) {
        return originalList.remove(index);
    }

    @Override
    public synchronized int indexOf(Object o) {
        return originalList.indexOf(o);
    }

    @Override
    public synchronized int lastIndexOf(Object o) {
        return originalList.lastIndexOf(o);
    }

    @Override
    public synchronized ListIterator listIterator() {
        return originalList.listIterator();
    }

    @Override
    public synchronized ListIterator listIterator(int index) {
        return originalList.listIterator(index);
    }

    @Override
    public synchronized List subList(int fromIndex, int toIndex) {
        return originalList.subList(fromIndex, toIndex);
    }
}
