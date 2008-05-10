/* *********************************************************************** *
 * project: org.matsim.*
 * PersonAlgorithm.java
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

package org.matsim.plans.algorithms;

import org.apache.log4j.Logger;
import org.matsim.plans.Person;
import org.matsim.plans.Plans;
import org.matsim.utils.misc.Counter;

public abstract class PersonAlgorithm implements PersonAlgorithmI {

	private final static Logger log = Logger.getLogger(PersonAlgorithm.class);

	public final void run(final Plans plans) {
		log.info("running " + this.getClass().getName() + " algorithm...");
		Counter counter = new Counter(" person # ");

		for (Person p : plans.getPersons().values()) {
			counter.incCounter();
			this.run(p);
		}
		counter.printCounter();
		log.info("done running algorithm.");
	}

	public abstract void run(Person person);
}
