// Copyright © 2015 HSL <https://www.hsl.fi>
// This program is dual-licensed under the EUPL v1.2 and AGPLv3 licenses.

package fi.hsl.parkandride.front;

import fi.hsl.parkandride.core.domain.*;
import fi.hsl.parkandride.core.domain.prediction.PredictionBatch;
import fi.hsl.parkandride.core.domain.prediction.PredictionRequest;
import fi.hsl.parkandride.core.domain.prediction.PredictionResult;
import fi.hsl.parkandride.core.service.FacilityService;
import fi.hsl.parkandride.core.service.PredictionService;
import fi.hsl.parkandride.front.geojson.Feature;
import fi.hsl.parkandride.front.geojson.FeatureCollection;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import javax.inject.Inject;
import javax.validation.Valid;
import java.util.List;
import java.util.Set;

import static fi.hsl.parkandride.front.UrlSchema.*;
import static fi.hsl.parkandride.front.geojson.FeatureCollection.FACILITY_TO_FEATURE;
import static java.util.stream.Collectors.toList;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.*;

@RestController
public class FacilityController {

    private final Logger log = LoggerFactory.getLogger(FacilityController.class);

    @Inject FacilityService facilityService;
    @Inject PredictionService predictionService;

    @RequestMapping(method = POST, value = FACILITIES, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<Facility> createFacility(@RequestBody Facility facility,
                                                   User currentUser,
                                                   UriComponentsBuilder builder) {
        log.info("createFacility");
        Facility newFacility = facilityService.createFacility(facility, currentUser);
        log.info("createFacility({})", newFacility.id);

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(builder.path(FACILITY).buildAndExpand(newFacility.id).toUri());
        return new ResponseEntity<>(newFacility, headers, CREATED);
    }

    @RequestMapping(method = GET, value = FACILITY, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<Facility> getFacility(@PathVariable(FACILITY_ID) long facilityId) {
        log.info("getFacility({})", facilityId);
        Facility facility = facilityService.getFacility(facilityId);
        return new ResponseEntity<>(facility, OK);
    }

    @RequestMapping(method = GET, value = FACILITY, produces = GEOJSON)
    public ResponseEntity<Feature> getFacilityAsFeature(@PathVariable(FACILITY_ID) long facilityId) {
        log.info("getFacilityAsFeature({})", facilityId);
        Facility facility = facilityService.getFacility(facilityId);
        return new ResponseEntity<>(FACILITY_TO_FEATURE.apply(facility), OK);
    }

    @RequestMapping(method = PUT, value = FACILITY, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<Facility> updateFacility(@PathVariable(FACILITY_ID) long facilityId,
                                                   @RequestBody Facility facility,
                                                   User currentUser) {
        log.info("updateFacility({})", facilityId);
        Facility response = facilityService.updateFacility(facilityId, facility, currentUser);
        return new ResponseEntity<>(response, OK);
    }

    @RequestMapping(method = GET, value = FACILITIES, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<SearchResults<FacilityInfo>> findFacilities(PageableFacilitySearch search) {
        log.info("findFacilities");
        SearchResults<FacilityInfo> results = facilityService.search(search);
        return new ResponseEntity<>(results, OK);
    }

    @RequestMapping(method = GET, value = FACILITIES, params = "summary", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<FacilitySummary> summarizeFacilities(FacilitySearch search) {
        log.info("summarizeFacilities");
        FacilitySummary summary = facilityService.summarize(search);
        return new ResponseEntity<>(summary, OK);
    }

    @RequestMapping(method = GET, value = FACILITIES, produces = GEOJSON)
    public ResponseEntity<FeatureCollection> findFacilitiesAsFeatureCollection(PageableFacilitySearch search) {
        log.info("findFacilitiesAsFeatureCollection");
        SearchResults<FacilityInfo> results = facilityService.search(search);
        return new ResponseEntity<>(FeatureCollection.ofFacilities(results), OK);
    }

    @RequestMapping(method = PUT, value = FACILITY_UTILIZATION, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<Set<Utilization>> registerUtilization(@PathVariable(FACILITY_ID) long facilityId,
                                                                @RequestBody List<Utilization> utilization,
                                                                User currentUser) {
        log.info("registerUtilization({})", facilityId);
        facilityService.registerUtilization(facilityId, utilization, currentUser);
        Set<Utilization> results = facilityService.findLatestUtilization(facilityId);
        return new ResponseEntity<>(results, OK);
    }

    @RequestMapping(method = GET, value = FACILITY_UTILIZATION, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<Set<Utilization>> getUtilization(@PathVariable(FACILITY_ID) long facilityId) {
        log.info("getUtilization({})", facilityId);
        Set<Utilization> results = facilityService.findLatestUtilization(facilityId);
        return new ResponseEntity<>(results, OK);
    }

    @RequestMapping(method = GET, value = FACILITY_PREDICTION, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<List<PredictionResult>> getPrediction(@PathVariable(FACILITY_ID) long facilityId,
                                                                @ModelAttribute @Valid PredictionRequest request) {
        DateTime time = request.requestedTime();
        log.info("getPrediction({}, {})", facilityId, time);
        List<PredictionBatch> predictions = predictionService.getPredictionsByFacility(facilityId, time);
        List<PredictionResult> results = predictions.stream()
                .flatMap(pb -> PredictionResult.from(pb).stream())
                .collect(toList());
        return new ResponseEntity<>(results, OK);
    }
}
