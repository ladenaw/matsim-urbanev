/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.taxi.passenger;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.passenger.PassengerRequest;
import org.matsim.contrib.taxi.schedule.TaxiDropoffTask;
import org.matsim.contrib.taxi.schedule.TaxiPickupTask;

import com.google.common.base.MoreObjects;

/**
 * @author michalm
 */
public class TaxiRequest implements PassengerRequest {
	// request is in one of the following states:
	// UNPLANNED, // submitted by the CUSTOMER and received by the DISPATCHER
	// PLANNED, // planned - included into one of the routes
	// PICKUP, // being picked up
	// RIDE, // on board
	// DROPOFF, // being dropped off
	// PERFORMED, // completed
	// REJECTED // rejected by the DISPATCHER

	private final Id<Request> id;
	private final double submissionTime;
	private final double earliestStartTime;

	private final Id<Person> passengerId;
	private final String mode;

	private final Link fromLink;
	private final Link toLink;

	public TaxiRequest(Id<Request> id, Id<Person> passengerId, String mode, Link fromLink, Link toLink,
			double earliestStartTime, double submissionTime) {
		this.id = id;
		this.submissionTime = submissionTime;
		this.earliestStartTime = earliestStartTime;
		this.passengerId = passengerId;
		this.mode = mode;
		this.fromLink = fromLink;
		this.toLink = toLink;
	}

	@Override
	public Id<Request> getId() {
		return id;
	}

	@Override
	public double getSubmissionTime() {
		return submissionTime;
	}

	@Override
	public double getEarliestStartTime() {
		return earliestStartTime;
	}

	@Override
	public Link getFromLink() {
		return fromLink;
	}

	@Override
	public Link getToLink() {
		return toLink;
	}

	@Override
	public Id<Person> getPassengerId() {
		return passengerId;
	}

	@Override
	public String getMode() {
		return mode;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("id", id)
				.add("submissionTime", submissionTime)
				.add("earliestStartTime", earliestStartTime)
				.add("passengerId", passengerId)
				.add("mode", mode)
				.add("fromLink", fromLink)
				.add("toLink", toLink)
				.toString();
	}
}
