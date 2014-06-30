//
// Cleversafe open-source code header - Version 1.3 - January 2, 2009
//
// Cleversafe Dispersed Storage(TM) is software for secure, private and
// reliable storage of the world's data using information dispersal.
//
// Copyright (C) 2005-2009 Cleversafe, Inc. All rights reserved.
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301,
// USA.
//
// Contact Information: Cleversafe, 224 North Desplaines Street, Suite 500
// Chicago, IL 60661, USA.
// email licensing@cleversafe.org
//
// END-OF-HEADER
//
// -----------------------
// @author: conor
//
// Date: Nov 16, 2010
// ---------------------

package com.cleversafe.og.object.manager;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;

// This is modified from java.util's ConcurrentHashMap to support randomly selecting a member, and
// to be a set instead of a map
public class RandomAccessConcurrentHashSet<K>
{
   /*
    * The basic strategy is to subdivide the table among Segments, each of which itself is a
    * concurrently readable hash table.
    */

   /* ---------------- Constants -------------- */

   /**
    * The default initial capacity for this table, used when not otherwise specified in a
    * constructor.
    */
   static final int DEFAULT_INITIAL_CAPACITY = 16;

   /**
    * The default load factor for this table, used when not otherwise specified in a constructor.
    */
   static final float DEFAULT_LOAD_FACTOR = 0.75f;

   /**
    * The default concurrency level for this table, used when not otherwise specified in a
    * constructor.
    */
   static final int DEFAULT_CONCURRENCY_LEVEL = 16;

   /**
    * The maximum capacity, used if a higher value is implicitly specified by either of the
    * constructors with arguments. MUST be a power of two <= 1<<30 to ensure that entries are
    * indexable using ints.
    */
   static final int MAXIMUM_CAPACITY = 1 << 30;

   /**
    * The maximum number of segments to allow; used to bound constructor arguments.
    */
   // slightly conservative
   static final int MAX_SEGMENTS = 1 << 16;

   /**
    * Number of unsynchronized retries in size and containsValue methods before resorting to
    * locking. This is used to avoid unbounded retries if tables undergo continuous modification
    * which would make it impossible to obtain an accurate result.
    */
   static final int RETRIES_BEFORE_LOCK = 2;

   /* ---------------- Fields -------------- */

   /**
    * Mask value for indexing into segments. The upper bits of a key's hash code are used to choose
    * the segment.
    */
   final int segmentMask;

   /**
    * Shift value for indexing within segments.
    */
   final int segmentShift;

   /**
    * The segments, each of which is a specialized hash table
    */
   final Segment<K>[] segments;

   /* ---------------- Small Utilities -------------- */

   /**
    * Applies a supplemental hash function to a given hashCode, which defends against poor quality
    * hash functions. This is critical because ConcurrentHashMap uses power-of-two length hash
    * tables, that otherwise encounter collisions for hashCodes that do not differ in lower or upper
    * bits.
    */
   private static int hash(int h)
   {
      // Spread bits to regularize both segment and index locations,
      // using variant of single-word Wang/Jenkins hash.
      h += (h << 15) ^ 0xffffcd7d;
      h ^= (h >>> 10);
      h += (h << 3);
      h ^= (h >>> 6);
      h += (h << 2) + (h << 14);
      return h ^ (h >>> 16);
   }

   /**
    * Returns the segment that should be used for key with given hash
    * 
    * @param hash
    *           the hash code for the key
    * @return the segment
    */
   final Segment<K> segmentFor(final int hash)
   {
      return this.segments[(hash >>> this.segmentShift) & this.segmentMask];
   }

   /* ---------------- Inner Classes -------------- */

   /**
    * ConcurrentHashSet list entry. Note that this is never exported out as a user-visible entry.
    * 
    */
   static final class HashEntry<K>
   {
      final K key;
      final int hash;
      volatile int length;
      final HashEntry<K> next;

      HashEntry(final K key, final int hash, final HashEntry<K> next)
      {
         this(key, hash, next, next == null ? 1 : next.length + 1);
      }

      HashEntry(final K key, final int hash, final HashEntry<K> next, final int length)
      {
         this.key = key;
         this.hash = hash;
         this.next = next;
         this.length = length;
      }

      @SuppressWarnings("unchecked")
      static final <K> HashEntry<K>[] newArray(final int i)
      {
         return new HashEntry[i];
      }
   }

   /**
    * Segments are specialized versions of hash tables. This subclasses from ReentrantLock
    * opportunistically, just to simplify some locking and avoid separate construction.
    */
   @SuppressWarnings("serial")
   static final class Segment<K> extends ReentrantLock
   {
      /*
       * Segments maintain a table of entry lists that are ALWAYS kept in a consistent state, so can
       * be read without locking. Next fields of nodes are immutable (final). All list additions are
       * performed at the front of each bin. This makes it easy to check changes, and also fast to
       * traverse. When nodes would otherwise be changed, new nodes are created to replace them.
       * This works well for hash tables since the bin lists tend to be short. (The average length
       * is less than two for the default load factor threshold.)
       * 
       * Read operations can thus proceed without locking, but rely on selected uses of volatiles to
       * ensure that completed write operations performed by other threads are noticed. For most
       * purposes, the "count" field, tracking the number of elements, serves as that volatile
       * variable ensuring visibility. This is convenient because this field needs to be read in
       * many read operations anyway:
       * 
       * - All (unsynchronized) read operations must first read the "count" field, and should not
       * look at table entries if it is 0.
       * 
       * - All (synchronized) write operations should write to the "count" field after structurally
       * changing any bin. The operations must not take any action that could even momentarily cause
       * a concurrent read operation to see inconsistent data. This is made easier by the nature of
       * the read operations in Map. For example, no operation can reveal that the table has grown
       * but the threshold has not yet been updated, so there are no atomicity requirements for this
       * with respect to reads.
       * 
       * As a guide, all critical volatile reads and writes to the count field are marked in code
       * comments.
       */

      /**
       * The number of elements in this segment's region.
       */
      transient volatile int count;

      /**
       * Number of updates that alter the size of the table. This is used during bulk-read methods
       * to make sure they see a consistent snapshot: If modCounts change during a traversal of
       * segments computing size or checking containsValue, then we might have an inconsistent view
       * of state so (usually) must retry.
       */
      transient int modCount;

      /**
       * The table is rehashed when its size exceeds this threshold. (The value of this field is
       * always <tt>(int)(capacity *
       * loadFactor)</tt>.)
       */
      transient int threshold;

      /**
       * The per-segment table.
       */
      transient volatile HashEntry<K>[] table;

      /**
       * The load factor for the hash table. Even though this value is same for all segments, it is
       * replicated to avoid needing links to outer object.
       * 
       * @serial
       */
      final float loadFactor;

      Segment(final int initialCapacity, final float lf)
      {
         this.loadFactor = lf;
         setTable(HashEntry.<K> newArray(initialCapacity));
      }

      @SuppressWarnings("unchecked")
      static final <K> Segment<K>[] newArray(final int i)
      {
         return new Segment[i];
      }

      /**
       * Sets table to new HashEntry array. Call only while holding lock or in constructor.
       */
      void setTable(final HashEntry<K>[] newTable)
      {
         this.threshold = (int) (newTable.length * this.loadFactor);
         this.table = newTable;
      }

      /**
       * Returns properly casted first entry of bin for given hash.
       */
      HashEntry<K> getLeft(final int hash)
      {
         final HashEntry<K>[] tab = this.table;
         return tab[hash & (tab.length - 1)];
      }

      /* Specialized implementations of set methods */

      K get(final Object key, final int hash, final boolean random)
      {
         HashEntry<K> e = null;
         if (this.count != 0)
         { // read-volatile
            if (random)
            {
               while (this.count != 0 && e == null)
               {
                  final HashEntry<K>[] tab = this.table;
                  e = tab[generator.nextInt(tab.length)];
               }
               if (e == null)
               {
                  return null;
               }
               final HashEntry<K> first = e;
               int skip = generator.nextInt(e.length);
               while (skip > 0)
               {
                  if (e == null || e.next == null)
                  {
                     e = first;
                  }
                  else
                  {
                     e = e.next;
                  }
                  skip--;
               }
            }
            else
            {
               e = getLeft(hash);
               while (e != null)
               {
                  if (e.hash == hash && key.equals(e.key))
                  {
                     break;
                  }
                  e = e.next;
               }
            }
         }
         if (e == null)
         {
            return null;
         }
         return e.key;
      }

      boolean contains(final Object key, final int hash)
      {
         if (this.count != 0)
         { // read-volatile
            HashEntry<K> e = getLeft(hash);
            while (e != null)
            {
               if (e.hash == hash && key.equals(e.key))
               {
                  return true;
               }
               e = e.next;
            }
         }
         return false;
      }

      K put(final K key, final int hash)
      {
         lock();
         try
         {
            int c = this.count;
            if (c++ > this.threshold)
            {
               rehash();
            }
            final HashEntry<K>[] tab = this.table;
            final int index = hash & (tab.length - 1);
            final HashEntry<K> first = tab[index];
            HashEntry<K> e = first;
            while (e != null && (e.hash != hash || !key.equals(e.key)))
            {
               e = e.next;
            }

            if (e != null)
            {
               return null;
            }
            else
            {
               ++this.modCount;
               tab[index] = new HashEntry<K>(key, hash, first);
               this.count = c; // write-volatile
            }
            return key;
         }
         finally
         {
            unlock();
         }
      }

      void rehash()
      {
         final HashEntry<K>[] oldTable = this.table;
         final int oldCapacity = oldTable.length;
         if (oldCapacity >= MAXIMUM_CAPACITY)
         {
            return;
         }

         /*
          * Reclassify nodes in each list to new Map. Because we are using power-of-two expansion,
          * the elements from each bin must either stay at same index, or move with a power of two
          * offset. We eliminate unnecessary node creation by catching cases where old nodes can be
          * reused because their next fields won't change. Statistically, at the default threshold,
          * only about one-sixth of them need cloning when a table doubles. The nodes they replace
          * will be garbage collectable as soon as they are no longer referenced by any reader
          * thread that may be in the midst of traversing table right now.
          */

         final HashEntry<K>[] newTable = HashEntry.newArray(oldCapacity << 1);
         this.threshold = (int) (newTable.length * this.loadFactor);
         final int sizeMask = newTable.length - 1;
         for (int i = 0; i < oldCapacity; i++)
         {
            // We need to guarantee that any existing reads of old Map can
            // proceed. So we cannot yet null out each bin.
            final HashEntry<K> e = oldTable[i];

            if (e != null)
            {
               final HashEntry<K> next = e.next;
               final int idx = e.hash & sizeMask;

               // Single node on list
               if (next == null)
               {
                  newTable[idx] = e;
               }
               else
               {
                  // Reuse trailing consecutive sequence at same slot
                  HashEntry<K> lastRun = e;
                  int lastIdx = idx;
                  for (HashEntry<K> last = next; last != null; last = last.next)
                  {
                     final int k = last.hash & sizeMask;
                     if (k != lastIdx)
                     {
                        lastIdx = k;
                        lastRun = last;
                     }
                  }
                  newTable[lastIdx] = lastRun;

                  // Clone all remaining nodes
                  for (HashEntry<K> p = e; p != lastRun; p = p.next)
                  {
                     final int k = p.hash & sizeMask;
                     final HashEntry<K> n = newTable[k];
                     newTable[k] = new HashEntry<K>(p.key, p.hash, n);
                  }
               }
            }
         }
         this.table = newTable;
      }

      /**
       * Remove
       */

      K remove(final Object key, final int hash, final boolean random)
      {
         lock();
         try
         {
            final int c = this.count - 1;
            final HashEntry<K>[] tab = this.table;
            HashEntry<K> e = null;
            final HashEntry<K> first;
            int index = 0;
            if (random)
            {
               while (this.count != 0 && e == null)
               {
                  index = generator.nextInt(tab.length);
                  e = tab[index];
               }
               if (e == null)
               {
                  return null;
               }
               first = e;
               int skip = generator.nextInt(e.length);
               while (skip > 0)
               {
                  if (e == null || e.next == null)
                  {
                     e = first;
                  }
                  else
                  {
                     e = e.next;
                  }
                  skip--;
               }
            }
            else
            {
               index = hash & (tab.length - 1);
               first = tab[index];
               e = first;
               while (e != null && (e.hash != hash || !key.equals(e.key)))
               {
                  e = e.next;
               }
            }

            K oldKey = null;
            if (e != null)
            {
               oldKey = e.key;
               // All entries following removed node can stay
               // in list, but all preceding ones need to be
               // cloned.
               ++this.modCount;
               HashEntry<K> newFirst = e.next;
               for (HashEntry<K> p = first; p != e; p = p.next)
               {
                  newFirst = new HashEntry<K>(p.key, p.hash, newFirst);
               }
               tab[index] = newFirst;
               this.count = c; // write-volatile
            }
            return oldKey;
         }
         finally
         {
            unlock();
         }
      }

      void clear()
      {
         if (this.count != 0)
         {
            lock();
            try
            {
               final HashEntry<K>[] tab = this.table;
               for (int i = 0; i < tab.length; i++)
               {
                  tab[i] = null;
               }
               ++this.modCount;
               this.count = 0; // write-volatile
            }
            finally
            {
               unlock();
            }
         }
      }
   }

   /* ---------------- Public operations -------------- */

   /**
    * Creates a new, empty set with the specified initial capacity, load factor and concurrency
    * level.
    * 
    * @param initialCapacity
    *           the initial capacity. The implementation performs internal sizing to accommodate
    *           this many elements.
    * @param loadFactor
    *           the load factor threshold, used to control resizing. Resizing may be performed when
    *           the average number of elements per bin exceeds this threshold.
    * @param concurrencyLevel
    *           the estimated number of concurrently updating threads. The implementation performs
    *           internal sizing to try to accommodate this many threads.
    * @throws IllegalArgumentException
    *            if the initial capacity is negative or the load factor or concurrencyLevel are
    *            nonpositive.
    */
   public RandomAccessConcurrentHashSet(
         int initialCapacity,
         final float loadFactor,
         int concurrencyLevel)
   {
      if (!(loadFactor > 0) || initialCapacity < 0 || concurrencyLevel <= 0)
      {
         throw new IllegalArgumentException();
      }

      if (concurrencyLevel > MAX_SEGMENTS)
      {
         concurrencyLevel = MAX_SEGMENTS;
      }

      // Find power-of-two sizes best matching arguments
      int sshift = 0;
      int ssize = 1;
      while (ssize < concurrencyLevel)
      {
         ++sshift;
         ssize <<= 1;
      }
      this.segmentShift = 32 - sshift;
      this.segmentMask = ssize - 1;
      this.segments = Segment.newArray(ssize);

      if (initialCapacity > MAXIMUM_CAPACITY)
      {
         initialCapacity = MAXIMUM_CAPACITY;
      }
      int c = initialCapacity / ssize;
      if (c * ssize < initialCapacity)
      {
         ++c;
      }
      int cap = 1;
      while (cap < c)
      {
         cap <<= 1;
      }

      for (int i = 0; i < this.segments.length; ++i)
      {
         this.segments[i] = new Segment<K>(cap, loadFactor);
      }
   }

   /**
    * Creates a new, empty map with the specified initial capacity and load factor and with the
    * default concurrencyLevel (16).
    * 
    * @param initialCapacity
    *           The implementation performs internal sizing to accommodate this many elements.
    * @param loadFactor
    *           the load factor threshold, used to control resizing. Resizing may be performed when
    *           the average number of elements per bin exceeds this threshold.
    * @throws IllegalArgumentException
    *            if the initial capacity of elements is negative or the load factor is nonpositive
    * 
    * @since 1.6
    */
   public RandomAccessConcurrentHashSet(final int initialCapacity, final float loadFactor)
   {
      this(initialCapacity, loadFactor, DEFAULT_CONCURRENCY_LEVEL);
   }

   /**
    * Creates a new, empty map with the specified initial capacity, and with default load factor
    * (0.75) and concurrencyLevel (16).
    * 
    * @param initialCapacity
    *           the initial capacity. The implementation performs internal sizing to accommodate
    *           this many elements.
    * @throws IllegalArgumentException
    *            if the initial capacity of elements is negative.
    */
   public RandomAccessConcurrentHashSet(final int initialCapacity)
   {
      this(initialCapacity, DEFAULT_LOAD_FACTOR, DEFAULT_CONCURRENCY_LEVEL);
   }

   /**
    * Creates a new, empty map with a default initial capacity (16), load factor (0.75) and
    * concurrencyLevel (16).
    */
   public RandomAccessConcurrentHashSet()
   {
      this(DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR, DEFAULT_CONCURRENCY_LEVEL);
   }

   /**
    * Returns <tt>true</tt> if this set contains no members.
    * 
    * @return <tt>true</tt> if this set contains no members
    */
   public boolean isEmpty()
   {
      final Segment<K>[] segments = this.segments;
      /*
       * We keep track of per-segment modCounts to avoid ABA problems in which an element in one
       * segment was added and in another removed during traversal, in which case the table was
       * never actually empty at any point. Note the similar use of modCounts in the size() and
       * containsValue() methods, which are the only other methods also susceptible to ABA problems.
       */
      final int[] mc = new int[segments.length];
      int mcsum = 0;
      for (int i = 0; i < segments.length; ++i)
      {
         if (segments[i].count != 0)
         {
            return false;
         }
         else
         {
            mcsum += mc[i] = segments[i].modCount;
         }
      }
      // If mcsum happens to be zero, then we know we got a snapshot
      // before any modifications at all were made. This is
      // probably common enough to bother tracking.
      if (mcsum != 0)
      {
         for (int i = 0; i < segments.length; ++i)
         {
            if (segments[i].count != 0 || mc[i] != segments[i].modCount)
            {
               return false;
            }
         }
      }
      return true;
   }

   /**
    * Returns the number of members in this set. If the set contains more than
    * <tt>Integer.MAX_VALUE</tt> elements, returns <tt>Integer.MAX_VALUE</tt>.
    * 
    * @return the number of members in this set
    */
   public int size()
   {
      final Segment<K>[] segments = this.segments;
      long sum = 0;
      long check = 0;
      final int[] mc = new int[segments.length];
      // Try a few times to get accurate count. On failure due to
      // continuous async changes in table, resort to locking.
      for (int k = 0; k < RETRIES_BEFORE_LOCK; ++k)
      {
         check = 0;
         sum = 0;
         int mcsum = 0;
         for (int i = 0; i < segments.length; ++i)
         {
            sum += segments[i].count;
            mcsum += mc[i] = segments[i].modCount;
         }
         if (mcsum != 0)
         {
            for (int i = 0; i < segments.length; ++i)
            {
               check += segments[i].count;
               if (mc[i] != segments[i].modCount)
               {
                  // force retry
                  check = -1;
                  break;
               }
            }
         }
         if (check == sum)
         {
            break;
         }
      }
      if (check != sum)
      {
         // Resort to locking all segments
         sum = 0;
         for (final Segment<K> segment : segments)
         {
            segment.lock();
         }
         for (final Segment<K> segment : segments)
         {
            sum += segment.count;
         }
         for (final Segment<K> segment : segments)
         {
            segment.unlock();
         }
      }
      if (sum > Integer.MAX_VALUE)
      {
         return Integer.MAX_VALUE;
      }
      else
      {
         return (int) sum;
      }
   }

   /**
    * Returns the entry if it is a member of the set, or {@code null} if this set does not contain
    * that object.
    * 
    * @throws NullPointerException
    *            if the specified key is null
    */
   public K get(final Object key)
   {
      final int hash = RandomAccessConcurrentHashSet.hash(key.hashCode());
      return segmentFor(hash).get(key, hash, false);
   }

   /*
    * Returns a random member of the set or {@code null} if it is empty
    */
   public K getRandom()
   {
      K random = null;
      do
      {
         final Segment<K>[] segments = this.segments;
         random = segments[generator.nextInt(segments.length)].get(null, 0, true);
      } while (random == null && !isEmpty());
      return random;
   }

   /**
    * Tests if the specified object is a key in this table.
    * 
    * @param key
    *           possible key
    * @return <tt>true</tt> if and only if the specified object is a key in this table, as
    *         determined by the <tt>equals</tt> method; <tt>false</tt> otherwise.
    * @throws NullPointerException
    *            if the specified key is null
    */
   public boolean contains(final Object key)
   {
      final int hash = RandomAccessConcurrentHashSet.hash(key.hashCode());
      return segmentFor(hash).contains(key, hash);
   }

   /**
    * Puts the specified object in this table. The object cannot be null
    * 
    * @param key
    *           object to put in the set
    * @return the <tt>key</tt> itself, or <tt>null</tt> if it was already in the set
    * @throws NullPointerException
    *            if the specified key is null
    */
   public K put(final K key)
   {
      final int hash = RandomAccessConcurrentHashSet.hash(key.hashCode());
      return segmentFor(hash).put(key, hash);
   }

   /**
    * Removes the object from this set. This method does nothing if the object is not in the set.
    * 
    * @param key
    *           the key that needs to be removed
    * @return the previous <tt>key</tt> itself, or <tt>null</tt> if the <tt>key</tt> is not in the
    *         set
    * @throws NullPointerException
    *            if the specified key is null
    */
   public K remove(final Object key)
   {
      final int hash = RandomAccessConcurrentHashSet.hash(key.hashCode());
      return segmentFor(hash).remove(key, hash, false);
   }

   /*
    * Removes and returns a random member of the set or {@code null} if it is empty.
    */
   static final Random generator = new Random();

   public K removeRandom()
   {
      K random = null;
      do
      {
         final Segment<K>[] segments = this.segments;
         random =
               segments[RandomAccessConcurrentHashSet.generator.nextInt(segments.length)].remove(
                     null, 0, true);
      } while (random == null && !isEmpty());
      return random;
   }

   /**
    * Removes all of the members from this set.
    */
   public void clear()
   {
      for (final Segment<K> segment : this.segments)
      {
         segment.clear();
      }
   }

   public SetIterator iterator()
   {
      return new SetIterator();
   }

   class SetIterator implements Iterator<K>, Enumeration<K>
   {
      int nextSegmentIndex;
      int nextTableIndex;
      HashEntry<K>[] currentTable;
      HashEntry<K> nextEntry;
      HashEntry<K> lastReturned;

      SetIterator()
      {
         this.nextSegmentIndex = RandomAccessConcurrentHashSet.this.segments.length - 1;
         this.nextTableIndex = -1;
         advance();
      }

      @Override
      public boolean hasMoreElements()
      {
         return hasNext();
      }

      final void advance()
      {
         if (this.nextEntry != null && (this.nextEntry = this.nextEntry.next) != null)
         {
            return;
         }

         while (this.nextTableIndex >= 0)
         {
            if ((this.nextEntry = this.currentTable[this.nextTableIndex--]) != null)
            {
               return;
            }
         }

         while (this.nextSegmentIndex >= 0)
         {
            final Segment<K> seg =
                  RandomAccessConcurrentHashSet.this.segments[this.nextSegmentIndex--];
            if (seg.count != 0)
            {
               this.currentTable = seg.table;
               for (int j = this.currentTable.length - 1; j >= 0; --j)
               {
                  if ((this.nextEntry = this.currentTable[j]) != null)
                  {
                     this.nextTableIndex = j - 1;
                     return;
                  }
               }
            }
         }
      }

      @Override
      public boolean hasNext()
      {
         return this.nextEntry != null;
      }

      HashEntry<K> nextEntry()
      {
         if (this.nextEntry == null)
         {
            throw new NoSuchElementException();
         }
         this.lastReturned = this.nextEntry;
         advance();
         return this.lastReturned;
      }

      @Override
      public void remove()
      {
         if (this.lastReturned == null)
         {
            throw new IllegalStateException();
         }
         RandomAccessConcurrentHashSet.this.remove(this.lastReturned.key);
         this.lastReturned = null;
      }

      @Override
      public K next()
      {
         return nextEntry().key;
      }

      @Override
      public K nextElement()
      {
         return nextEntry().key;
      }

   }

}
