// Copyright © 2015 HSL <https://www.hsl.fi>
// This program is dual-licensed under the EUPL v1.2 and AGPLv3 licenses.

package fi.hsl.parkandride.back;

import com.google.common.collect.*;
import fi.hsl.parkandride.core.back.ContactRepository;
import fi.hsl.parkandride.core.back.FacilityRepository;
import fi.hsl.parkandride.core.back.HubRepository;
import fi.hsl.parkandride.core.back.OperatorRepository;
import fi.hsl.parkandride.core.domain.*;
import fi.hsl.parkandride.core.service.TransactionalWrite;
import org.geolatte.geom.Point;
import org.geolatte.geom.Polygon;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import static fi.hsl.parkandride.core.domain.CapacityType.CAR;
import static fi.hsl.parkandride.core.domain.CapacityType.ELECTRIC_CAR;
import static fi.hsl.parkandride.core.domain.DayType.SATURDAY;
import static fi.hsl.parkandride.core.domain.DayType.SUNDAY;
import static fi.hsl.parkandride.core.domain.FacilityStatus.EXCEPTIONAL_SITUATION;
import static fi.hsl.parkandride.core.domain.Service.*;
import static fi.hsl.parkandride.core.domain.Spatial.fromWkt;
import static fi.hsl.parkandride.core.domain.Usage.PARK_AND_RIDE;
import static java.util.Arrays.asList;

public class Dummies {

    @Inject ContactRepository contactDao;
    @Inject FacilityRepository facilityDao;
    @Inject OperatorRepository operatorDao;
    @Inject HubRepository hubDao;

    @TransactionalWrite
    public long createHub(Long... facilityIds) {
        final Hub hub = new Hub();
        hub.name = new MultilingualString("Malmi");
        hub.location = (Point) fromWkt("POINT(25.010563 60.251022)");
        hub.facilityIds = ImmutableSet.of(1L, 2L, 3L);
        hub.address = new Address(new MultilingualString("street"), "00100", new MultilingualString("city"));
        hub.facilityIds = Sets.newHashSet(facilityIds);
        return hubDao.insertHub(hub);
    }

    @TransactionalWrite
    public long createFacility() {
        Long operatorId = createDummyOperator();
        FacilityContacts contacts = new FacilityContacts(createDummyContact(), createDummyContact(), createDummyContact());
        Facility facility = createFacility(operatorId, contacts);
        return facilityDao.insertFacility(facility);
    }

    private Long createDummyOperator() {
        Operator operator = new Operator("SMOOTH" + uniqueNumber());
        return operatorDao.insertOperator(operator);
    }

    private Long createDummyContact() {
        Contact contact = new Contact();
        contact.name = new MultilingualString("TEST " + uniqueNumber());
        contact.email = "test@example.com";
        contact.phone = new Phone("09 4766 4000");
        contact.address = new Address(new MultilingualString("street"), "00100", new MultilingualString("city"));
        contact.info = new MultilingualString("info");
        contact.openingHours = new MultilingualString("opening hours");
        return contactDao.insertContact(contact);
    }

    @TransactionalWrite
    public Facility createFacility(Long operatorId, FacilityContacts contacts) {
        Facility facility = new Facility();
        facility.id = 0L;
        facility.name = new MultilingualString("Facility " + uniqueNumber());
        facility.location = (Polygon) Spatial.fromWkt("POLYGON((" +
                "25.010822 60.25054, " +
                "25.010822 60.250023, " +
                "25.012479 60.250337, " +
                "25.011449 60.250885, " +
                "25.010822 60.25054))");
        facility.operatorId = operatorId;
        facility.status = EXCEPTIONAL_SITUATION;
        facility.pricingMethod = PricingMethod.CUSTOM;
        facility.statusDescription = new MultilingualString("Status description");
        facility.aliases = ImmutableSortedSet.of("alias", "blias");
        facility.ports = ImmutableList.of(new Port((Point) Spatial.fromWkt("POINT(25.010822 60.25054)"), true, false, true, false, "street", "00100", "city", "info"));
        facility.services = new NullSafeSortedSet<>(asList(ELEVATOR, TOILETS, ACCESSIBLE_TOILETS));
        facility.contacts = contacts;

        facility.builtCapacity = ImmutableMap.of(
                CAR, 50,
                ELECTRIC_CAR, 2
        );
        // pricing in wrong order should be sorted on load
        facility.pricing.add(new Pricing(CAR, PARK_AND_RIDE, 50, SUNDAY, "8", "18", "1 EUR/H"));
        facility.pricing.add(new Pricing(CAR, PARK_AND_RIDE, 50, SATURDAY, "8", "18", "2 EUR/H"));
        facility.unavailableCapacities = Arrays.asList(
                new UnavailableCapacity(CAR, PARK_AND_RIDE, 1)
        );

        facility.openingHours.info = new MultilingualString("Opening Hours");
        facility.openingHours.url = new MultilingualUrl("http://www.hsl.fi");

        facility.paymentInfo.detail = new MultilingualString("Payment details");
        facility.paymentInfo.url = new MultilingualUrl("http://www.hsl.fi");

        return facility;
    }

    private static final AtomicInteger seq = new AtomicInteger(0);

    private int uniqueNumber() {
        return seq.incrementAndGet();
    }
}
