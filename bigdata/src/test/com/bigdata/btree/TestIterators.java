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
 * Created on Dec 11, 2006
 */

package com.bigdata.btree;

import org.apache.log4j.Level;

/**
 * Test suite for iterators. The tests are presented from the least dependencies
 * to the most dependencies ((traversal of the entries for a single leaf, then
 * children of a node, then dirty child of a node, then post-order traversal,
 * then post-order traversal of dirty nodes).
 * 
 * @see Leaf#entryIterator()
 * @see Node#childIterator(boolean)
 * @see AbstractNode#postOrderIterator(boolean)
 * 
 * @see TestDirtyIterators, which handles tests when some nodes or leaves are
 *      NOT dirty and verifies that the iterators do NOT visit such nodes or
 *      leaves. This tests {@link AbstractNode#postOrderIterator()} as well
 *      since that is just {@link AbstractNode#postOrderIterator(boolean)} with
 *      <code>false</code> passed in.
 * 
 * @todo write tests for efficient key range traversals for {@link IndexSegment}.
 * 
 * @todo write a test suite for concurrent modification under traversal and
 *       implement support for that feature in the various iterators.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TestIterators extends AbstractBTreeTestCase {

    /**
     * 
     */
    public TestIterators() {
    }

    /**
     * @param name
     */
    public TestIterators(String name) {
        super(name);
    }

    /**
     * Test ability to visit the entries on a leaf in key order.
     */
    public void test_leaf_entryIterator01() {
        
        BTree btree = getBTree(3);
        
        final Leaf root = (Leaf) btree.root;
        
        final byte[] k1 = i2k(1); // before any used key.
        final byte[] k3 = i2k(3);
        final byte[] k5 = i2k(5);
        final byte[] k7 = i2k(7);
        final byte[] k8 = i2k(8); // successor of all used keys.
        
        SimpleEntry v3 = new SimpleEntry(3);
        SimpleEntry v5 = new SimpleEntry(5);
        SimpleEntry v7 = new SimpleEntry(7);

        // insert keys until the root leaf is full.
        assertSameIterator(new Object[]{},root.entryIterator());
        btree.insert(k7, v7);
        assertSameIterator(new Object[]{v7},root.entryIterator());
        btree.insert(k5, v5);
        assertSameIterator(new Object[]{v5,v7},root.entryIterator());
        btree.insert(k3, v3);
        assertSameIterator(new Object[]{v3,v5,v7},root.entryIterator());
        // node range iterator tests.
        assertSameIterator(new Object[]{root},root.postOrderIterator(null,null));
        assertSameIterator(new Object[]{root},root.postOrderIterator(k1,k8));
        // entry range iterator tests.
        assertSameIterator(new Object[]{v3,v5,v7},root.rangeIterator(null,null));
        assertSameIterator(new Object[]{v3,v5,v7},root.rangeIterator(k3, k8));
        assertSameIterator(new Object[]{v3,v5},root.rangeIterator(k3, k7));
        assertSameIterator(new Object[]{v5},root.rangeIterator(k5, k7));
        assertSameIterator(new Object[]{v5,v7},root.rangeIterator(k5, k8));
        
        try {
            /*
             * try with search keys out of order.
             * 
             * Note: calling next() is required to force construction of an
             * EntryIterator that actually detects the search key ordering
             * problem.
             */
            root.rangeIterator(k8, k3).next();
            fail("Expecting: "+IllegalArgumentException.class);
        } catch(IllegalArgumentException ex) {
            System.err.println("Ignoring expected exception: "+ex);
        }
        
        // remove keys until the root leaf is empty.
        assertEquals(v5,btree.remove(k5));
        assertSameIterator(new Object[]{v3,v7},root.entryIterator());
        assertEquals(v7,btree.remove(k7));
        assertSameIterator(new Object[]{v3},root.entryIterator());
        assertEquals(v3,btree.remove(k3));
        assertSameIterator(new Object[]{},root.entryIterator());
        // node range iterator tests.
        assertSameIterator(new Object[]{root},root.postOrderIterator(null,null));
        assertSameIterator(new Object[]{root},root.postOrderIterator(k1,k8));
        // entry range iterator tests.
        assertSameIterator(new Object[]{},root.rangeIterator(k3, k8));
        assertSameIterator(new Object[]{},root.rangeIterator(k3, k7));
        assertSameIterator(new Object[]{},root.rangeIterator(k5, k7));
        assertSameIterator(new Object[]{},root.rangeIterator(k5, k8));

    }

    /**
     * Test ability to visit the direct children of a node.
     */
    public void test_childIterator01() {

        BTree btree = getBTree(3);

        final Leaf a = (Leaf) btree.root;
        
//        final byte[] k0 = i2k(0); // lies before any used key.
        final byte[] k1 = i2k(1);
        final byte[] k2 = i2k(2);
        final byte[] k3 = i2k(3);
        final byte[] k5 = i2k(5);
        final byte[] k6 = i2k(6); // lies between leaf(a) and leaf(b)
        final byte[] k7 = i2k(7);
        final byte[] k9 = i2k(9);
        final byte[] k10 = i2k(10); // lies after any used key.

        SimpleEntry v1 = new SimpleEntry(1);
        SimpleEntry v2 = new SimpleEntry(2);
        SimpleEntry v3 = new SimpleEntry(3);
        SimpleEntry v5 = new SimpleEntry(5);
        SimpleEntry v7 = new SimpleEntry(7);
        SimpleEntry v9 = new SimpleEntry(9);

        // fill up the root leaf.
        btree.insert(3, v3);
        btree.insert(5, v5);
        btree.insert(7, v7);

        // split the root leaf.
        btree.insert(9, v9);
        final Node c = (Node) btree.root;
        assertKeys(new int[]{7},c);
        assertEquals(a,c.getChild(0));
        final Leaf b = (Leaf)c.getChild(1);
        assertKeys(new int[]{3,5},a);
        assertValues(new Object[]{v3,v5}, a);
        assertKeys(new int[]{7,9},b);
        assertValues(new Object[]{v7,v9}, b);
        
        // verify visiting all children.
        assertSameIterator(new IAbstractNode[] { a, b }, ((Node) btree.root)
                .childIterator(false));
        // verify visiting all entries.
        assertSameIterator(new Object[]{v3,v5,v7,v9},btree.entryIterator());
        /*
         * verify child range iterator.
         * 
         * Note: there are interesting fence posts here. The key range is
         * sensitive to the separator key in (c) (which is 7) NOT to the keys
         * actually found in (a) and (b). For this reason, a child range query
         * with fromKey := 6 will visit (a) since the separator key is 7 and we
         * would insert 6 into (a) if the key existed.
         */
        assertSameIterator(new Object[]{a,b},c.childIterator(null,null));
        assertSameIterator(new Object[]{a},c.childIterator(k3,k6));
        assertSameIterator(new Object[]{b},c.childIterator(k7,k10));
        assertSameIterator(new Object[]{a},c.childIterator(k3,k5)); // fence post - only visits (a).
        assertSameIterator(new Object[]{b},c.childIterator(k7,k9)); // fence post - only visits (b).
        assertSameIterator(new Object[]{a,b},c.childIterator(k6,k7)); // fence post - visits (a) and (b).
        // verify node range iterator.
        assertSameIterator(new Object[]{a,b,c},c.postOrderIterator(null,null));
        assertSameIterator(new Object[]{a,c},c.postOrderIterator(k3,k6));
        assertSameIterator(new Object[]{b,c},c.postOrderIterator(k7,k10));
        assertSameIterator(new Object[]{a,b,c},c.postOrderIterator(k6,k7)); // fence post - visits both leaves even though no keys lie in the range.
        // verify entry range iterator.
        assertSameIterator(new Object[]{v3,v5,v7,v9},btree.rangeIterator(null,null));
        assertSameIterator(new Object[]{v3},btree.rangeIterator(k3,k5));
        assertSameIterator(new Object[]{v5,v7,v9},btree.rangeIterator(k5,k10));

        try { // try with search keys out of order.
            c.childIterator(k9, k3);
            fail("Expecting: "+IllegalArgumentException.class);
        } catch(IllegalArgumentException ex) {
            System.err.println("Ignoring expected exception: "+ex);
        }

        try { // try with search keys out of order.
            btree.rangeIterator(k9, k3);
            fail("Expecting: "+IllegalArgumentException.class);
        } catch(IllegalArgumentException ex) {
            System.err.println("Ignoring expected exception: "+ex);
        }

        /*
         * split another leaf so that there are now three children to visit. at
         * this point the root is full.
         */
        btree.insert(1, v1);
        btree.insert(2, v2);
        assertKeys(new int[]{3,7},c);
        assertEquals(a,c.getChild(0));
        Leaf d = (Leaf)c.getChild(1);
        assertEquals(b,c.getChild(2));
        assertKeys(new int[]{1,2},a);
        assertValues(new Object[]{v1,v2}, a);
        assertKeys(new int[]{3,5},d);
        assertValues(new Object[]{v3,v5}, d);
        assertKeys(new int[]{7,9},b);
        assertValues(new Object[]{v7,v9}, b);

        // verify visiting all children.
        assertSameIterator(new IAbstractNode[] { a, d, b }, ((Node) btree.root)
                .childIterator(false));
        // verify visiting children in a key range.
        assertSameIterator(new Object[]{a,d,b},c.childIterator(null,null));
        assertSameIterator(new Object[]{a},c.childIterator(k1,k2));
        assertSameIterator(new Object[]{d},c.childIterator(k3,k5));
        assertSameIterator(new Object[]{b},c.childIterator(k7,k9));
        assertSameIterator(new Object[]{a,d},c.childIterator(k1,k3));
        assertSameIterator(new Object[]{d,b},c.childIterator(k3,k9));
        // verify node range iterator.
        assertSameIterator(new Object[]{a,d,b,c},c.postOrderIterator(null,null));
        assertSameIterator(new Object[]{a,c},c.postOrderIterator(k1,k2));
        assertSameIterator(new Object[]{d,c},c.postOrderIterator(k3,k5));
        assertSameIterator(new Object[]{b,c},c.postOrderIterator(k7,k9));
        assertSameIterator(new Object[]{a,d,c},c.postOrderIterator(k1,k3));
        assertSameIterator(new Object[]{d,b,c},c.postOrderIterator(k3,k9));
        assertSameIterator(new Object[]{a,d,b,c},c.postOrderIterator(k1,k9));
        // verify entry range iterator.
        assertSameIterator(new Object[]{v1,v2,v3,v5,v7,v9},btree.rangeIterator(null,null));
        assertSameIterator(new Object[]{v3,v5,v7,v9},btree.rangeIterator(k3,k10));
        assertSameIterator(new Object[]{v2,v3},btree.rangeIterator(k2,i2k(4)));
        assertSameIterator(new Object[]{v3},btree.rangeIterator(k3,i2k(4)));
        assertSameIterator(new Object[]{v5,v7},btree.rangeIterator(k5,i2k(8)));
        assertSameIterator(new Object[]{v5,v7,v9},btree.rangeIterator(k5,k10));

        /*
         * remove a key from a leaf forcing two leaves to join and verify the
         * visitation order.
         */
        assertEquals(v1,btree.remove(1));
        assertKeys(new int[]{7},c);
        assertEquals(a,c.getChild(0));
        assertEquals(b,c.getChild(1));
        assertKeys(new int[]{2,3,5},a);
        assertValues(new Object[]{v2,v3,v5}, a);
        assertKeys(new int[]{7,9},b);
        assertValues(new Object[]{v7,v9}, b);
        assertTrue(d.isDeleted());

        // verify visiting all children.
        assertSameIterator(new IAbstractNode[] { a, b }, ((Node) btree.root)
                .childIterator(false));

        /*
         * Note: the test ends here since there must be either 2 or 3 children
         * for the root node.  If we force the remaining leaves to join, then
         * the root node will be replaced by a root leaf.
         */

    }

    /**
     * Test ability to visit the nodes of the tree in a post-order traversal.
     */
    public void test_postOrderIterator01() {

        BTree btree = getBTree(3);

        final Leaf a = (Leaf) btree.root;
        
        SimpleEntry v1 = new SimpleEntry(1);
        SimpleEntry v2 = new SimpleEntry(2);
        SimpleEntry v3 = new SimpleEntry(3);
        SimpleEntry v4 = new SimpleEntry(4);
        SimpleEntry v6 = new SimpleEntry(6);
        SimpleEntry v5 = new SimpleEntry(5);
        SimpleEntry v7 = new SimpleEntry(7);
        SimpleEntry v9 = new SimpleEntry(9);

        // empty tree visits the root leaf.
        assertSameIterator(new IAbstractNode[] { btree.root }, btree.root
                .postOrderIterator());
        
        // fill up the root leaf.
        btree.insert(3, v3);
        btree.insert(5, v5);
        btree.insert(7, v7);

        // split the root leaf.
        btree.insert(9, v9);
        final Node c = (Node) btree.root;
        assertKeys(new int[]{7},c);
        assertEquals(a,c.getChild(0));
        final Leaf b = (Leaf)c.getChild(1);
        assertKeys(new int[]{3,5},a);
        assertValues(new Object[]{v3,v5}, a);
        assertKeys(new int[]{7,9},b);
        assertValues(new Object[]{v7,v9}, b);
        
        // verify iterator.
        assertSameIterator(new IAbstractNode[] { a, b, c }, btree.root
                .postOrderIterator());

        /*
         * split another leaf so that there are now three children to visit. at
         * this point the root is full.
         */
        btree.insert(1, v1);
        btree.insert(2, v2);
        assertKeys(new int[]{3,7},c);
        assertEquals(a,c.getChild(0));
        Leaf d = (Leaf)c.getChild(1);
        assertEquals(b,c.getChild(2));
        assertKeys(new int[]{1,2},a);
        assertValues(new Object[]{v1,v2}, a);
        assertKeys(new int[]{3,5},d);
        assertValues(new Object[]{v3,v5}, d);
        assertKeys(new int[]{7,9},b);
        assertValues(new Object[]{v7,v9}, b);

        // verify iterator
        assertSameIterator(new IAbstractNode[] { a, d, b, c }, btree.root
                .postOrderIterator());
        
        /*
         * cause another leaf (d) to split, forcing the split to propagate to and
         * split the root and the tree to increase in height.
         */
        btree.insert(4, v4);
        btree.insert(6, v6);
//        btree.dump(Level.DEBUG,System.err);
        assertNotSame(c,btree.root);
        final Node g = (Node)btree.root;
        assertKeys(new int[]{5},g);
        assertEquals(c,g.getChild(0));
        final Node f = (Node)g.getChild(1);
        assertKeys(new int[]{3},c);
        assertEquals(a,c.getChild(0));
        assertEquals(d,c.getChild(1));
        assertKeys(new int[]{1,2},a);
        assertValues(new Object[]{v1,v2}, a);
        assertKeys(new int[]{3,4},d);
        assertValues(new Object[]{v3,v4}, d);
        assertKeys(new int[]{7},f);
        Leaf e = (Leaf)f.getChild(0);
        assertEquals(b,f.getChild(1));
        assertKeys(new int[]{5,6},e);
        assertValues(new Object[]{v5,v6}, e);
        assertKeys(new int[]{7,9},b);
        assertValues(new Object[]{v7,v9}, b);

        // verify iterator
        assertSameIterator(new IAbstractNode[] { a, d, c, e, b, f, g }, btree.root
                .postOrderIterator());

        /*
         * remove a key (4) from (d) forcing (d,a) to merge into (d) and (a) to
         * be deleted. this causes (c,f) to merge as well, which in turn forces
         * the root to be replaced by (c).
         */
        assertEquals(v4,btree.remove(4));
//        btree.dump(Level.DEBUG,System.err);
        assertKeys(new int[]{5,7},c);
        assertEquals(d,c.getChild(0));
        assertEquals(e,c.getChild(1));
        assertEquals(b,c.getChild(2));
        assertKeys(new int[]{1,2,3},d);
        assertValues(new Object[]{v1,v2,v3}, d);
        assertKeys(new int[]{5,6},e);
        assertValues(new Object[]{v5,v6}, e);
        assertKeys(new int[]{7,9},b);
        assertValues(new Object[]{v7,v9}, b);
        assertTrue(a.isDeleted());

        // verify iterator
        assertSameIterator(new IAbstractNode[] { d, e, b, c }, btree.root
                .postOrderIterator());

        /*
         * remove a key (7) from a leaf (b) forcing two leaves to join and
         * verify the visitation order.
         */
        assertEquals(v7,btree.remove(7));
        btree.dump(Level.DEBUG,System.err);
        assertKeys(new int[]{5},c);
        assertEquals(d,c.getChild(0));
        assertEquals(b,c.getChild(1));
        assertKeys(new int[]{1,2,3},d);
        assertValues(new Object[]{v1,v2,v3}, d);
        assertKeys(new int[]{5,6,9},b);
        assertValues(new Object[]{v5,v6,v9}, b);
        assertTrue(e.isDeleted());

        // verify iterator
        assertSameIterator(new IAbstractNode[] { d, b, c }, btree.root
                .postOrderIterator());

        /*
         * remove keys from a leaf forcing the remaining two leaves to join and
         * verify the visitation order.
         */
        assertEquals(v3,btree.remove(3));
        assertEquals(v5,btree.remove(5));
        assertEquals(v6,btree.remove(6));
        assertKeys(new int[]{1,2,9},b);
        assertValues(new Object[]{v1,v2,v9}, b);
        assertTrue(d.isDeleted());
        assertTrue(c.isDeleted());

        // verify iterator
        assertSameIterator(new IAbstractNode[] { b }, btree.root
                .postOrderIterator());

    }

    /**
     * Test the use of an {@link EntryFilter} to visit only certain values.
     */
    public void test_entryFilter() {
        
        final byte[] k3 = i2k(3);
        final byte[] k5 = i2k(5);
        final byte[] k7 = i2k(7);

        final SimpleEntry v3 = new SimpleEntry(3);
        final SimpleEntry v5 = new SimpleEntry(5);
        final SimpleEntry v7 = new SimpleEntry(7);

        BTree btree = getBTree(3);

        btree.insert(k3,v3);
        btree.insert(k5,v5);
        btree.insert(k7,v7);
        
        final Leaf a = (Leaf) btree.root;
        
        // visit everything in the root leaf.
        assertSameIterator(new Object[]{v3,v5,v7},btree.entryIterator());

        // visit everything in the root leaf.
        assertSameIterator(new Object[]{v3,v5,v7},btree.rangeIterator(null,null));

        // visit everything in the root leaf using an explicit EntryIterator ctor.
        assertSameIterator(new Object[]{v3,v5,v7},new EntryIterator(a,null,null,null,null));
        
        // visit everything exception v3.
        assertSameIterator(new Object[]{v5,v7},new EntryIterator(a,null,null,null,new EntryFilter() {
            private static final long serialVersionUID = 1L;
            public boolean isValid(Object value) {
                        if (value.equals(v3))
                            return false;
                        return true;
                    }
        }));
        
        // visit everything exception v5.
        assertSameIterator(new Object[]{v3,v7},new EntryIterator(a,null,null,null,new EntryFilter() {
            private static final long serialVersionUID = 1L;
            public boolean isValid(Object value) {
                        if (value.equals(v5))
                            return false;
                        return true;
                    }
        }));
        
        // visit everything exception v7.
        assertSameIterator(new Object[]{v3,v5},new EntryIterator(a,null,null,null,new EntryFilter() {
            private static final long serialVersionUID = 1L;
            public boolean isValid(Object value) {
                        if (value.equals(v7))
                            return false;
                        return true;
                    }
        }));

        // visit everything exception v7 using a rangeIterator.
        assertSameIterator(new Object[]{v3,v5},a.rangeIterator(null,null,new EntryFilter() {
            private static final long serialVersionUID = 1L;
            public boolean isValid(Object value) {
                        if (value.equals(v7))
                            return false;
                        return true;
                    }
        }));

    }
    
}
