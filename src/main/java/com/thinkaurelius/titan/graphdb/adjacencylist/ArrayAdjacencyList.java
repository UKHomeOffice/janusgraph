package com.thinkaurelius.titan.graphdb.adjacencylist;

import com.google.common.base.Preconditions;
import com.thinkaurelius.titan.core.EdgeType;
import com.thinkaurelius.titan.core.EdgeTypeGroup;
import com.thinkaurelius.titan.exceptions.InvalidEdgeException;
import com.thinkaurelius.titan.graphdb.edges.InternalEdge;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class ArrayAdjacencyList implements AdjacencyList {

	private static final long serialVersionUID = -2868708972683152295L;

	private final ArrayAdjListFactory factory;
	private InternalEdge[] contents;

	
	ArrayAdjacencyList(ArrayAdjListFactory factory) {
		this.factory=factory;
		contents = new InternalEdge[factory.getInitialCapacity()];
	}
	
	@Override
	public synchronized AdjacencyList addEdge(InternalEdge e, ModificationStatus status) {
		return addEdge(e,false,status);
	}


	@Override
	public synchronized AdjacencyList addEdge(InternalEdge e, boolean checkTypeUniqueness, ModificationStatus status) {
		int emptySlot = -1;
		for (int i=0;i<contents.length;i++) {
			if (contents[i]==null) {
				emptySlot=i;
				continue;
			}
			InternalEdge oth = contents[i];
			assert oth!=null;
			if (oth.equals(e)) {
				status.nochange();
				return this;
			} else if (checkTypeUniqueness && oth.getEdgeType().equals(e.getEdgeType())) {
				throw new InvalidEdgeException("Cannot add functional edge since an edge of that type already exists!");
			}
		}
		if (emptySlot<0 && contents.length>=factory.getMaxCapacity()) {
			return factory.extend(this, e, status);
		} else {
			//Add internally
			if (emptySlot>=0) {
				contents[emptySlot]=e;
			} else {
				//Expand & copy
				InternalEdge[] contents2 = new InternalEdge[factory.updateCapacity(contents.length)];
				System.arraycopy(contents, 0, contents2, 0, contents.length);
				contents2[contents.length]=e;
				contents=contents2;
			}
			status.change();
			return this;
		}
	}

	@Override
	public boolean containsEdge(InternalEdge e) {
		for (int i=0;i<contents.length;i++) {
			if (contents[i]!=null && e.equals(contents[i])) return true;
		}
		return false;
	}
	

	@Override
	public boolean isEmpty() {
		for (int i=0;i<contents.length;i++) {
			if (contents[i]!=null) return false;
		}
		return true;
	}

	@Override
	public synchronized void removeEdge(InternalEdge e, ModificationStatus status) {
		status.nochange();
		for (int i=0;i<contents.length;i++) {
			if (contents[i]!=null && e.equals(contents[i])) {
				contents[i]=null;
				status.change();
			}
		}
	}

	@Override
	public AdjacencyListFactory getFactory() {
		return factory;
	}

	

	@Override
	public Iterable<InternalEdge> getEdges() {
		return new Iterable<InternalEdge>() {

			@Override
			public Iterator<InternalEdge> iterator() {
				return new InternalIterator();
			}
			
		};
	}

	@Override
	public Iterable<InternalEdge> getEdges(final EdgeType type) {
		return new Iterable<InternalEdge>() {

			@Override
			public Iterator<InternalEdge> iterator() {
				return new InternalTypeIterator(type);
			}
			
		};
	}
	

	@Override
	public Iterable<InternalEdge> getEdges(final EdgeTypeGroup group) {
		return new Iterable<InternalEdge>() {

			@Override
			public Iterator<InternalEdge> iterator() {
				return new InternalGroupIterator(group);
			}
			
		};
	}

	
	private class InternalGroupIterator extends InternalIterator {
		
		private final EdgeTypeGroup group;
		
		private InternalGroupIterator(EdgeTypeGroup group) {
			super(false);
			Preconditions.checkNotNull(group);
			this.group=group;
			findNext();
		}
		
		@Override
		protected boolean applies(InternalEdge edge) {
			return group.equals(edge.getEdgeType().getGroup());
		}
		
	}
	
	private class InternalTypeIterator extends InternalIterator {
		
		private final EdgeType type;
		
		private InternalTypeIterator(EdgeType type) {
			super(false);
			Preconditions.checkNotNull(type);
			this.type=type;
			findNext();
		}
		
		@Override
		protected boolean applies(InternalEdge edge) {
			return type.equals(edge.getEdgeType());
		}
		
	}
	
	private class InternalIterator implements Iterator<InternalEdge> {

		private InternalEdge next = null;
		private int position = -1;
		private InternalEdge last = null;

		
		InternalIterator() {
			this(true);
		}
		
		InternalIterator(boolean initialize) {
			if (initialize) findNext();
		}
		

		
		protected void findNext() {
			last = next;
			next = null;
			for (position = position+1; position<contents.length;position++) {
				if (contents[position]!=null && applies(contents[position])) {
					next = contents[position];
					break;
				}
			}
		}
		
		protected boolean applies(InternalEdge edge) {
			return true;
		}
		
		@Override
		public boolean hasNext() {
			return next!=null;
		}

		@Override
		public InternalEdge next() {
			if (next==null) throw new NoSuchElementException();
			InternalEdge old = next;
			findNext();
			return old;
		}

		@Override
		public void remove() {
			if (last==null) throw new NoSuchElementException();
			removeEdge(last,ModificationStatus.none);
		}
		
	}


	@Override
	public Iterator<InternalEdge> iterator() {
		return new InternalIterator();
	}


	
}