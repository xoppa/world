package com.xoppa.android.world;

import java.util.Iterator;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class PredicateIterator<T> implements Iterator<T> {
	public interface Predicate<T> {
		boolean evaluate(T arg0);
	}
	public static class PredicateIterable<T> implements Iterable<T> {
		public final Iterable<T> iterable;
		public final Predicate<T> predicate;
		public PredicateIterable(Iterable<T> iterable, Predicate<T> predicate) {
			this.iterable = iterable;
			this.predicate = predicate;
		}
		
		@Override
		public Iterator<T> iterator() {
			return new PredicateIterator<T>(iterable.iterator(), predicate);
		}
	}
	
	protected Iterator<T> iterator;
	protected Predicate<T> predicate;
	protected boolean end = false;
	protected T next = null;
	
	public PredicateIterator(final Iterator<T> iterator, final Predicate<T> predicate) {
		this.iterator = iterator;
		this.predicate = predicate;
	}

	@Override
	public boolean hasNext() {
		if (end) return false;
		if (next != null) return true;
		while(iterator.hasNext()) {
			final T n = iterator.next();
			if (predicate.evaluate(n)) {
				next = n;
				return true;
			}
		}
		end = true;
		return false;
	}

	@Override
	public T next() {
		if (next == null)
			hasNext();
		final T result = next;
		next = null;
		return result;
	}

	@Override
	public void remove() {
	}
}
