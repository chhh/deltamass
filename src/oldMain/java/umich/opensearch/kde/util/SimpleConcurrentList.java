package umich.opensearch.kde.util;

import java.util.ArrayList;
import java.util.Collection;

/**
 * A kludge with all modification methods being synchronized on the list itself.
 *
 * @author Dmitry Avtonomov
 */
public class SimpleConcurrentList<E> extends ArrayList<E> {

  public SimpleConcurrentList(int initialCapacity) {
    super(initialCapacity);
  }

  public SimpleConcurrentList() {
  }

  public SimpleConcurrentList(Collection<? extends E> c) {
    super(c);
  }

  @Override
  public synchronized boolean isEmpty() {
    return super.isEmpty();
  }

  @Override
  public synchronized int size() {
    return super.size();
  }

  @Override
  public synchronized void ensureCapacity(int minCapacity) {
    super.ensureCapacity(minCapacity);
  }

  @Override
  public synchronized E get(int index) {
    return super.get(index);
  }

  @Override
  public synchronized E set(int index, E element) {
    return super.set(index, element);
  }

  @Override
  public synchronized boolean add(E e) {
    return super.add(e);
  }

  @Override
  public synchronized void add(int index, E element) {
    super.add(index, element);
  }

  @Override
  public synchronized E remove(int index) {
    return super.remove(index);
  }

  @Override
  public synchronized boolean remove(Object o) {
    return super.remove(o);
  }

  @Override
  public synchronized void clear() {
    super.clear();
  }

  @Override
  public synchronized boolean addAll(Collection<? extends E> c) {
    return super.addAll(c);
  }

  @Override
  public synchronized boolean addAll(int index, Collection<? extends E> c) {
    return super.addAll(index, c);
  }

  @Override
  protected void removeRange(int fromIndex, int toIndex) {
    super.removeRange(fromIndex, toIndex);
  }

  @Override
  public synchronized boolean removeAll(Collection<?> c) {
    return super.removeAll(c);
  }

  @Override
  public synchronized boolean retainAll(Collection<?> c) {
    return super.retainAll(c);
  }

  @Override
  public synchronized boolean contains(Object o) {
    return super.contains(o);
  }

  @Override
  public synchronized int indexOf(Object o) {
    return super.indexOf(o);
  }

  @Override
  public synchronized int lastIndexOf(Object o) {
    return super.lastIndexOf(o);
  }
}
