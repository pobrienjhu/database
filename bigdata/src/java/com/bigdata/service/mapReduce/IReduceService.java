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
 * Created on Sep 20, 2007
 */

package com.bigdata.service.mapReduce;

import java.io.IOException;
import java.rmi.Remote;
import java.util.UUID;
import java.util.concurrent.Future;


/**
 * Interface for the map/reduce client service.
 * <p>
 * Note: Methods on this interface are declared to throw {@link IOException}
 * since they may be invoked by RMI.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public interface IReduceService extends Remote {

    /**
     * Declare a job.
     * 
     * @param uuid
     *            The job identifier.
     */
    public void startJob(UUID uuid) throws IOException;
    
    /**
     * Terminate a job.
     * 
     * @param uuid
     *            The job identifier.
     */
    public void endJob(UUID uuid) throws IOException;

    /**
     * Execute an {@link IReduceTask}. The task will be placed into a queue and
     * will begin executing once there is an available thread. Once it begins to
     * execute it will read from the reduce store on which the map clients for
     * the job have written.
     * 
     * @param uuid
     *            The job identifier.
     * @param task
     *            The reduce task.
     */
    public Future submit(UUID uuid, IReduceTask task) throws IOException;
    
}
