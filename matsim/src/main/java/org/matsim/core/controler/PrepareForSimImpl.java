
/* *********************************************************************** *
 * project: org.matsim.*
 * PrepareForSimImpl.java
 *                                                                         *
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
 * *********************************************************************** */

 package org.matsim.core.controler;


import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.groups.FacilitiesConfigGroup;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.config.groups.PlansConfigGroup.HandlingOfPlansWithoutRoutingMode;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.algorithms.ParallelPersonAlgorithmUtils;
import org.matsim.core.population.algorithms.PersonPrepareForSim;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.scenario.Lockable;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.FacilitiesFromPopulation;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.*;
import java.util.stream.Collectors;

import static org.matsim.core.config.groups.QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData;

public final class PrepareForSimImpl implements PrepareForSim, PrepareForMobsim {
	// I think it is ok to have this public final.  Since one may want to use it as a delegate.  kai, may'18
	// but how should that work with a non-public constructor? kai, jun'18
	// Well, I guess it can be injected as well?!
	// bind( PrepareForSimImpl.class ) ;
	// bind( PrepareForSim.class ).to( MyPrepareForSimImpl.class ) ;

	private static Logger log = Logger.getLogger(PrepareForSim.class);

	private final GlobalConfigGroup globalConfigGroup;
	private final Scenario scenario;
	private final Network network;
	private final Population population;
	private final ActivityFacilities activityFacilities;
	private final Provider<TripRouter> tripRouterProvider;
	private final QSimConfigGroup qSimConfigGroup;
	private final FacilitiesConfigGroup facilitiesConfigGroup;
	private final PlansConfigGroup plansConfigGroup;
	private final MainModeIdentifier backwardCompatibilityMainModeIdentifier;
	private final TimeInterpretation timeInterpretation;

	/**
	 * backwardCompatibilityMainModeIdentifier should be a separate MainModeidentifier, neither the routing mode identifier from TripStructureUtils, 
	 * nor the AnalysisMainModeidentifier used for analysis (ModeStats etc.).
	 */
	@Inject
	PrepareForSimImpl(GlobalConfigGroup globalConfigGroup, Scenario scenario, Network network,
				Population population, ActivityFacilities activityFacilities, Provider<TripRouter> tripRouterProvider,
				QSimConfigGroup qSimConfigGroup, FacilitiesConfigGroup facilitiesConfigGroup, 
				PlansConfigGroup plansConfigGroup, 
				MainModeIdentifier backwardCompatibilityMainModeIdentifier,
				TimeInterpretation timeInterpretation) {
		this.globalConfigGroup = globalConfigGroup;
		this.scenario = scenario;
		this.network = network;
		this.population = population;
		this.activityFacilities = activityFacilities;
		this.tripRouterProvider = tripRouterProvider;
		this.qSimConfigGroup = qSimConfigGroup;
		this.facilitiesConfigGroup = facilitiesConfigGroup;
		this.plansConfigGroup = plansConfigGroup;
		this.backwardCompatibilityMainModeIdentifier = backwardCompatibilityMainModeIdentifier;
		this.timeInterpretation = timeInterpretation;
	}


	@Override
	public void run() {
		/*
		 * Create single-mode network here and hand it over to PersonPrepareForSim. Otherwise, each instance would create its
		 * own single-mode network. However, this assumes that the main mode is car - which PersonPrepareForSim also does. Should
		 * be probably adapted in a way that other main modes are possible as well. cdobler, oct'15.
		 */

		//As the routing modules find nearest links anyways, I do not think using a filtered single-mode network (in this case: carOnlyNetwork) is needed anymore.
		//The routing modules even use the mode filtered networks for each mode, which seems to be way more useful e.g. for scenarios
		//with superblocks -sm march22
		final Network net = network;

		//matsim-724
		switch(this.facilitiesConfigGroup.getFacilitiesSource()){
			case none:
//				Gbl.assertIf( this.activityFacilities.getFacilities().isEmpty() );
				// I have at least one use case where people use the facilities as some kind
				// of database for stuff, but don't run the activities off them.  I have thus
				// disabled the above check.  yy We need to think about what we want to
				// do in such cases; might want to auto-generate our facilities as below
				// and _add_ them to the existing facilities.  kai, feb'18
				break;
			case fromFile:
			case setInScenario:
				Gbl.assertIf(! this.activityFacilities.getFacilities().isEmpty() );
				break;
			case onePerActivityLinkInPlansFile:
				/* fall-through */ // switch is inside "FacilitiesFromPopulation" method!
			case onePerActivityLocationInPlansFile:
//				FacilitiesFromPopulation facilitiesFromPopulation = new FacilitiesFromPopulation(activityFacilities, facilitiesConfigGroup);
				FacilitiesFromPopulation facilitiesFromPopulation = new FacilitiesFromPopulation(scenario);

//				facilitiesFromPopulation.setAssignLinksToFacilitiesIfMissing(true, network);
				// (yy not sure if the false setting makes sense at all. kai, jul'18)

//				facilitiesFromPopulation.assignOpeningTimes(facilitiesConfigGroup.isAssigningOpeningTime(), scenario.getConfig().planCalcScore());
				facilitiesFromPopulation.run(population);
				// Note that location choice, when switched on, should now either use the facilities generated here,
				// or come with explicit pre-existing facilities.  kai, jul'18
				break;
			default:
				throw new RuntimeException("Facilities source '"+this.facilitiesConfigGroup.getFacilitiesSource()+"' is not implemented.");
		}

		// get links for facilities
		// using car only network to get the links for facilities. Amit July'18
		XY2LinksForFacilities.run(net, this.activityFacilities);

		// yyyy from a behavioral perspective, the vehicle must be somehow linked to
		// the person (maybe via the household).    kai, feb'18
		// each agent receives a vehicle for each main mode now. janek, aug'19
		createAndAddVehiclesForEveryNetworkMode();
		
		adaptOutdatedPlansForRoutingMode();

		// make sure all routes are calculated.
		// the above creation of vehicles per agent has to be run before executing the initial routing here. janek, aug'19
		// At least xy2links is needed here, i.e. earlier than PrepareForMobsimImpl.  It could, however, presumably be separated out
		// (i.e. we introduce a separate PersonPrepareForMobsim).  kai, jul'18
		ParallelPersonAlgorithmUtils.run(population, globalConfigGroup.getNumberOfThreads(),
				() -> new PersonPrepareForSim(new PlanRouter(tripRouterProvider.get(), activityFacilities, timeInterpretation), scenario,
						net)
		);
		
		if (scenario instanceof Lockable) {
			((Lockable)scenario).setLocked();
			// see comment in ScenarioImpl. kai, sep'14
		}

		if (population instanceof Lockable) {
			((Lockable) population).setLocked();
		}
		
		if ( network instanceof Lockable ) {
			((Lockable) network).setLocked();
		}

		if (activityFacilities instanceof  Lockable) {
			((Lockable) activityFacilities).setLocked();
		}

		// (yyyy means that if someone replaces prepareForSim and does not add the above lines, the containers are not locked.  kai, nov'16)
	}

	// only warn once that legacy vehicle id is used
	private static boolean hasWarned = false;

	private void createAndAddVehiclesForEveryNetworkMode() {

		final Map<String, VehicleType> modeVehicleTypes = getVehicleTypesForAllNetworkAndMainModes();

		for (Person person : scenario.getPopulation().getPersons().values()) {

			var modeToVehicle = modeVehicleTypes.entrySet().stream()
					// map mode type to vehicle id
					.map(modeType -> Tuple.of(modeType, createVehicleId(person, modeType.getKey())))
					// create a corresponding vehicle
					.peek(tuple -> createAndAddVehicleIfNecessary(tuple.getSecond(), tuple.getFirst().getValue()))
					// write mode-string to vehicle-id into a map
					.collect(Collectors.toMap(tuple -> tuple.getFirst().getKey(), Tuple::getSecond));

			VehicleUtils.insertVehicleIdsIntoAttributes(person, modeToVehicle);
		}
	}

	private Id<Vehicle> createVehicleId(Person person, String modeType) {

		if (qSimConfigGroup.getUsePersonIdForMissingVehicleId() && TransportMode.car.equals(modeType)) {
			if (!hasWarned) {
				log.warn("'usePersonIdForMissingVehicleId' is deprecated. It will be removed soon.");
				hasWarned = true;
			}

			return Id.createVehicleId(person.getId());
		}

		return VehicleUtils.createVehicleId(person, modeType);
	}

	private Map<String, VehicleType> getVehicleTypesForAllNetworkAndMainModes() {

		Map<String, VehicleType> modeVehicleTypes = new HashMap<>();

		if (qSimConfigGroup.getVehiclesSource().equals(QSimConfigGroup.VehiclesSource.fromVehiclesData)) {
			// in this case the user has to do everything on their own and we can short circuit here.
			return modeVehicleTypes;
		}

		Set<String> modes = new HashSet<>(qSimConfigGroup.getMainModes());
		modes.addAll(scenario.getConfig().plansCalcRoute().getNetworkModes());

		for (String mode : modes) {
			VehicleType type;
			switch (qSimConfigGroup.getVehiclesSource()) {
				case defaultVehicle:
					type = VehicleUtils.getDefaultVehicleType();
					if (!scenario.getVehicles().getVehicleTypes().containsKey(type.getId())){
						scenario.getVehicles().addVehicleType( type );
					}
					break;
				case modeVehicleTypesFromVehiclesData:
					type = scenario.getVehicles().getVehicleTypes().get(Id.create(mode, VehicleType.class));
					if ( type==null ) {
						log.fatal( "Could not find requested vehicle type =" + mode + ". With config setting " + modeVehicleTypesFromVehiclesData.toString() + ", you need");
						log.fatal( "to add, for each mode that performs network routing and/or is used as network/main mode in the qsim, a vehicle type for that mode." );
						throw new RuntimeException("Could not find requested vehicle type = " + mode + ". See above.");
					}
					break;
				default:
					throw new RuntimeException(qSimConfigGroup.getVehiclesSource().toString() + " is not implemented yet.");
			}
			Gbl.assertNotNull(type);
			modeVehicleTypes.put(mode, type);
		}
		return modeVehicleTypes;
	}

	private void createAndAddVehicleIfNecessary(Id<Vehicle> vehicleId, VehicleType vehicleType) {

		if (!scenario.getVehicles().getVehicles().containsKey(vehicleId)) {

			switch (qSimConfigGroup.getVehiclesSource()) {
				case defaultVehicle:
				case modeVehicleTypesFromVehiclesData:
					Vehicle vehicle = scenario.getVehicles().getFactory().createVehicle(vehicleId, vehicleType);
					scenario.getVehicles().addVehicle(vehicle);
					break;
				default:
					throw new RuntimeException("Expecting a vehicle id which is missing in the vehicles database: " + vehicleId);
			}
		}
	}
	
	private static boolean insistingOnPlansWithoutRoutingModeLogWarnNotShownYet = true;
	
	private void adaptOutdatedPlansForRoutingMode() {
		for (Person person : population.getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				for (Trip trip : TripStructureUtils.getTrips(plan.getPlanElements())) {
					List<Leg> legs = trip.getLegsOnly();
					if (legs.size() >= 1) {
						String routingMode = TripStructureUtils.getRoutingMode(legs.get(0));

						for (Leg leg : legs) {
							// 1. check all legs either have the same routing mode or all have routingMode==null
							if (TripStructureUtils.getRoutingMode(leg) == null) {
								if (routingMode != null) {
									String errorMessage = "Found a mixed trip having some legs with routingMode set and others without. "
											+ "This is inconsistent. Agent id: " + person.getId().toString()
											+ "\nTrip: " + trip.getTripElements().toString();
									log.error(errorMessage);
									throw new RuntimeException(errorMessage);
								}

							} else {
								if (routingMode.equals(TripStructureUtils.getRoutingMode(leg))) {
									TripStructureUtils.setRoutingMode(leg, routingMode);
								} else {
									String errorMessage = "Found a trip whose legs have different routingModes. "
											+ "This is inconsistent. Agent id: " + person.getId().toString()
											+ "\nTrip: " + trip.getTripElements().toString();
									log.error(errorMessage);
									throw new RuntimeException(errorMessage);
								}
							}
						}

						// add routing mode
						if (routingMode == null) {
							if (legs.size() == 1) {
								// there is only a single leg (e.g. after Trips2Legs and a mode choice replanning
								// module)

								String oldMainMode = replaceOutdatedFallbackModesAndReturnOldMainMode(legs.get(0),
										null);
								if (oldMainMode != null) {
									routingMode = oldMainMode;
									TripStructureUtils.setRoutingMode(legs.get(0), routingMode);
								} else {
									// leg has a real mode (not an outdated fallback mode)
									routingMode = legs.get(0).getMode();
									TripStructureUtils.setRoutingMode(legs.get(0), routingMode);
								}
							} else {
								if (plansConfigGroup.getHandlingOfPlansWithoutRoutingMode().equals(HandlingOfPlansWithoutRoutingMode.useMainModeIdentifier)) {
									for (Leg leg : legs) {
										replaceOutdatedAccessEgressWalkModes(leg, routingMode);
									}
									routingMode = getAndAddRoutingModeFromBackwardCompatibilityMainModeIdentifier(
											person, trip);
								} else {
									String errorMessage = "Found a trip with multiple legs and no routingMode. "
											+ "Person id " + person.getId().toString()
											+ "\nTrip: " + trip.getTripElements().toString()
											+ "\nTerminating. Take care to inject an adequate MainModeIdentifier and set config switch "
											+ "plansConfigGroup.setHandlingOfPlansWithoutRoutingMode("
											+ HandlingOfPlansWithoutRoutingMode.useMainModeIdentifier.toString() + ").";
									log.error(errorMessage);
									throw new RuntimeException(errorMessage);
								}
							}
						}

						for (Leg leg : legs) {
							// check before replaceOutdatedAccessEgressHelperModes
							if (leg.getMode().equals(TransportMode.walk) && leg.getRoute() instanceof NetworkRoute) {
								log.error(
										"Found a walk leg with a NetworkRoute. This is the only allowed use case of having "
												+ "non_network_walk as an access/egress mode. PrepareForSimImpl replaces "
												+ "non_network_walk with walk, because access/egress to modes other than walk should "
												+ "use the walk Router. If this causes any problem please report to gleich or kai -nov'19");
							}
						}

						for (Leg leg : legs) {
							replaceOutdatedAccessEgressWalkModes(leg, routingMode);
							replaceOutdatedNonNetworkWalk(leg, routingMode);
							replaceOutdatedFallbackModesAndReturnOldMainMode(leg, routingMode);
						}
					}
				}
			}
		}
	}

	private String getAndAddRoutingModeFromBackwardCompatibilityMainModeIdentifier(Person person, Trip trip) {
		String routingMode;
		if (insistingOnPlansWithoutRoutingModeLogWarnNotShownYet) {
			log.warn(
					"Insisting on using backward compatibility MainModeIdentifier instead of setting routingMode directly.");
			insistingOnPlansWithoutRoutingModeLogWarnNotShownYet = false;
		}
		if (backwardCompatibilityMainModeIdentifier == null) {
			log.error(
					"Found a trip without routingMode, but there is no MainModeIdentifier set up for PrepareForSim, so cannot infer the routing mode from a MainModeIdentifier. Trip: "
							+ trip.getTripElements());
			throw new RuntimeException("no MainModeIdentifier set up for PrepareForSim");
		}
		routingMode = backwardCompatibilityMainModeIdentifier.identifyMainMode(trip.getTripElements());
		if (routingMode != null) {
			for (Leg leg : trip.getLegsOnly()) {
				TripStructureUtils.setRoutingMode(leg, routingMode);
			}
		} else {
			String errorMessage = "Found a trip whose legs had no routingMode. "
					+ "The backwardCompatibilityMainModeIdentifier could not identify the mode. " + "Agent id: "
					+ person.getId().toString() + "\nTrip: " + trip.getTripElements().toString();
			log.error(errorMessage);
			throw new RuntimeException(errorMessage);
		}
		return routingMode;
	}

	private void replaceOutdatedAccessEgressWalkModes(Leg leg, String routingMode) {
		// access_walk and egress_walk were replaced by non_network_walk
		if (leg.getMode().equals("access_walk") || leg.getMode().equals("egress_walk")) {
			leg.setMode(TransportMode.non_network_walk);
			TripStructureUtils.setRoutingMode(leg, routingMode);
		}
	}

	// non_network_walk as access/egress to modes other than walk on the network was replaced by walk. -
	// kn/gl-nov'19
	private void replaceOutdatedNonNetworkWalk(Leg leg, String routingMode) {
		if (leg.getMode().equals(TransportMode.non_network_walk)) {
			leg.setMode(TransportMode.walk);
			TripStructureUtils.setRoutingMode(leg, routingMode);
		}
	}

	/**
	 * Method to replace outdated TransportModes such as drt_fallback or transit_walk.
	 * Some of those were also used as access/egress to pt/drt helper modes.
	 *
	 * @param routingMode new routingMode which will be set at the leg
	 * @return null if no fallback mode was found or the main mode of the fallback mode found
	 */
	private String replaceOutdatedFallbackModesAndReturnOldMainMode(Leg leg, String routingMode) {
		// transit_walk was replaced by walk (formerly fallback and access/egress/transfer to pt mode)
		if (leg.getMode().equals(TransportMode.transit_walk)) {
			leg.setMode(TransportMode.walk);
			TripStructureUtils.setRoutingMode(leg, routingMode);
			return TransportMode.pt;
		}

		// replace drt_walk etc. (formerly fallback and access/egress to drt modes)
		if (leg.getMode().endsWith("_walk") && !leg.getMode().equals(TransportMode.non_network_walk)) {
			String oldMainMode = leg.getMode().substring(0, leg.getMode().length() - 5);
			leg.setMode(TransportMode.walk);
			TripStructureUtils.setRoutingMode(leg, routingMode);
			return oldMainMode;
		}

		// replace drt_fallback etc. (formerly fallback for drt modes)
		if (leg.getMode().endsWith("_fallback")) {
			String oldMainMode = leg.getMode().substring(0, leg.getMode().length() - 9);
			leg.setMode(TransportMode.walk);
			TripStructureUtils.setRoutingMode(leg, routingMode);
			return oldMainMode;
		}

		return null;
	}
}
