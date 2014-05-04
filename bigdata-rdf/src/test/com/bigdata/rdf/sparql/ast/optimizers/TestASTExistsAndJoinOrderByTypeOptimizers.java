/**

Copyright (C) SYSTAP, LLC 2006-2014.  All rights reserved.

Contact:
     SYSTAP, LLC
     4501 Tower Road
     Greensboro, NC 27410
     licenses@bigdata.com

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; version 2 of the License.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/
/*
 * Created on May 3, 2014
 */

package com.bigdata.rdf.sparql.ast.optimizers;

import com.bigdata.rdf.sparql.ast.ArbitraryLengthPathNode;

/**
 * Test suite for {@link ASTUnionFiltersOptimizer}.
 * 
 * @author Jeremy Carroll
 */
public class TestASTExistsAndJoinOrderByTypeOptimizers extends AbstractOptimizerTestCase {

    /**
     * 
     */
    public TestASTExistsAndJoinOrderByTypeOptimizers() {
    }

    /**
     * @param name
     */
    public TestASTExistsAndJoinOrderByTypeOptimizers(String name) {
        super(name);
    }
	@Override
	IASTOptimizer newOptimizer() {
		return new ASTOptimizerList(new ASTExistsOptimizer(), 
				new ASTJoinOrderByTypeOptimizer());
	}
	
	public void testSimpleExists() {
		new Helper(){{
			given = select( varNode(w), 
					where ( joinGroupNode(
							    filter(
							    	exists(varNode(y), joinGroupNode(
							    			statementPatternNode(constantNode(a),constantNode(b),varNode(w))))
							    )
							) ) );
			
			expected = select( varNode(w), 
					where (joinGroupNode(
								ask(varNode(y),
										joinGroupNode(
											statementPatternNode(constantNode(a),constantNode(b),varNode(w))
											 ) ),
								filter(exists(varNode(y), joinGroupNode(
						    			statementPatternNode(constantNode(a),constantNode(b),varNode(w))))
						    ) )
							) );
			
		}}.test();
		
	}
	public void testOrExists() {
		new Helper(){{
			given = select( varNode(w), 
					where ( joinGroupNode(
							    filter(
							    	or (
							    	    exists(varNode(y), joinGroupNode(
							    			statementPatternNode(constantNode(a),constantNode(b),varNode(w)))),
								    	exists(varNode(z), joinGroupNode(
											statementPatternNode(constantNode(a),constantNode(c),varNode(w)))))
							    )
							) ) );
			
			expected = select( varNode(w), 
					where (joinGroupNode(
								ask(varNode(y),
										joinGroupNode(
											statementPatternNode(constantNode(a),constantNode(b),varNode(w))
											 ) ),
								ask(varNode(z),
										joinGroupNode(
											statementPatternNode(constantNode(a),constantNode(c),varNode(w))
											) ),
							    filter(
								    	or (
								    	    exists(varNode(y), joinGroupNode(
								    			statementPatternNode(constantNode(a),constantNode(b),varNode(w)))),
									    	exists(varNode(z), joinGroupNode(
												statementPatternNode(constantNode(a),constantNode(c),varNode(w)))))
								    )
						    ) )
							);
			
		}}.test();
		
	}
	public void testOrWithPropertyPath() {
		new Helper(){{
			given = select( varNode(w), 
					where ( joinGroupNode(
							    filter(
							    	or (
							    	    exists(varNode(y), joinGroupNode(
							    			arbitartyLengthPropertyPath(varNode(w), constantNode(b), HelperFlag.ONE_OR_MORE,
													joinGroupNode( statementPatternNode(leftVar(), constantNode(b),  rightVar()) ) )

							    			)),
								    	exists(varNode(z), joinGroupNode(
											statementPatternNode(constantNode(a),constantNode(c),varNode(w)))))
							    )
							) ) );

    		varCount = 0;
			final ArbitraryLengthPathNode alpp1 = arbitartyLengthPropertyPath(varNode(w), constantNode(b), HelperFlag.ONE_OR_MORE,
					joinGroupNode( statementPatternNode(leftVar(), constantNode(b),  rightVar()) ) );
    		varCount = 0;
			final ArbitraryLengthPathNode alpp2 = arbitartyLengthPropertyPath(varNode(w), constantNode(b), HelperFlag.ONE_OR_MORE,
					joinGroupNode( statementPatternNode(leftVar(), constantNode(b),  rightVar()) ) );
			expected = select( varNode(w), 
					where (joinGroupNode(
								ask(varNode(y),
										joinGroupNode(
							    			alpp1
											 ) ),
								ask(varNode(z),
										joinGroupNode(
											statementPatternNode(constantNode(a),constantNode(c),varNode(w))
											) ),
							    filter(
								    	or (
								    	    exists(varNode(y), joinGroupNode(
									    			alpp2

									    			)),
									    	exists(varNode(z), joinGroupNode(
												statementPatternNode(constantNode(a),constantNode(c),varNode(w)))))
								    )
						    ) )
							);
			
		}}.test();
	}

}