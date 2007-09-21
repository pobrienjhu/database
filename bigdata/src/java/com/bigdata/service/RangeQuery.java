/**

The Notice below must appear in each file of the Source Code of any
copy you distribute of the Licensed Product.  Contributors to any
Modifications may add their own copyright notices to identify their
own contributions.

License:

The contents of this file are subject to the CognitiveWeb Open Source
License Version 1.1 (the License).  You may not copy or use this file,
in either source code or executable form, except in compliance with
the License.  You may obtain a copy of the License from

  http://www.CognitiveWeb.org/legal/license/

Software distributed under the License is distributed on an AS IS
basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.  See
the License for the specific language governing rights and limitations
under the License.

Copyrights:

Portions created by or assigned to CognitiveWeb are Copyright
(c) 2003-2003 CognitiveWeb.  All Rights Reserved.  Contact
information for CognitiveWeb is available at

  http://www.CognitiveWeb.org

Portions Copyright (c) 2002-2003 Bryan Thompson.

Acknowledgements:

Special thanks to the developers of the Jabber Open Source License 1.0
(JOSL), from which this License was derived.  This License contains
terms that differ from JOSL.

Special thanks to the CognitiveWeb Open Source Contributors for their
suggestions and support of the Cognitive Web.

Modifications:

*/
/*
 * Created on Sep 21, 2007
 */

package com.bigdata.service;

import java.util.NoSuchElementException;

import org.apache.log4j.Logger;

import com.bigdata.btree.BytesUtil;
import com.bigdata.btree.IEntryIterator;
import com.bigdata.service.ClientIndexView.PartitionedRangeQuery;

/**
 * Class supports range query across against an unpartitioned index on an
 * {@link IDataService}.
 * 
 * @todo support an optional key and/or value filter that is executed on the
 *       data service.
 * 
 * @todo if unisolated or read-committed, then we may need to re-assess the
 *       toIndex during the query.
 * 
 * @todo this is derived from {@link PartitionedRangeQuery} and needs its own
 *       test suite.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class RangeQuery implements IEntryIterator {
    
    public static final transient Logger log = Logger
            .getLogger(RangeQuery.class);

    /**
     * Error message used by {@link #getKey()} when the iterator was not
     * provisioned to request keys from the data service.
     */
    static public transient final String ERR_NO_KEYS = "Keys not requested";
    
    /**
     * Error message used by {@link #getValue()} when the iterator was not
     * provisioned to request values from the data service.
     */
    static public transient final String ERR_NO_VALS = "Values not requested";

    /**
     * The index on which the range query is being performed.
     */
    private final String name;
    
    /**
     * The transaction identifier -or- zero iff the request is unisolated.
     */
    private final long tx;

    /**
     * The first key to visit -or- null iff no lower bound.
     */
    private final byte[] fromKey;
    
    /**
     * The first key to NOT visit -or- null iff no upper bound.
     */
    private final byte[] toKey;

    /**
     * This controls the #of results per data service query.
     */
    private final int capacity;

    /**
     * These flags control whether keys and/or values are requested. If
     * neither keys nor values are requested, then this is just a range
     * count operation and you might as well use rangeCount instead.
     */
    private final int flags;

    /**
     * The #of range query operations executed.
     */
    private int nqueries;
    
    /**
     * The data service for the index.
     * 
     * @todo this should failover.
     */
    final IDataService dataService;

    /**
     * The current result set. For each index partition spanned by the
     * overall key range supplied by the client, we will issue at least one
     * range query against the data service for that index partition. Once
     * all entries in a result set have been consumed by the client, we test
     * the result set to see whether or not it exhausted the entries that
     * could be matched for that index partition. If not, then we will issue
     * another "continuation" range query against the same index position
     * starting from the successor of the last key scanned. If it is
     * exhausted, then we will issue a new range query against the next
     * index partition. If no more index partitions remain that are spanned
     * by the key range specified by the client then we are done.
     * <p>
     * Note: A result set will be empty if there are no entries (including
     * after filtering) that lie within the key range in a given index
     * partition. It is possible for any of the result sets to be empty.
     * Consider a case of static partitioning of an index into N partitions.
     * When the index is empty, a range query of the entire index will still
     * query each of the N partitions. However, since the index is empty
     * none of the partitions will have any matching entries and all result
     * sets will be empty.
     */
    private ResultSet rset = null;

    /**
     * The #of enties visited so far.
     */
    private long nvisited = 0;
    
    /**
     * The index of the last entry visited in the current {@link ResultSet}.
     * This is reset to <code>-1</code> each time we obtain a new
     * {@link ResultSet}.
     */
    private int lastVisited = -1;
    
    /**
     * The last key visited by the iterator.
     */
    private byte[] lastKey = null;

    /**
     * The last value visited by the iterator.
     */
    private byte[] lastVal = null;
    
    /**
     * When true, the entire key range specified by the client has been
     * visited and the iterator is exhausted (i.e., all done).
     */
    private boolean exhausted = false;
    
    /**
     * The #of queries issued so far.
     */
    public int getQueryCount() {

        return nqueries;
        
    }
    
    /**
     * The #of entries visited so far (not the #of entries scanned, which
     * can be much greater if a filter is in use).
     */
    public long getVisitedCount() {
        
        return nvisited;
        
    }
    
    public RangeQuery(IDataService dataService, String name, long tx,
            byte[] fromKey, byte[] toKey, int capacity, int flags) {

        if (dataService == null) {

            throw new IllegalArgumentException();
            
        }

        if (name == null) {

            throw new IllegalArgumentException();
            
        }
        
        if (capacity <= 0) {

            throw new IllegalArgumentException("capacity must be positive.");
            
        }

        this.dataService = dataService;
        this.name = name;
        this.tx = tx;
        this.fromKey = fromKey;
        this.toKey = toKey;
        this.capacity = capacity;
        this.flags = flags;

        // Obtain the first result set.
        rangeQuery();

    }

    /**
     * Issues a new range query against the current partition (the one
     * identified by {@link #index}. 
     */
    private void rangeQuery() {

        assert ! exhausted;

        try {

            log.info("name=" + name + ", fromKey="
                    + BytesUtil.toString(fromKey) + ", toKey="
                    + BytesUtil.toString(toKey));
            
            rset = dataService.rangeQuery(tx, name, -1 /*unpartitioned*/,
                    fromKey, toKey, capacity, flags);
            
            // reset index into the ResultSet.
            lastVisited = -1;
            
            nqueries++;

        } catch (Exception ex) {

            throw new RuntimeException(ex);

        }

    }
    
    /**
     * Issues a "continuation" query within the same index partition. This
     * is invoked iff the there are no entries left to visit in the current
     * {@link ResultSet} but {@link ResultSet#isExhausted()} is [false],
     * indicating that there is more data in that index partition.
     */
    private void continuationQuery() {
        
        assert ! exhausted;
        assert rset != null;
        assert ! rset.isExhausted();
        assert dataService != null;
        
        try {

            /*
             * Start from the successor of the last key scanned by the
             * previous result set.
             */

            final byte[] _fromKey = rset.successor();

            log.info("name=" + name 
                    + ", fromKey=" + BytesUtil.toString(_fromKey)
                    + ", toKey=" + BytesUtil.toString(toKey));
            
            rset = dataService.rangeQuery(tx, name, -1/*unpartitioned*/,
                    _fromKey, toKey, capacity, flags);
            
            // reset index into the ResultSet.
            lastVisited = -1;

            nqueries++;
            
        } catch (Exception ex) {

            throw new RuntimeException(ex);

        }

    }
    
    /**
     * There are three levels at which we need to test in order to determine
     * if the total iterator is exhausted. First, we need to test to see if
     * there are more entries remaining in the current {@link ResultSet}.
     * If not and the {@link ResultSet} is NOT
     * {@link ResultSet#isExhausted() exhausted}, then we issue a
     * {@link #continuationQuery()} against the same index partition. If the
     * {@link ResultSet} is {@link ResultSet#isExhausted() exhausted}, then
     * we test to see whether or not we have visited all index partitions.
     * If so, then the iterator is exhausted. Otherwise we issue a range
     * query against the {@link #nextPartition()}.
     * 
     * @return True iff the iterator is not exhausted.
     */
    public boolean hasNext() {
        
        assert rset != null;
        
        if (exhausted) {

            return false;
            
        }
        
        final int ntuples = rset.getNumTuples();
        
        if (ntuples > 0 && lastVisited + 1 < ntuples) {
            /*
             * There is more data in the current ResultSet.
             */
            return true;
        }
        
        if(!rset.isExhausted()) {
            /*
             * This result set is empty but there is more data in the
             * current partition.
             */
            continuationQuery();
            /*
             * Recursive query since the result set might be empty.
             * 
             * Note: The result set could be empty if we are: (a)
             * unisolated; or (b) in a read committed transaction; or (c) if
             * a filter is being applied.
             */
            return hasNext();
        }
        /*
         * Exausted.
         */
        exhausted = true;
        return false;
    }

    /**
     * The byte[] containing the serialized value for the next matching
     * entry in the index -or- <code>null</code> iff the iterator was
     * provisioned to NOT visit values.
     */
    public Object next() {

        if (!hasNext()) {

            throw new NoSuchElementException();
            
        }
        
        nvisited++;

        lastVisited++;
        
        lastKey = ((flags & IDataService.KEYS) == 0) ? null : rset
                .getKeys()[lastVisited];

        lastVal = ((flags & IDataService.VALS) == 0) ? null : rset
                .getValues()[lastVisited];
        
        return lastVal;
        
    }

    /**
     * The byte[] containing the key for the last matching entry in the
     * index.
     * 
     * @exception IllegalStateException
     *                if nothing has been visited.
     * 
     * @exception UnsupportedOperationException
     *                if the iterator was not provisioned to visit keys.
     */
    public byte[] getKey() {
        
        if(nvisited==0L) {
            
            // Nothing visited yet.
            throw new IllegalStateException();
            
        }
        
        if((flags & IDataService.KEYS)==0) {

            // Keys not requested.
            throw new UnsupportedOperationException(ERR_NO_KEYS);
            
        }
        
        return lastKey;
        
    }

    public Object getValue() {

        if(nvisited==0L) {
           
            // Nothing visited yet.
            throw new IllegalStateException();
            
        }
        
        if((flags & IDataService.VALS)==0) {
            
            // Values not requested.
            throw new UnsupportedOperationException(ERR_NO_VALS);
            
        }

        return lastVal;
        
    }

    /**
     * This operation is not supported.
     * 
     * @exception UnsupportedOperationException
     *                Always.
     */
    public void remove() {
        
        throw new UnsupportedOperationException();
        
    }

}
