/* *********************************************************************** *
 * project: org.matsim.*
 * KmlNetworkWriter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package playground.dgrether.events.filters;

import org.matsim.events.BasicEvent;


/**
 * @author dgrether
 *
 */
public interface EventFilter {
	/**
	 * judges whether the BasicEvent
	 * (org.matsim.demandmodeling.events.BasicEvent) will be processed or not
	 *
	 * @param event -
	 *            which is being judged
	 * @return true if the event meets the criterion of the implementation
	 */
	boolean judge(BasicEvent event);


}
