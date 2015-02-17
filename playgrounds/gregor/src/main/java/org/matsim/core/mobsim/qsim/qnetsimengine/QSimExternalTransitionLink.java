/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package org.matsim.core.mobsim.qsim.qnetsimengine;

import java.util.Collection;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.vis.snapshotwriters.VisData;

import playground.gregor.external.ExternalEngine;

public class QSimExternalTransitionLink extends AbstractQLink {

	private ExternalEngine e;
	private EventsManager em;

	QSimExternalTransitionLink(Link link, QNetwork network, ExternalEngine e) {
		super(link, network);
		this.e = e;
		this.em = e.getEventsManager();
	}

	@Override
	boolean doSimStep(double now) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	void addFromUpstream(QVehicle veh) {
		this.e.addFromUpstream(link.getFromNode().getId(), link.getToNode()
				.getId(), veh);
		double now = this.e.getMobsim().getSimTimer().getTimeOfDay();
		em.processEvent(new LinkEnterEvent(now, veh.getDriver().getId(), link
				.getId(), veh.getId()));

	}

	@Override
	boolean isAcceptingFromUpstream() {
		return this.e.hasSpace(link.getFromNode().getId());
	}

	@Override
	public Link getLink() {
		return super.link;
	}

	@Override
	public void recalcTimeVariantAttributes(double time) {
		throw new RuntimeException("not yet implemented");
	}

	@Override
	public Collection<MobsimVehicle> getAllNonParkedVehicles() {
		throw new RuntimeException("not yet implemented");
	}

	@Override
	public VisData getVisData() {
		throw new RuntimeException("not yet implemented");
	}

	@Override
	QNode getToNode() {
		throw new RuntimeException("not yet implemented");
	}

	@Override
	boolean isNotOfferingVehicle() {
		return true;
	}

	@Override
	QVehicle popFirstVehicle() {
		throw new RuntimeException("not yet implemented");
	}

	@Override
	QVehicle getFirstVehicle() {
		throw new RuntimeException("not yet implemented");
	}

	@Override
	double getLastMovementTimeOfFirstVehicle() {
		throw new RuntimeException("not yet implemented");
	}

	@Override
	boolean hasGreenForToLink(Id<Link> toLinkId) {
		throw new RuntimeException("not yet implemented");
	}

}
